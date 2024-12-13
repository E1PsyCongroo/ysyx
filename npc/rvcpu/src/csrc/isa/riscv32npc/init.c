#include <isa.h>
#include <memory/paddr.h>
#include <memory/vaddr.h>
#include "local-include/verilating.h"
// this is not consistent with uint8_t
// but it is ok since we do not access the array directly
static const uint32_t img [] = {
  0x00000297,  // auipc t0,0
  0x00028823,  // sb  zero,16(t0)
  0x0102c503,  // lbu a0,16(t0)
  0x00100073,  // ebreak (used as nemu_trap)
  0xdeadbeef,  // some data
};

void init_isa(const char *wave_file, int argc, char **argv) {
  /* Load built-in image. */
  memcpy(guest_to_host(RESET_VECTOR), img, sizeof(img));
  rvcpu_init(wave_file, argc, argv);
}