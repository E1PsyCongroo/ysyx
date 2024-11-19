#include "local-include/dev.h"
#include "local-include/verilating.h"
#include <common.h>
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <isa.h>
#include <memory/paddr.h>
#include <memory/vaddr.h>
#include <stdint.h>

void npc_difftest_skipi_ref() { difftest_skip_ref(); }

void sim_end(int code) { set_npc_state(NPC_END, cpu.pc, code); }

word_t rvcpu_pmem_read(paddr_t raddr) { return vaddr_read(raddr & ~0x3, 4); }

void rvcpu_pmem_write(paddr_t waddr, word_t wdata, uint8_t wmask) {
  paddr_t aligned_addr = waddr & ~0x3;
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

void mrom_read(int32_t addr, int32_t *data) {
  *data = dev_mrom_read(addr & ~0x3u, 4);
}

void flash_read(int32_t addr, int32_t *data) {
  *data = dev_flash_read((addr + CONFIG_FLASH_BASE) & ~0x3u, 4);
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
  case 0b11:
    return;
  case 0b10:
    dev_sdram_write(waddr + CONFIG_SDRAM_BASE, 1, (uint16_t)wdata & 0xff);
    break;
  case 0b01:
    dev_sdram_write(waddr + CONFIG_SDRAM_BASE + 1, 1, (uint16_t)wdata >> 8);
    break;
  case 0b00:
    dev_sdram_write(waddr + CONFIG_SDRAM_BASE, 2, (uint16_t)wdata);
    break;
  default:
    panic("sdram write: unknown dqm %d", dqm);
  }
}

void count_data_fetch(uint32_t cycle) {
  g_nr_fetch_data++;
  g_fetch_data_cycle += cycle;
}

void count_cache_hit(int hit, uint32_t access_cycle, uint32_t miss_penalty) {
  g_nr_cache_access++;
  g_cache_access_time += access_cycle;
  g_cache_miss_penalty += miss_penalty;
  if (hit) {
    g_nr_cache_hit++;
  }
}

void decode_inst(uint32_t inst, uint64_t exec_cycle) {
#define INSTPAT_INST(s) (inst)
#define INSTPAT_MATCH(s, type, ... /* execute body */)                         \
  { __VA_ARGS__; }

  INSTPAT_START();
  INSTPAT("??????? ????? ????? ??? ????? 0?101 11", UTYPE,
          g_insts[RISCV_AL].total_inst_count++;
          g_insts[RISCV_AL].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? ??? ????? 110?1 11", JMP,
          g_insts[RISCV_JMP].total_inst_count++;
          g_insts[RISCV_JMP].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? ??? ????? 11000 11", BRANCH,
          g_insts[RISCV_BRANCH].total_inst_count++;
          g_insts[RISCV_BRANCH].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? ??? ????? 00000 11", LOAD,
          g_insts[RISCV_LOAD].total_inst_count++;
          g_insts[RISCV_LOAD].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? ??? ????? 01000 11", STORE,
          g_insts[RISCV_STORE].total_inst_count++;
          g_insts[RISCV_STORE].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? ??? ????? 0?100 11", AL,
          g_insts[RISCV_AL].total_inst_count++;
          g_insts[RISCV_AL].total_exec_cycle += exec_cycle);
  INSTPAT("0000000 00000 00000 000 00000 11100 11", ECALL,
          g_insts[RISCV_SYSTEM].total_inst_count++;
          g_insts[RISCV_SYSTEM].total_exec_cycle += exec_cycle);
  INSTPAT("0000000 00001 00000 000 00000 11100 11", EBREAK,
          g_insts[RISCV_SYSTEM].total_inst_count++;
          g_insts[RISCV_SYSTEM].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? ??? ????? 11100 11", CSR,
          g_insts[RISCV_CSR].total_inst_count++;
          g_insts[RISCV_CSR].total_exec_cycle += exec_cycle);
  INSTPAT("??????? ????? ????? 001 ????? 00011 11", FENCE_I,
          g_insts[RISCV_FENCE_I].total_inst_count++;
          g_insts[RISCV_FENCE_I].total_exec_cycle += exec_cycle);

  INSTPAT("??????? ????? ????? ??? ????? ????? ??", INV,
          panic("Invalid Instruction"));
  INSTPAT_END();
}