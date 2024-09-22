#include "nvboard.h"
#include <VysyxSoCFull.h>
#include <VysyxSoCFull___024root.h>
#include <VysyxSoCFull__Dpi.h>
#include <stdint.h>
#include <verilated_vcd_c.h>
#include <nvboard.h>

extern "C" {
#include <common.h>
#include <isa.h>
#include <memory/vaddr.h>
#include <memory/paddr.h>
#include <cpu/cpu.h>
#include <cpu/difftest.h>
#include "local-include/verilating.h"
}

void nvboard_bind_all_pins(VysyxSoCFull* top);

extern "C"{

static VysyxSoCFull* rvcpu = nullptr;
static VerilatedContext* contextp = nullptr;
static VerilatedVcdC* tfp = nullptr;
static uint32_t cur_inst;
uint64_t g_nr_guest_cycle = 0;

enum {
  RVCPU_IDLE    = 0,
  RVCPU_SETADDR = 1,
  RVCPU_FETCH   = 2,
  RVCPU_EXEC    = 3,
};

void npc_difftest_skipi_ref() {
  difftest_skip_ref();
}

void sim_end(int code) {
  set_npc_state(NPC_END, cpu.pc, code);
}

word_t rvcpu_pmem_ifetch(paddr_t raddr) {
  return vaddr_ifetch(raddr, 4);
}

word_t rvcpu_pmem_read(paddr_t raddr) {
  return vaddr_read(raddr & ~0x3u, 4);
}

void rvcpu_pmem_write(paddr_t waddr, word_t wdata, char wmask) {
  word_t bit_mask = 0;
  int len = 0;
  paddr_t addr = waddr;
  uint32_t offset = 0;
  for (uint32_t i = 0; i < sizeof(word_t); i++) {
    if (wmask & (1 << i)) {
      bit_mask |= (0xFF << (len * 8));
      len++;
      addr = waddr + offset;
    } else {
      offset += 1;
    }
  }
  vaddr_write(addr, len, wdata & bit_mask);
}

void mrom_read(int32_t addr, int32_t *data) {
  *data = dev_mrom_read(addr & ~0x3u, 4);
}

void flash_read(int32_t addr, int32_t *data) {
  *data = dev_flash_read((addr + CONFIG_FLASH_BASE) & ~0x3u, 4);
}

void sram_read(int32_t addr, int32_t data) {
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "read sram", addr, 4, data); }
#endif
}

void sram_write(int32_t waddr, int32_t wdata, int len) {
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "write sram", waddr, len, wdata); }
#endif
}

void psram_read(int32_t addr, int32_t *data) {
  *data = dev_psram_read((addr + CONFIG_PSRAM_BASE) & ~0x3u, 4);
}

void psram_write(int32_t waddr, int32_t wdata, int len) {
  dev_psram_write(waddr + CONFIG_PSRAM_BASE, len, wdata);
}

void sdram_read(int32_t addr, int16_t *data) {
  *data = dev_sdram_read(addr + CONFIG_SDRAM_BASE, 2);
}

void sdram_write(int32_t waddr, int16_t wdata, char dqm) {
  switch (dqm) {
    case 0b11: return;
    case 0b10: dev_sdram_write(waddr + CONFIG_SDRAM_BASE, 1, (uint16_t)wdata & 0xff)  ;break;
    case 0b01: dev_sdram_write(waddr + CONFIG_SDRAM_BASE + 1, 1, (uint16_t)wdata >> 8);break;
    case 0b00: dev_sdram_write(waddr + CONFIG_SDRAM_BASE, 2, (uint16_t)wdata)         ;break;
    default: panic("sdram write: unknown dqm %d", dqm);
  }
}


static void rvcpu_sync(void) {
  /* synchronizing cpu with rvcpu */
  cpu.pc      = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__pc;
  cpu.gpr[0]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_0;
  cpu.gpr[1]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_1;
  cpu.gpr[2]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_2;
  cpu.gpr[3]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_3;
  cpu.gpr[4]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_4;
  cpu.gpr[5]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_5;
  cpu.gpr[6]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_6;
  cpu.gpr[7]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_7;
  cpu.gpr[8]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_8;
  cpu.gpr[9]  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_9;
  cpu.gpr[10] = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_10;
  cpu.gpr[11] = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_11;
  cpu.gpr[12] = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_12;
  cpu.gpr[13] = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_13;
  cpu.gpr[14] = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_14;
  cpu.gpr[15] = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__RegFile__DOT__reg_15;
  cpu.mstatus = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_4_2;
  cpu.mtvec   = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_2_2;
  cpu.mepc    = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_3_2;
  cpu.mcause  = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__csrs_0_2;
  cpu.priv    = static_cast<decltype(cpu.priv)>
                (rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__EXU__DOT__CSRControl__DOT__priv);
  /* synchronizing instruction with rvcpu */
  cur_inst    = rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__instruction;
}

void rvcpu_init(const char* wave_file, int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  Verilated::traceEverOn(true);
  contextp = new VerilatedContext;
  rvcpu = new VysyxSoCFull {contextp};
  tfp = new VerilatedVcdC;
  contextp->traceEverOn(true);
  rvcpu->trace(tfp, 5);
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

void rvcpu_exit(void){
  tfp->close();
  delete rvcpu;
  delete contextp;
  delete tfp;
  nvboard_quit();
}

void rvcpu_single_cycle(void) {
  /* time down */
  rvcpu->clock = 0; rvcpu->eval();
  contextp->timeInc(1); tfp->dump(contextp->time());
  rvcpu_sync();

  // if (rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__state == RVCPU_EXEC) {
  //   printf("pc: " FMT_WORD ", inst: " FMT_WORD "\n", rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__pc, rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__instruction);
  // }

  /* time up */
  rvcpu->clock = 1; rvcpu->eval();
  contextp->timeInc(1); tfp->dump(contextp->time());
  rvcpu_sync();
  nvboard_update();
  g_nr_guest_cycle++;
}

void rvcpu_single_exec(void) {
  /* Exec Instruction */
  while (rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__state != RVCPU_IDLE) {
    rvcpu_single_cycle();
  }
  /* Fetch Instruction */
  while (rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__IFU__DOT__state == RVCPU_IDLE) {
    rvcpu_single_cycle();
  }
}

void rvcpu_reset(void) {
  g_nr_guest_cycle = 0;
  rvcpu->reset = 1; rvcpu_single_cycle();
  rvcpu->reset = 0;
  while (!rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_0) {
    rvcpu_single_cycle();
  }
  while (rvcpu->rootp->ysyxSoCFull__DOT__asic__DOT__cpu_reset_chain__DOT__output_chain__DOT__sync_0) {
    rvcpu_single_cycle();
  }

  // for (int i = 0; i < 2000; i++) {
  //   rvcpu_single_cycle();
  // }
  // exit(0);
}

uint32_t rvcpu_ifetch(vaddr_t *pc, int len) {
  (*pc) += len;
  return cur_inst;
}

}
