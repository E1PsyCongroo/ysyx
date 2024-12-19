#include <am.h>
#include <npc.h>
#include <klib.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)

void __am_gpu_init() {
  // int i;
  // int w = inl(VGACTL_ADDR) >> 16;
  // int h = inl(VGACTL_ADDR) & 0xffff;
  // uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  // for (i = 0; i < w * h; i ++) fb[i] = i;
  // outl(SYNC_ADDR, 1);
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = inl(VGACTL_ADDR) >> 16, .height = inl(VGACTL_ADDR) & 0xffff,
    .vmemsz = (inl(VGACTL_ADDR) >> 16) * (inl(VGACTL_ADDR) & 0xffff) * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t width = io_read(AM_GPU_CONFIG).width;
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  uint32_t *pixels = ctl->pixels;
  int base_fb = ctl->y * width + ctl->x;
  int base_pixels = 0;
  for (int y = 0; y < ctl->h; y++) {
    memcpy(fb + base_fb, pixels + base_pixels, ctl->w * sizeof(uint32_t));
    base_fb += width;
    base_pixels += ctl->w;
  }
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}