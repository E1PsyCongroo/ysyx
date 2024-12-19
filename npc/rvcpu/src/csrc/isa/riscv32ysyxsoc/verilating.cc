#include <VysyxSoCFull.h>
#include <VysyxSoCFull__Dpi.h>
#include <VysyxSoCFull___024root.h>
#include <cstdint>
#include <nvboard.h>
#include <verilated.h>
#include <verilated_fst_c.h>

extern "C" {
#include "local-include/verilating.h"
#include <common.h>
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <isa.h>
#include <memory/paddr.h>
}

extern void nvboard_bind_all_pins(VysyxSoCFull* top);

extern "C" {
static VysyxSoCFull *rvcpu = nullptr;
static VerilatedContext *contextp = nullptr;
static VerilatedFstC *tfp = nullptr;
uint32_t cur_inst;

uint64_t g_guest_cycle = 0;
uint64_t g_nr_fetch_inst = 0;

InstStatistic g_insts[RISCV_TOTAL_TYPE] = {{"JMP"},   {"BRANCH"}, {"LOAD"},
                                           {"STORE"}, {"AL"},     {"ECALL"},
                                           {"CSR"},   {"FENCE_I"}};

uint64_t g_nr_cache_access = 0;
uint64_t g_nr_cache_hit = 0;
uint64_t g_cache_access_time = 0;
uint64_t g_cache_miss_penalty = 0;

void rvcpu_init(const char *wave_file, int argc, char **argv) {
  contextp = new VerilatedContext;
  rvcpu = new VysyxSoCFull{contextp};
  tfp = new VerilatedFstC;
  contextp->commandArgs(argc, argv);
  contextp->traceEverOn(true);
  rvcpu->trace(tfp, 0);
  if (wave_file) {
    tfp->open(wave_file);
    Log("Wave is written to %s", wave_file);
  }
  nvboard_bind_all_pins(rvcpu);
  nvboard_init();
  rvcpu_reset();
  cpu.pc = RESET_VECTOR;
  cpu.gpr[0] = 0;
  cpu.mstatus = 0x1800;
  cpu.mcause = 11;
  cpu.priv = static_cast<decltype(cpu.priv)>(0b11);
  /* Exit */
  atexit(rvcpu_exit);
}

void rvcpu_exit(void) {
  tfp->close();
  delete rvcpu;
  delete contextp;
  delete tfp;
  setlocale(LC_NUMERIC, "");
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  double cpi = (double)g_guest_cycle / g_nr_fetch_inst;
  double ipc = (double)g_nr_fetch_inst / g_guest_cycle;
  uint64_t icache_total_cost = 0, idu_total_cost = 0, exu_total_cost = 0,
           lsu_total_cost = 0, wbu_total_cost = 0;
  for (int i = 0; i < RISCV_TOTAL_TYPE; i++) {
    icache_total_cost += g_insts[i].total_icache_cost_cycle;
    idu_total_cost += g_insts[i].total_idu_cost_cycle;
    exu_total_cost += g_insts[i].total_exu_cost_cycle;
    lsu_total_cost += g_insts[i].total_lsu_cost_cycle;
    wbu_total_cost += g_insts[i].total_wbu_cost_cycle;
  }
  double icache_avg_cost = (double)icache_total_cost / g_nr_fetch_inst;
  double idu_avg_cost = (double)idu_total_cost / g_nr_fetch_inst;
  double exu_avg_cost = (double)exu_total_cost / g_nr_fetch_inst;
  double lsu_avg_cost = (double)lsu_total_cost / g_nr_fetch_inst;
  double wbu_avg_cost = (double)wbu_total_cost / g_nr_fetch_inst;
  double icache_hit_ratio = (double)g_nr_cache_hit / g_nr_cache_access * 100;
  double avg_icache_access_cycle =
      (double)g_cache_access_time / g_nr_cache_access;
  double avg_icache_miss_penalty =
      (double)g_cache_miss_penalty / (g_nr_cache_access - g_nr_cache_hit);
  double icache_amat =
      (double)(g_cache_access_time + g_cache_miss_penalty) / g_nr_cache_access;
  Log("total guest cycles = " NUMBERIC_FMT, g_guest_cycle);
  Log("total fetch instructions = " NUMBERIC_FMT, g_nr_fetch_inst);
  Log("CPI(cycle per instruction) = %f", cpi);
  Log("IPC(instruction per cycle) = %f", ipc);
  Log("average icache cost cycle = %f", icache_avg_cost);
  Log("average idu cost cycle = %f", idu_avg_cost);
  Log("average exu cost cycle = %f", exu_avg_cost);
  Log("average lsu cost cycle = %f", lsu_avg_cost);
  Log("average wbu cost cycle = %f", wbu_avg_cost);
  Log("total cache access = " NUMBERIC_FMT, g_nr_cache_access);
  Log("total cache hit = " NUMBERIC_FMT, g_nr_cache_hit);
  Log("icache hit ratio = %f%%", icache_hit_ratio);
  Log("average icache access cycle = %f", avg_icache_access_cycle);
  Log("average icache miss penalty = %f", avg_icache_miss_penalty);
  Log("icache AMAT = %f", icache_amat);
  for (int i = 0; i < RISCV_TOTAL_TYPE; i++) {
    Log("instructions for %s type (\ntotal = " NUMBERIC_FMT ", "
        "\naverage icache cost cycle = %f, "
        "\naverage idu cost cycle = %f, "
        "\naverage exu cost cycle = %f, "
        "\naverage lsu cost cycle = %f, "
        "\naverage wbu cost cycle = %f)",
        g_insts[i].inst_type, g_insts[i].total_inst_count,
        g_insts[i].total_icache_cost_cycle
            ? ((double)g_insts[i].total_icache_cost_cycle /
               g_insts[i].total_inst_count)
            : 0,
        g_insts[i].total_idu_cost_cycle
            ? ((double)g_insts[i].total_idu_cost_cycle /
               g_insts[i].total_inst_count)
            : 0,
        g_insts[i].total_exu_cost_cycle
            ? ((double)g_insts[i].total_exu_cost_cycle /
               g_insts[i].total_inst_count)
            : 0,
        g_insts[i].total_lsu_cost_cycle
            ? ((double)g_insts[i].total_lsu_cost_cycle /
               g_insts[i].total_inst_count)
            : 0,
        g_insts[i].total_wbu_cost_cycle
            ? ((double)g_insts[i].total_wbu_cost_cycle /
               g_insts[i].total_inst_count)
            : 0);
  }
}

void rvcpu_single_cycle(void) {
  /* time down */
  rvcpu->clock = 0;
  rvcpu->eval();
  contextp->timeInc(1);
  tfp->dump(contextp->time());

  /* time up */
  rvcpu->clock = 1;
  rvcpu->eval();
  contextp->timeInc(1);
  tfp->dump(contextp->time());

  nvboard_update();
  g_guest_cycle++;
}

void rvcpu_single_exec(void) {
  g_nr_fetch_inst++;
  while (
      !rvcpu->rootp
           ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___WBU_io_out_valid) {
    rvcpu_single_cycle();
  }
  if (rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___WBU_io_out_valid) {
    rvcpu_single_cycle();
  }
}

void rvcpu_reset(void) {
  rvcpu->reset = 1;
  for (int i = 0; i < 10; i++) {
    rvcpu_single_cycle();
  }
  rvcpu->reset = 0;
  while (
      !rvcpu->rootp
           ->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_0) {
    rvcpu_single_cycle();
  }
  while (
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_0) {
    rvcpu_single_cycle();
  }
  g_guest_cycle = 0;
}

uint32_t rvcpu_ifetch(vaddr_t *pc, int len) {
  (*pc) += len;
  return cur_inst;
}
}
