#include <VRVCPU.h>
#include <VRVCPU___024root.h>
#include <verilated_vcd_c.h>

extern "C" {
#include <isa.h>
#include <memory/paddr.h>
#include <memory/vaddr.h>
#include <cpu/cpu.h>
}

extern "C"{

static VRVCPU* rvcpu = nullptr;
static VerilatedContext* contextp = nullptr;
static VerilatedVcdC* tfp = nullptr;
// void nvboard_bind_all_pins(TOP_NAME* top);

void sim_end() {
  set_npc_state(NPC_END, rvcpu->io_pc, 0);
}

word_t pmem_read(paddr_t raddr) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  return paddr_read(raddr & ~0x3u, 4);
}

void pmem_write(paddr_t waddr, word_t wdata, char wmask) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  word_t bit_mask = 0;
  for (uint32_t i = 0; i < sizeof(word_t); i++) {
      if (wmask & (1 << i)) {
          bit_mask |= (0xFF << (i * 8));
      }
  }
  paddr_write(waddr & ~0x3u, 4, wdata & bit_mask);
}

void rvcpu_init(void){
  Verilated::traceEverOn(true);
  contextp = new VerilatedContext;
  rvcpu = new VRVCPU {contextp};
  tfp = new VerilatedVcdC;
  contextp->traceEverOn(true);
  rvcpu->trace(tfp, 99);
  tfp->open("./wave/rvcpu.vcd");
  rvcpu->clock = 0;
}

void rvcpu_exit(void){
  tfp->close();
  delete rvcpu;
  delete contextp;
  delete tfp;
}

void rvcpu_single_cycle(void) {
  rvcpu->io_inst = vaddr_ifetch(rvcpu->io_pc, 4);
  printf("pc: " FMT_WORD ", inst: " FMT_WORD "\n", rvcpu->io_pc, rvcpu->io_inst);
  rvcpu->clock = 1; rvcpu->eval();
  contextp->timeInc(1); tfp->dump(contextp->time());
  rvcpu->clock = 0; rvcpu->eval();
  contextp->timeInc(1); tfp->dump(contextp->time());
}

void rvcpu_reset(int n) {
  rvcpu->reset = 1; rvcpu->eval();
  while (n -- > 0) rvcpu_single_cycle();
  rvcpu->reset = 0; rvcpu->eval();
}

void rvcpu_to_cpu(void) {
  cpu.gpr[0] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_0;
  cpu.gpr[1] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_1;
  cpu.gpr[2] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_2;
  cpu.gpr[3] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_3;
  cpu.gpr[4] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_4;
  cpu.gpr[5] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_5;
  cpu.gpr[6] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_6;
  cpu.gpr[7] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_7;
  cpu.gpr[8] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_8;
  cpu.gpr[9] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_9;
  cpu.gpr[10] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_10;
  cpu.gpr[11] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_11;
  cpu.gpr[12] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_12;
  cpu.gpr[13] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_13;
  cpu.gpr[14] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_14;
  cpu.gpr[15] = rvcpu->rootp->RVCPU__DOT__RegFile__DOT__reg_15;
  cpu.pc = rvcpu->io_pc;
}

}