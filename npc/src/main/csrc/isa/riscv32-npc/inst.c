#include "local-include/reg.h"
#include "local-include/verilating.h"
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/ifetch.h>


int isa_exec_once(Decode *s) {
  s->isa.inst.val = vaddr_ifetch(*(cpu.pc), 4);
  s->snpc = *(cpu.pc) + 4;
  rvcpu_single_cycle();
  s->dnpc = *(cpu.pc);
  return 0;
}