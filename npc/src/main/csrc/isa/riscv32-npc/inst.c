#include "local-include/verilating.h"
#include <cpu/cpu.h>
#include <cpu/decode.h>

int isa_exec_once(Decode *s) {
  s->isa.inst.val = rvcpu_ifetch(&s->snpc, 4);
  rvcpu_single_cycle();
  s->dnpc = cpu.pc;
  return 0;
}
