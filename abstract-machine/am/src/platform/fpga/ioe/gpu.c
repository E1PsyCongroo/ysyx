#include <am.h>
#include <fpga.h>
#include <klib.h>

#define WIDTH 640
#define HEIGHT 480

void __am_gpu_init() {
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = false, .has_accel = false,
    .width = WIDTH, .height = HEIGHT,
    .vmemsz = WIDTH * HEIGHT * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
