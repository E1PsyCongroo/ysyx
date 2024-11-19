#include <isa.h>
#include "local-include/reg.h"

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void isa_reg_display() {
  printf("%-8s  0x%-16x  %d\n", "pc", cpu.pc, cpu.pc);
  for (int i = 0; i < MUXDEF(CONFIG_RVE, 16, 32); i++) {
    printf("%-8s  0x%-16x  %d\n", reg_name(i), gpr(i), gpr(i));
  }
  printf("%-8s  0x%-16x  %d\n", "mtvec", cpu.mtvec, cpu.mtvec);
  printf("%-8s  0x%-16x  %d\n", "mstatus", cpu.mstatus, cpu.mstatus);
  printf("%-8s  0x%-16x  %d\n", "mcause", cpu.mcause, cpu.mcause);
  printf("%-8s  0x%-16x  %d\n", "mepc", cpu.mepc, cpu.mepc);
}

word_t isa_reg_str2val(const char *s, bool *success) {
  *success = false;
  for (int i = 0; i < MUXDEF(CONFIG_RVE, 16, 32); i++) {
    if (!strcmp(s, reg_name(i))) {
      *success = true;
      return gpr(i);
    }
  }
  if (!strcmp(s, "pc")) {
    *success = true;
    return cpu.pc;
  }
  else if (!strcmp(s, "mepc")) {
    *success = true;
    return cpu.mepc;
  }
  else if (!strcmp(s, "mstatus")) {
    *success = true;
    return cpu.mstatus;
  }
  else if (!strcmp(s, "mcause")) {
    *success = true;
    return cpu.mcause;
  }
  else if (!strcmp(s, "mtvec")) {
    *success = true;
    return cpu.mtvec;
  }

  return 0;
}