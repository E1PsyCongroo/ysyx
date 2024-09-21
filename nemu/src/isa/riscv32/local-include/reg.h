/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#ifndef __RISCV_REG_H__
#define __RISCV_REG_H__

#include "debug.h"
#include <common.h>
#include <isa.h>
#include <stdint.h>

static inline int check_reg_idx(int idx) {
  IFDEF(CONFIG_RT_CHECK, assert(idx >= 0 && idx < MUXDEF(CONFIG_RVE, 16, 32)));
  return idx;
}

#define gpr(idx) (cpu.gpr[check_reg_idx(idx)])

typedef struct
{
  word_t      : 1, sie  : 1,      : 1, mie  : 1,      : 1,
         spie : 1, ube  : 1, mpie : 1, spp  : 1, vs   : 2,
         mpp  : 2, fs   : 2, xs   : 2, mprv : 1, sum  : 1,
         mxr  : 1, tvm  : 1, tw   : 1, tsr  : 1,      : 8, sd   : 1;
} mstatus_t;

enum riscv32_CSR {
  MSTATUS = 0x300, MTVEC = 0x305,
  MEPC = 0x341, MCAUSE = 0x342,
  MVENDORID = 0xF11, MARCHID = 0xF12,
};

static inline word_t* get_csr(uint32_t csr_num) {
  Assert(csr_num < 4096, "csr number should less than 4096");
  static word_t mvendorid = 0x79737978;
  static word_t marchid = 0;
  Assert(mvendorid == 0x79737978, "mvendorid changed!");
  Assert(marchid == 0, "marchid changed!");
  switch (csr_num) {
    case MSTATUS: return &cpu.mstatus;
    case MTVEC: return &cpu.mtvec;
    case MEPC: return &cpu.mepc;
    case MCAUSE: return &cpu.mcause;
    case MVENDORID: return &mvendorid;
    case MARCHID: return &marchid;
    default: panic("csr[0x%03x] is not implemented", csr_num);
  }
  return NULL;
}

#define csr(csr_num) (*get_csr(csr_num))

static inline const char* reg_name(int idx) {
  extern const char* regs[];
  return regs[check_reg_idx(idx)];
}

#endif
