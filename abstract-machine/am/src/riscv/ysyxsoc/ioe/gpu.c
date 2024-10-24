#include <am.h>
#include <ysyxsoc.h>
#include <klib.h>

void __am_gpu_init() {
  // int w = 640;
  // int h = 480;
  // uint32_t *fb = (uint32_t *)VGAFB_ADDR;
  // for (int i = 0; i < w * h; i ++) fb[i] = i;
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = 640, .height = 480,
    .vmemsz = 640 * 480 * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t width = io_read(AM_GPU_CONFIG).width;
  uint32_t *fb = (uint32_t *)VGAFB_ADDR;
  uint32_t *pixels = ctl->pixels;
  int base_fb = ctl->y * width + ctl->x;
  int base_pixels = 0;
  for (int y = 0; y < ctl->h; y++) {
    memcpy(fb + base_fb, pixels + base_pixels, ctl->w * sizeof(uint32_t));
    base_fb += width;
    base_pixels += ctl->w;
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
