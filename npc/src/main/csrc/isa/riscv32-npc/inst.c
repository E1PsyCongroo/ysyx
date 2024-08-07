#include "local-include/reg.h"
#include "local-include/verilating.h"
#include <cpu/cpu.h>
#include <cpu/decode.h>


int isa_exec_once(Decode *s) {
  rvcpu_single_cycle();
  s->dnpc = *(cpu.pc);
  return 0;
}