#ifndef __RISCV_REG_H__
#define __RISCV_REG_H__

#include <common.h>

static inline int check_reg_idx(int idx) {
  IFDEF(CONFIG_RT_CHECK, assert(idx >= 0 && idx < MUXDEF(CONFIG_RVE, 16, 32)));
  return idx;
}

#define gpr(idx) (cpu.gpr[check_reg_idx(idx)])

enum riscv32_CSR {
  MSTATUS = 0x300, MTVEC = 0x305,
  MEPC = 0x341, MCAUSE = 0x342,
};

static inline word_t* get_csr(int csr_num) {
  Assert(csr_num < 4096, "csr number should less than 4096");
  switch (csr_num) {
    case MSTATUS: return &cpu.mstatus;
    case MTVEC: return &cpu.mtvec;
    case MEPC: return &cpu.mepc;
    case MCAUSE: return &cpu.mcause;
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
