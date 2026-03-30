#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <atomic>
#include <chrono>
#include <array>
#include <jni.h>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "renderers/audio_renderer.h"
#include "renderers/video_renderer.h"

int start_server(
    std::vector<char> hw_addr,
    std::string name,
    bool debug_log,
    video_renderer_config_t const* video_config,
    audio_renderer_config_t const* audio_config,
    unsigned short raop_port);

int stop_server();

namespace {
constexpr const char* kTag = "MirrorNodeNative";

JavaVM* g_vm = nullptr;
jclass g_decoderClass = nullptr;
jmethodID g_setSurfaceMethod = nullptr;
jmethodID g_startMethod = nullptr;
jmethodID g_stopMethod = nullptr;
jmethodID g_flushMethod = nullptr;
jmethodID g_queueSampleMethod = nullptr;

std::atomic<bool> g_running(false);
std::mutex g_mutex;
ANativeWindow* g_window = nullptr;

JNIEnv* getEnv(bool* did_attach) {
  *did_attach = false;
  if (g_vm == nullptr) {
    return nullptr;
  }

  JNIEnv* env = nullptr;
  const jint status = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
  if (status == JNI_OK) {
    return env;
  }
  if (status != JNI_EDETACHED) {
    return nullptr;
  }
  if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
    return nullptr;
  }
  *did_attach = true;
  return env;
}

void releaseEnv(bool did_attach) {
  if (did_attach && g_vm != nullptr) {
    g_vm->DetachCurrentThread();
  }
}

void callDecoderSetSurface(JNIEnv* env, jobject surface) {
  if (g_decoderClass == nullptr || g_setSurfaceMethod == nullptr) {
    return;
  }
  env->CallStaticVoidMethod(g_decoderClass, g_setSurfaceMethod, surface);
}

bool callDecoderStart() {
  bool did_attach = false;
  JNIEnv* env = getEnv(&did_attach);
  if (env == nullptr || g_decoderClass == nullptr || g_startMethod == nullptr) {
    releaseEnv(did_attach);
    return false;
  }
  env->CallStaticVoidMethod(g_decoderClass, g_startMethod);
  const bool ok = !env->ExceptionCheck();
  if (env->ExceptionCheck()) {
    env->ExceptionClear();
  }
  releaseEnv(did_attach);
  return ok;
}

void callDecoderStop() {
  bool did_attach = false;
  JNIEnv* env = getEnv(&did_attach);
  if (env != nullptr && g_decoderClass != nullptr && g_stopMethod != nullptr) {
    env->CallStaticVoidMethod(g_decoderClass, g_stopMethod);
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }
  releaseEnv(did_attach);
}

void callDecoderFlush() {
  bool did_attach = false;
  JNIEnv* env = getEnv(&did_attach);
  if (env != nullptr && g_decoderClass != nullptr && g_flushMethod != nullptr) {
    env->CallStaticVoidMethod(g_decoderClass, g_flushMethod);
    if (env->ExceptionCheck()) {
      env->ExceptionClear();
    }
  }
  releaseEnv(did_attach);
}

bool callDecoderQueueSample(const unsigned char* data, int data_len, uint64_t pts, int type) {
  bool did_attach = false;
  JNIEnv* env = getEnv(&did_attach);
  if (env == nullptr || g_decoderClass == nullptr || g_queueSampleMethod == nullptr) {
    releaseEnv(did_attach);
    return false;
  }

  jbyteArray sample = env->NewByteArray(data_len);
  if (sample == nullptr) {
    releaseEnv(did_attach);
    return false;
  }
  env->SetByteArrayRegion(sample, 0, data_len, reinterpret_cast<const jbyte*>(data));
  const jboolean result = env->CallStaticBooleanMethod(
      g_decoderClass,
      g_queueSampleMethod,
      sample,
      static_cast<jlong>(pts),
      static_cast<jint>(type));
  env->DeleteLocalRef(sample);
  const bool ok = !env->ExceptionCheck() && result == JNI_TRUE;
  if (env->ExceptionCheck()) {
    env->ExceptionClear();
  }
  releaseEnv(did_attach);
  return ok;
}

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

}  // namespace

