#include "local-include/verilating.h"
#include <cpu/cpu.h>
#include <cpu/decode.h>

int isa_exec_once(Decode *s) {
  rvcpu_single_exec();
  s->isa.inst.val = rvcpu_ifetch(&s->snpc, 4);
  s->dnpc = cpu.pc;
  return 0;
}
