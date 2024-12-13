#include "debug.h"
#include "local-include/verilating.h"
#include <common.h>
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <isa.h>
#include <memory/paddr.h>
#include <memory/vaddr.h>
#include <stdint.h>
#include <svdpi.h>

void rvcpu_difftest_skip_ref() { difftest_skip_ref(); }

void rvcpu_sim_end(int code) { set_npc_state(NPC_END, cpu.pc, code); }

word_t rvcpu_pmem_ifetch(paddr_t raddr) {
  return vaddr_ifetch((raddr << 2) + PMEM_LEFT, 4);
}

word_t rvcpu_pmem_read(paddr_t raddr) {
  return vaddr_read((raddr << 2) + PMEM_LEFT, 4);
}

void rvcpu_pmem_write(paddr_t waddr, word_t wdata, uint8_t wmask) {
  paddr_t aligned_addr = (waddr << 2) + PMEM_LEFT;
  int len = 0;
  uint32_t offset = 0;
  while ((wmask & 1) == 0) {
    wmask >>= 1;
    wdata >>= 8;
    offset++;
  }
  while ((wmask & 1) == 1) {
    wmask >>= 1;
    len++;
  }
  vaddr_write(aligned_addr + offset, len, wdata);
}

void rvcpu_trace_cache(int hit, uint32_t access_cycle, uint32_t miss_penalty) {
  g_nr_cache_access++;
  g_cache_access_time += access_cycle;
  g_cache_miss_penalty += miss_penalty;
  if (hit) {
    g_nr_cache_hit++;
  }
}

void rvcpu_trace_read(uint32_t addr, int len, uint32_t data) {}

void rvcpu_trace_write(uint32_t addr, int len, uint32_t data) {}

void rvcpu_decode_inst(uint32_t inst, uint32_t npc, uint64_t icache_cost_cycle,
                       uint64_t idu_cost_cycle, uint64_t exu_cost_cycle,
                       uint64_t lsu_cost_cycle, uint64_t wbu_cost_cycle) {
  cur_inst = inst;
  cpu.pc = npc;
#define INSTPAT_INST(s) (inst)
#define INSTPAT_MATCH(s, type, ... /* execute body */)                         \
  { __VA_ARGS__; }

  INSTPAT_START();
  INSTPAT("??????? ????? ????? ??? ????? 0?101 11", UTYPE,
          g_insts[RISCV_AL].total_inst_count++;
          g_insts[RISCV_AL].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_AL].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_AL].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_AL].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_AL].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? ??? ????? 110?1 11", JMP,
          g_insts[RISCV_JMP].total_inst_count++;
          g_insts[RISCV_JMP].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_JMP].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_JMP].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_JMP].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_JMP].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? ??? ????? 11000 11", BRANCH,
          g_insts[RISCV_BRANCH].total_inst_count++;
          g_insts[RISCV_BRANCH].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_BRANCH].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_BRANCH].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_BRANCH].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_BRANCH].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? ??? ????? 00000 11", LOAD,
          g_insts[RISCV_LOAD].total_inst_count++;
          g_insts[RISCV_LOAD].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_LOAD].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_LOAD].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_LOAD].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_LOAD].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? ??? ????? 01000 11", STORE,
          g_insts[RISCV_STORE].total_inst_count++;
          g_insts[RISCV_STORE].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_STORE].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_STORE].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_STORE].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_STORE].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? ??? ????? 0?100 11", AL,
          g_insts[RISCV_AL].total_inst_count++;
          g_insts[RISCV_AL].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_AL].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_AL].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_AL].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_AL].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("0000000 00000 00000 000 00000 11100 11", ECALL,
          g_insts[RISCV_SYSTEM].total_inst_count++;
          g_insts[RISCV_SYSTEM].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_SYSTEM].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_SYSTEM].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_SYSTEM].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_SYSTEM].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("0000000 00001 00000 000 00000 11100 11", EBREAK,
          g_insts[RISCV_SYSTEM].total_inst_count++;
          g_insts[RISCV_SYSTEM].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_SYSTEM].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_SYSTEM].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_SYSTEM].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_SYSTEM].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? ??? ????? 11100 11", CSR,
          g_insts[RISCV_CSR].total_inst_count++;
          g_insts[RISCV_CSR].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_CSR].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_CSR].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_CSR].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_CSR].total_wbu_cost_cycle += wbu_cost_cycle;);
  INSTPAT("??????? ????? ????? 001 ????? 00011 11", FENCE_I,
          g_insts[RISCV_FENCE_I].total_inst_count++;
          g_insts[RISCV_FENCE_I].total_icache_cost_cycle += icache_cost_cycle;
          g_insts[RISCV_FENCE_I].total_idu_cost_cycle += idu_cost_cycle;
          g_insts[RISCV_FENCE_I].total_exu_cost_cycle += exu_cost_cycle;
          g_insts[RISCV_FENCE_I].total_lsu_cost_cycle += lsu_cost_cycle;
          g_insts[RISCV_FENCE_I].total_wbu_cost_cycle += wbu_cost_cycle;);

  INSTPAT("??????? ????? ????? ??? ????? ????? ??", INV,
          panic("Invalid Instruction"));
  INSTPAT_END();
}

void rvcpu_sync_csrs(const svOpenArrayHandle csrState) {
  uint32_t *state = (uint32_t*)svGetArrayPtr(csrState);
  cpu.mstatus = state[0];
  cpu.mtvec = state[1];
  cpu.mepc = state[2];
  cpu.mcause = state[3];
}

void rvcpu_sync_gprs(const svOpenArrayHandle gprState) {
  memcpy(cpu.gpr, svGetArrayPtr(gprState), sizeof(uint32_t) * MUXDEF(CONFIG_RVE, 16, 32));
}
