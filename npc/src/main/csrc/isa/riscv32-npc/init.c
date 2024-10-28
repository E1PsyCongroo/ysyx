#include "common.h"
#include "local-include/dev.h"
#include "local-include/verilating.h"
#include <isa.h>
#include <memory/paddr.h>
#include <memory/vaddr.h>
// this is not consistent with uint8_t
// but it is ok since we do not access the array directly
static const uint32_t img[] = {
    // 0x00000297,  // auipc t0,0
    // 0x00028823,  // sb  zero,16(t0)
    // 0x0102c503,  // lbu a0,16(t0)
    // 0x00100073,  // ebreak (used as nemu_trap)
    // 0xdeadbeef,  // some data

    0x100007b7, // lui	a5,0x10000
    0x04100713, // li	a4,65
    0x00e78023, // sb	a4,0(a5) # 10000000
    0x00a00713, // li	a4,10
    0x00e78023, // sb	a4,0(a5)
    0x00000513, // li	a0,0
    0x00100073, // ebreak (used as npc_trap)
};

void init_isa(const char *wave_file, int argc, char **argv) {
  /* Load built-in image. */
  init_isa_dev();
  memcpy(dev_flash_to_host(FLASH_LEFT), img,
         sizeof(img));
  memcpy(dev_mrom_to_host(MROM_LEFT), img,
         sizeof(img));
  rvcpu_init(wave_file, argc, argv);
}
