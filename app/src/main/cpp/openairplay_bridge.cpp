#include <android/log.h>
#include <android/native_window_jni.h>
#include <atomic>
#include <chrono>
#include <jni.h>
#include <mutex>
#include <string>
#include <thread>

namespace {
constexpr const char* kTag = "MirrorNodeNative";

std::atomic<bool> g_running(false);
std::thread g_receiverThread;
std::mutex g_mutex;
ANativeWindow* g_window = nullptr;

void receiverLoop(std::string receiver_name, std::string config_path, int airplay_port, int raop_port) {
  __android_log_print(
      ANDROID_LOG_INFO,
      kTag,
      "MirrorNode native receiver placeholder running. name=%s config=%s airplay=%d raop=%d",
      receiver_name.c_str(),
      config_path.c_str(),
      airplay_port,
      raop_port);

  while (g_running.load()) {
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
    ANativeWindow_release(g_window);
    g_window = nullptr;
  }
}
