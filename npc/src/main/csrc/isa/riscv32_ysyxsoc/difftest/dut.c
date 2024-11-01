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

#include <isa.h>
#include <cpu/difftest.h>
#include "../local-include/reg.h"

bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) {
  bool result = difftest_check_reg("pc", pc, ref_r->pc, cpu.pc);
  for (int i = 0; i < RISCV_GPR_NUM && result; i++) {
    result = difftest_check_reg(reg_name(i), pc, ref_r->gpr[i], gpr(i));
  }
  result &= difftest_check_reg("mepc", pc, ref_r->mepc, cpu.mepc);
  result &= difftest_check_reg("mstatus", pc, ref_r->mstatus, cpu.mstatus);
  result &= difftest_check_reg("mcause", pc, ref_r->mcause, cpu.mcause);
  result &= difftest_check_reg("mtvec", pc, ref_r->mtvec, cpu.mtvec);
  return result;
}

void isa_difftest_attach() {
}
