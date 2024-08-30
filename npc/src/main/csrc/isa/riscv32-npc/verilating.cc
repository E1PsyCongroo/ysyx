#include <VRVCPU.h>
#include <VRVCPU___024root.h>
#include <VRVCPU__Dpi.h>
#include <verilated_vcd_c.h>

extern "C" {
#include <common.h>
#include <isa.h>
#include <memory/vaddr.h>
#include <memory/paddr.h>
#include <cpu/cpu.h>
#include "local-include/verilating.h"
}

extern "C"{

static VRVCPU* rvcpu = nullptr;
static VerilatedContext* contextp = nullptr;
static VerilatedVcdC* tfp = nullptr;
static uint32_t cur_inst;
uint64_t g_nr_guest_cycle = 0;
// void nvboard_bind_all_pins(TOP_NAME* top);

enum {
  RVCPU_FETCH = 0,
  RVCPU_EXEC  = 1,
  RVCPU_RESET = 2,
};

void sim_end() {
  set_npc_state(NPC_END, cpu.pc, 0);
}

word_t rvcpu_pmem_ifetch(paddr_t raddr) {
  return vaddr_ifetch(raddr, 4);
}

word_t rvcpu_pmem_read(paddr_t raddr) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  // if (!in_pmem(raddr)) return 0;
  // printf("RVCPU read: " FMT_PADDR "\n", raddr);
  return vaddr_read(raddr & ~0x3u, 4);
}

void rvcpu_pmem_write(paddr_t waddr, word_t wdata, char wmask) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  // if (!in_pmem(waddr)) return;
  // printf("RVCPU write: " FMT_PADDR "\n", waddr);
  word_t bit_mask = 0;
  int len = 0;
  for (uint32_t i = 0; i < sizeof(word_t); i++) {
    if (wmask & (1 << i)) {
      bit_mask |= (0xFF << (len * 8));
      len++;
    }
  }
  vaddr_write(waddr, len, wdata & bit_mask);
}

static void rvcpu_sync(void) {
  /* synchronizing cpu with rvcpu */
  cpu.pc      = rvcpu->rootp->RVCPU__DOT__IFU__DOT__pc;
  cpu.gpr[0]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_0;
  cpu.gpr[1]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_1;
  cpu.gpr[2]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_2;
  cpu.gpr[3]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_3;
  cpu.gpr[4]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_4;
  cpu.gpr[5]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_5;
  cpu.gpr[6]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_6;
  cpu.gpr[7]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_7;
  cpu.gpr[8]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_8;
  cpu.gpr[9]  = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_9;
  cpu.gpr[10] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_10;
  cpu.gpr[11] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_11;
  cpu.gpr[12] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_12;
  cpu.gpr[13] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_13;
  cpu.gpr[14] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_14;
  cpu.gpr[15] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_15;
  cpu.mstatus = rvcpu->rootp->RVCPU__DOT__EXU__DOT__CSRControl__DOT__csrs_0_2;
  cpu.mtvec   = rvcpu->rootp->RVCPU__DOT__EXU__DOT__CSRControl__DOT__csrs_1_2;
  cpu.mepc    = rvcpu->rootp->RVCPU__DOT__EXU__DOT__CSRControl__DOT__csrs_2_2;
  cpu.mcause  = rvcpu->rootp->RVCPU__DOT__EXU__DOT__CSRControl__DOT__csrs_3_2;
  cpu.priv    = static_cast<decltype(cpu.priv)>
                (rvcpu->rootp->RVCPU__DOT__EXU__DOT__CSRControl__DOT__priv);
  /* synchronizing instruction with rvcpu */
  cur_inst    = rvcpu->rootp->RVCPU__DOT__IFU__DOT__instruction;
}

void rvcpu_init(const char* wave_file){
  Verilated::traceEverOn(true);
  contextp = new VerilatedContext;
  rvcpu = new VRVCPU {contextp};
  tfp = new VerilatedVcdC;
  contextp->traceEverOn(true);
  rvcpu->trace(tfp, 5);
  if (wave_file) {
    tfp->open(wave_file);
    Log("Wave is written to %s", wave_file);
  }
  rvcpu->clock = 1;
  rvcpu_sync();
  /* Exit */
  atexit(rvcpu_exit);
}

void rvcpu_exit(void){
  tfp->close();
  delete rvcpu;
  delete contextp;
  delete tfp;
}

void rvcpu_single_cycle(void) {
  /* time down */
  rvcpu->clock = 0; rvcpu->eval();
  contextp->timeInc(1); tfp->dump(contextp->time());
  rvcpu_sync();
  // if (rvcpu->rootp->RVCPU__DOT__IFU__DOT__state == RVCPU_EXEC) {
  //   printf("pc: " FMT_WORD ", inst: " FMT_WORD "\n", rvcpu->rootp->RVCPU__DOT__IFU__DOT__pc, rvcpu->rootp->RVCPU__DOT__IFU__DOT__instruction);
  // }
  /* time up */
  rvcpu->clock = 1; rvcpu->eval();
  contextp->timeInc(1); tfp->dump(contextp->time());
  rvcpu_sync();
  g_nr_guest_cycle++;
}

void rvcpu_single_exec(void) {
  /* Fetch Instruction */
  while (rvcpu->rootp->RVCPU__DOT__IFU__DOT__state != RVCPU_EXEC) {
    rvcpu_single_cycle();
  }
  /* Execute Instruction */
  while (rvcpu->rootp->RVCPU__DOT__IFU__DOT__state == RVCPU_EXEC) {
    rvcpu_single_cycle();
  }
}

void rvcpu_reset(int n) {
  rvcpu->reset = 1; rvcpu->eval();
  while (n -- > 0) rvcpu_single_cycle();
  rvcpu->reset = 0; rvcpu->eval();
  g_nr_guest_cycle = 0;

  // for (int i = 0; i < 50; i++) {
  //   rvcpu_single_cycle();
  // }
  // exit(0);
}

uint32_t rvcpu_ifetch(vaddr_t *pc, int len) {
  (*pc) += len;
  return cur_inst;
}

}