extern "C" jint JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
  g_vm = vm;
  JNIEnv* env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK || env == nullptr) {
    return JNI_ERR;
  }

  jclass localClass = env->FindClass("com/mirrornode/app/AndroidVideoDecoder");
  if (localClass == nullptr) {
    return JNI_ERR;
  }

  g_decoderClass = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
  env->DeleteLocalRef(localClass);
  if (g_decoderClass == nullptr) {
    return JNI_ERR;
  }

  g_setSurfaceMethod =
      env->GetStaticMethodID(g_decoderClass, "setSurface", "(Landroid/view/Surface;)V");
  g_startMethod = env->GetStaticMethodID(g_decoderClass, "start", "()V");
  g_stopMethod = env->GetStaticMethodID(g_decoderClass, "stop", "()V");
  g_flushMethod = env->GetStaticMethodID(g_decoderClass, "flush", "()V");
  g_queueSampleMethod =
      env->GetStaticMethodID(g_decoderClass, "queueSample", "([BJI)Z");

  if (g_setSurfaceMethod == nullptr || g_startMethod == nullptr || g_stopMethod == nullptr ||
      g_flushMethod == nullptr || g_queueSampleMethod == nullptr) {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}

extern "C" int mirroraire_android_video_start_renderer(void) {
  return callDecoderStart() ? 0 : -1;
}

extern "C" int mirroraire_android_video_render_buffer(
    const unsigned char* data,
    int data_len,
    unsigned long long pts,
    int type) {
  return callDecoderQueueSample(data, data_len, static_cast<uint64_t>(pts), type) ? 0 : -1;
}

extern "C" void mirroraire_android_video_flush_renderer(void) {
  callDecoderFlush();
}

extern "C" void mirroraire_android_video_destroy_renderer(void) {
  callDecoderStop();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_mirrornode_app_ReceiverNativeBridge_nativeStartReceiver(
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

  __android_log_print(
      ANDROID_LOG_INFO,
      kTag,
      "MirrorAir native receiver starting. name=%s config=%s airplay=%d raop=%d",
      receiver_name_value.c_str(),
      config_path_value.c_str(),
      static_cast<int>(airplay_port),
      static_cast<int>(raop_port));

  video_renderer_config_t video_config{};
  video_config.background_mode = BACKGROUND_MODE_OFF;
  video_config.low_latency = true;
  video_config.rotation = 0;
  video_config.flip = FLIP_NONE;

  audio_renderer_config_t audio_config{};
  audio_config.device = AUDIO_DEVICE_NONE;
  audio_config.low_latency = true;

  std::vector<char> hw_addr = {
      static_cast<char>(0x02),
      static_cast<char>(0x11),
      static_cast<char>(0x22),
      static_cast<char>(0x33),
      static_cast<char>(0x44),
      static_cast<char>(0x55),
  };

  if (raop_port <= 0) {
    __android_log_print(
        ANDROID_LOG_ERROR, kTag, "Invalid RAOP port: %d", static_cast<int>(raop_port));
    return JNI_FALSE;
  }

  const int started = start_server(
      hw_addr,
      receiver_name_value,
      true,
      &video_config,
      &audio_config,
      static_cast<unsigned short>(raop_port));
  if (started != 0) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "RPiPlay start_server failed: %d", started);
    return JNI_FALSE;
  }

  g_running.store(true);

  return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_mirrornode_app_ReceiverNativeBridge_nativeSetVideoSurface(
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
  callDecoderSetSurface(env, surface);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mirrornode_app_ReceiverNativeBridge_nativeStopReceiver(
    JNIEnv* /* env */,
    jobject /* this */) {
  std::scoped_lock lock(g_mutex);
  if (!g_running.load()) {
    return;
  }

  g_running.store(false);
  stop_server();
  if (g_window != nullptr) {
    ANativeWindow_release(g_window);
    g_window = nullptr;
  }
}
