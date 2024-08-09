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
#include <memory/paddr.h>

word_t vaddr_ifetch(vaddr_t addr, int len) {
  word_t inst = paddr_read(addr, len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, inst = " FMT_WORD "\n", ANSI_FG_CYAN), "Read Instruction", addr, len, inst); }
#endif
  return inst;
}

word_t vaddr_read(vaddr_t addr, int len) {
  word_t data = paddr_read(addr, len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "Read Memory", addr, len, data); }
#endif
  return data;
}

void vaddr_write(vaddr_t addr, int len, word_t data) {
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "Write Memory", addr, len, data); }
#endif
  paddr_write(addr, len, data);
}


