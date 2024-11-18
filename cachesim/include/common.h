#ifndef __COMMON_H__
#define __COMMON_H__

#include <debug.h>
#include <math.h>
#include <stdint.h>
#include <utils.h>

#define macro_exp2(x) (1 << (x))
#define mask_with_len(x) (macro_exp2(x) - 1)

#define SRAM_BASE 0x0f000000
#define SRAM_SIZE 0x01000000

#define MROM_BASE 0x20000000
#define MROM_SIZE 0x1000

#define PSRAM_BASE 0x80000000
#define PSRAM_SIZE 0x20000000

#define SDRAM_BASE 0xa0000000
#define SDRAM_SIZE 0x20000000

#define MEM_SIZE macro_exp2(24)

#define ADDR_WIDTH 32
#define XLEN 32
#define WORD_WIDTH ((uint32_t)log2(XLEN / 8))

#define ACCESS_TIME 2.275815
#define MISS_TIME 33.495026
#endif
