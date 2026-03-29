#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <atomic>
#include <chrono>
#include <jni.h>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>

namespace {
constexpr const char* kTag = "MirrorNodeNative";

std::atomic<bool> g_running(false);
std::thread g_receiverThread;
std::mutex g_mutex;
ANativeWindow* g_window = nullptr;

void drawReceiverFrameLocked() {
  if (g_window == nullptr) {
    return;
  }

  ANativeWindow_setBuffersGeometry(g_window, 0, 0, WINDOW_FORMAT_RGBA_8888);

  ANativeWindow_Buffer buffer;
  if (ANativeWindow_lock(g_window, &buffer, nullptr) != 0) {
    __android_log_print(ANDROID_LOG_WARN, kTag, "Could not lock native window");
    return;
  }

  auto* pixels = static_cast<std::uint32_t*>(buffer.bits);
  if (pixels == nullptr) {
    ANativeWindow_unlockAndPost(g_window);
    return;
  }

  const int width = buffer.width;
  const int height = buffer.height;
  const int stride = buffer.stride;
  const std::uint32_t background = 0xFF000000u;
  const std::uint32_t foreground = 0xFFFFFFFFu;

  for (int y = 0; y < height; ++y) {
    std::uint32_t* row = pixels + (y * stride);
    for (int x = 0; x < width; ++x) {
      row[x] = background;
    }
  }

  const int border = width > 600 ? 8 : 4;
  const int panelWidth = width * 3 / 5;
  const int panelHeight = height / 4;
  const int left = (width - panelWidth) / 2;
  const int top = (height - panelHeight) / 2;
  const int right = left + panelWidth;
  const int bottom = top + panelHeight;

  for (int y = top; y < bottom; ++y) {
    std::uint32_t* row = pixels + (y * stride);
    for (int x = left; x < right; ++x) {
      const bool onBorder =
          x < left + border || x >= right - border || y < top + border || y >= bottom - border;
      row[x] = onBorder ? foreground : background;
    }
  }

  ANativeWindow_unlockAndPost(g_window);
}

void receiverLoop(std::string receiver_name, std::string config_path, int airplay_port, int raop_port) {
  __android_log_print(
      ANDROID_LOG_INFO,
      kTag,
      "MirrorAir native receiver placeholder running. name=%s config=%s airplay=%d raop=%d",
      receiver_name.c_str(),
      config_path.c_str(),
      airplay_port,
      raop_port);

  while (g_running.load()) {
    {
      std::scoped_lock lock(g_mutex);
      drawReceiverFrameLocked();
    }
    std::this_thread::sleep_for(std::chrono::seconds(1));
  }
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_mirrornode_app_ReceiverNativeBridge_startReceiver(
    JNIEnv* env,
    jobject /* this */,
    jstring receiver_name,
    jstring config_path,
    jint airplay_port,
    jint raop_port) {
  std::scoped_lock lock(g_mutex);
  if (g_running.load()) {
    return JNI_TRUE;
  }

  const char* receiver_name_chars = env->GetStringUTFChars(receiver_name, nullptr);
  const char* config_path_chars = env->GetStringUTFChars(config_path, nullptr);

  std::string receiver_name_value(receiver_name_chars == nullptr ? "" : receiver_name_chars);
  std::string config_path_value(config_path_chars == nullptr ? "" : config_path_chars);

  if (receiver_name_chars != nullptr) {
    env->ReleaseStringUTFChars(receiver_name, receiver_name_chars);
  }
  if (config_path_chars != nullptr) {
    env->ReleaseStringUTFChars(config_path, config_path_chars);
  }

  g_running.store(true);
  g_receiverThread = std::thread(
      receiverLoop,
      std::move(receiver_name_value),
      std::move(config_path_value),
      static_cast<int>(airplay_port),
      static_cast<int>(raop_port));

  return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_mirrornode_app_ReceiverNativeBridge_setVideoSurface(
    JNIEnv* env,
    jobject /* this */,
    jobject surface) {
  std::scoped_lock lock(g_mutex);
  if (g_window != nullptr) {
    ANativeWindow_release(g_window);
    g_window = nullptr;
  }

  if (surface != nullptr) {
    g_window = ANativeWindow_fromSurface(env, surface);
    drawReceiverFrameLocked();
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_mirrornode_app_ReceiverNativeBridge_stopReceiver(
    JNIEnv* /* env */,
    jobject /* this */) {
  std::scoped_lock lock(g_mutex);
  if (!g_running.load()) {
    return;
  }

  g_running.store(false);
  if (g_receiverThread.joinable()) {
    g_receiverThread.join();
  }
  if (g_window != nullptr) {
    drawReceiverFrameLocked();
    ANativeWindow_release(g_window);
    g_window = nullptr;
  }
}
