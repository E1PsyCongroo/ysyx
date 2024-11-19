#include <VysyxSoCFull.h>
#include <VysyxSoCFull__Dpi.h>
#include <VysyxSoCFull___024root.h>
#include <cstdint>
#include <nvboard.h>
#include <stdint.h>
#include <verilated.h>
#include <verilated_fst_c.h>

extern "C" {
#include "local-include/verilating.h"
#include <common.h>
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <isa.h>
}

void nvboard_bind_all_pins(VysyxSoCFull *top);

extern "C" {

static VysyxSoCFull *rvcpu = nullptr;
static VerilatedContext *contextp = nullptr;
static VerilatedFstC *tfp = nullptr;
static uint32_t cur_inst;

uint64_t g_guest_cycle = 0;
uint64_t g_nr_fetch_inst = 0;

InstStatistic g_insts[RISCV_TOTAL_TYPE] = {
    {"JMP", 0, 0}, {"BRANCH", 0, 0}, {"LOAD", 0, 0}, {"STORE", 0, 0},
    {"AL", 0, 0},  {"ECALL", 0, 0},  {"CSR", 0, 0}, {"FENCE_I", 0, 0}
};

uint64_t g_nr_fetch_data = 0;
uint64_t g_fetch_data_cycle = 0;

uint64_t g_nr_cache_access = 0;
uint64_t g_nr_cache_hit = 0;
uint64_t g_cache_access_time = 0;
uint64_t g_cache_miss_penalty = 0;

enum {
  RVCPU_IDLE = 0,
  RVCPU_SETADDR = 1,
  RVCPU_FETCH = 2,
  RVCPU_EXEC = 3,
};

static void rvcpu_sync(void) {
  /* synchronizing cpu with rvcpu */
  cpu.pc = rvcpu->rootp
               ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__pc;
  cpu.gpr[0] = 0;
  cpu.gpr[1] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_1;
  cpu.gpr[2] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_2;
  cpu.gpr[3] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_3;
  cpu.gpr[4] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_4;
  cpu.gpr[5] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_5;
  cpu.gpr[6] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_6;
  cpu.gpr[7] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_6;
  cpu.gpr[8] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_8;
  cpu.gpr[9] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_9;
  cpu.gpr[10] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_10;
  cpu.gpr[11] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_11;
  cpu.gpr[12] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_12;
  cpu.gpr[13] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_13;
  cpu.gpr[14] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_14;
  cpu.gpr[15] =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_15;
  cpu.mstatus =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_4_2;
  cpu.mtvec =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_2_2;
  cpu.mepc =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_3_2;
  cpu.mcause =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_0_2;
  cpu.priv = static_cast<decltype(cpu.priv)>(
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__priv);
  /* synchronizing instruction with rvcpu */
  cur_inst =
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__instruction;
}

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
  rvcpu->clock = 1;
  nvboard_bind_all_pins(rvcpu);
  nvboard_init();
  rvcpu_reset();
  rvcpu_sync();
  /* Exit */
  atexit(rvcpu_exit);
}

void rvcpu_exit(void) {
  tfp->close();
  delete rvcpu;
  delete contextp;
  delete tfp;
  nvboard_quit();
  setlocale(LC_NUMERIC, "");
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  Log("total guest cycles = " NUMBERIC_FMT, g_guest_cycle);
  Log("total fetch instructions = " NUMBERIC_FMT, g_nr_fetch_inst);
  Log("CPI(cycle per instruction) = %f",
      (double)g_guest_cycle / g_nr_fetch_inst);
  Log("IPC(instruction per cycle) = %f",
      (double)g_nr_fetch_inst / g_guest_cycle);
  Log("total cache access = " NUMBERIC_FMT, g_nr_cache_access);
  Log("total cache hit = " NUMBERIC_FMT, g_nr_cache_hit);
  Log("cache hit ratio = %f%%",
      (double)g_nr_cache_hit / g_nr_cache_access * 100);
  Log("average cache access time = %f",
      (double)g_cache_access_time / g_nr_cache_access);
  Log("average cache miss penalty = %f",
      (double)g_cache_miss_penalty / (g_nr_cache_access - g_nr_cache_hit));
  Log("cache AMAT = %f",
      ((double)g_cache_access_time + g_cache_miss_penalty) / g_nr_cache_access);
  Log("total fetch data = " NUMBERIC_FMT, g_nr_fetch_data);
  Log("average cycle of fetch data = %f",
      (double)g_fetch_data_cycle / g_nr_fetch_data);
  for (int i = 0; i < RISCV_TOTAL_TYPE; i++) {
    Log("instructions for %s type (total = " NUMBERIC_FMT
        ", average exec cycle = %f)",
        g_insts[i].inst_type, g_insts[i].total_inst_count,
        ((double)g_insts[i].total_exec_cycle / g_insts[i].total_inst_count));
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

  rvcpu_sync();
  nvboard_update();
  g_guest_cycle++;
}

void rvcpu_single_exec(void) {
  /* Fetch Instruction */
  while (
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__state !=
      RVCPU_EXEC) {
    rvcpu_single_cycle();
  }
  g_nr_fetch_inst++;
  /* Exec Instruction */
  uint64_t cur_cycle = g_guest_cycle;
  while (
      rvcpu->rootp
          ->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__state !=
      RVCPU_SETADDR) {
    rvcpu_single_cycle();
  }
  void decode_inst(uint32_t inst, uint64_t exec_cycle);
  decode_inst(cur_inst, g_guest_cycle - cur_cycle);
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
