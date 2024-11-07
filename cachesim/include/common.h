#ifndef __COMMON_H__
#define __COMMON_H__

#include <debug.h>
#include <math.h>
#include <utils.h>

#define macro_exp2(x) (1 << (x))
#define mask_with_len(x) (macro_exp2(x) - 1)

#define MEM_SIZE (1 << 25) // 1MB
#define BLOCK_WIDTH 6      // 64B
#define BLOCK_SIZE macro_exp2(BLOCK_WIDTH)

#define ADDRWIDTH 32
#define XLEN 32
#define byte_offset_width ((int)log2(XLEN / 8))
#define block_offset_with (BLOCK_WIDTH - byte_offset_width)

void cycle_increase(int n);

#endif
