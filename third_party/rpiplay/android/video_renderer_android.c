#include "../renderers/video_renderer.h"

#include <stdlib.h>

extern int mirroraire_android_video_start_renderer(void);
extern int mirroraire_android_video_render_buffer(const unsigned char *data, int data_len, unsigned long long pts, int type);
extern void mirroraire_android_video_flush_renderer(void);
extern void mirroraire_android_video_destroy_renderer(void);

typedef struct video_renderer_android_s {
    video_renderer_t base;
} video_renderer_android_t;

static const video_renderer_funcs_t video_renderer_android_funcs;

video_renderer_t *video_renderer_android_init(logger_t *logger, video_renderer_config_t const *config) {
    video_renderer_android_t *renderer = calloc(1, sizeof(video_renderer_android_t));
    if (!renderer) {
        return NULL;
    }
    renderer->base.logger = logger;
    renderer->base.funcs = &video_renderer_android_funcs;
    renderer->base.type = VIDEO_RENDERER_ANDROID;
    return &renderer->base;
}

static void video_renderer_android_start(video_renderer_t *renderer) {
    (void) renderer;
    mirroraire_android_video_start_renderer();
}

static void video_renderer_android_render_buffer(
    video_renderer_t *renderer,
    raop_ntp_t *ntp,
    unsigned char *data,
    int data_len,
    uint64_t pts,
    int type) {
    (void) renderer;
    (void) ntp;
    if (!data || data_len <= 0) {
        return;
    }
    mirroraire_android_video_render_buffer(data, data_len, (unsigned long long) pts, type);
}

static void video_renderer_android_flush(video_renderer_t *renderer) {
    (void) renderer;
    mirroraire_android_video_flush_renderer();
}

static void video_renderer_android_destroy(video_renderer_t *renderer) {
    mirroraire_android_video_destroy_renderer();
    free(renderer);
}

static void video_renderer_android_update_background(video_renderer_t *renderer, int type) {
    (void) renderer;
    (void) type;
}

static const video_renderer_funcs_t video_renderer_android_funcs = {
    .start = video_renderer_android_start,
    .render_buffer = video_renderer_android_render_buffer,
    .flush = video_renderer_android_flush,
    .destroy = video_renderer_android_destroy,
    .update_background = video_renderer_android_update_background,
};
