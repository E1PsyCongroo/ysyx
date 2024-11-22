/***************************************************************************************
 * Copyright (c) 2014-2022 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan
 *PSL v2. You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
 *KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

#include "common.h"
#include <device/mmio.h>
#include <isa.h>
#include <memory/host.h>
#include <memory/paddr.h>

#if defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
IFDEF(CONFIG_HAS_MROM, static uint8_t *mrom = NULL);
IFDEF(CONFIG_HAS_FLASH, static uint8_t *flash = NULL);
IFDEF(CONFIG_HAS_SRAM, static uint8_t *sram = NULL);
IFDEF(CONFIG_HAS_PSRAM, static uint8_t *psram = NULL);
IFDEF(CONFIG_HAS_SDRAM, static uint8_t *sdram = NULL);
#else // CONFIG_PMEM_GARRAY
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
IFDEF(CONFIG_HAS_MROM, static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_FLASH, static uint8_t flash[CONFIG_FLASH_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_SRAM, static uint8_t sram[CONFIG_SRAM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_PSRAM, static uint8_t psram[CONFIG_PSRAM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_SDRAM, static uint8_t sdram[CONFIG_SDRAM_SIZE] PG_ALIGN = {});
#endif

#ifdef CONFIG_HAS_MROM
uint8_t *mrom_to_host(paddr_t addr) { return mrom + addr - CONFIG_MROM_BASE; }

static word_t mrom_read(paddr_t addr, int len) {
  return host_read(mrom_to_host(addr), len);
}

static void mrom_write(paddr_t addr, int len, word_t data) {
  panic("mrom can't be writen");
}
#endif

#ifdef CONFIG_HAS_FLASH
uint8_t *flash_to_host(paddr_t addr) {
  return flash + addr - CONFIG_FLASH_BASE;
}

static word_t flash_read(paddr_t addr, int len) {
  return host_read(flash_to_host(addr), len);
}

static void flash_write(paddr_t addr, int len, word_t data) {
  host_write(flash_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_HAS_SRAM
uint8_t *sram_to_host(paddr_t addr) { return sram + addr - CONFIG_SRAM_BASE; }

static word_t sram_read(paddr_t addr, int len) {
  return host_read(sram_to_host(addr), len);
}

static void sram_write(paddr_t addr, int len, word_t data) {
  host_write(sram_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_HAS_PSRAM
uint8_t *psram_to_host(paddr_t addr) {
  return psram + addr - CONFIG_PSRAM_BASE;
}

static word_t psram_read(paddr_t addr, int len) {
  return host_read(psram_to_host(addr), len);
}

static void psram_write(paddr_t addr, int len, word_t data) {
  host_write(psram_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_HAS_SDRAM
uint8_t *sdram_to_host(paddr_t addr) {
  return sdram + addr - CONFIG_SDRAM_BASE;
}

static word_t sdram_read(paddr_t addr, int len) {
  return host_read(sdram_to_host(addr), len);
}

static void sdram_write(paddr_t addr, int len, word_t data) {
  host_write(sdram_to_host(addr), len, data);
}
#endif

uint8_t *guest_to_host(paddr_t paddr) {
  if (in_pmem(paddr)) {
    return pmem + paddr - CONFIG_MBASE;
  }
  IFDEF(
      CONFIG_HAS_MROM, if (in_mrom(paddr)) { mrom + paddr - CONFIG_MROM_BASE; })
  IFDEF(
      CONFIG_HAS_FLASH,
      if (in_flash(paddr)) { flash + paddr - CONFIG_FLASH_BASE; })
  IFDEF(
      CONFIG_HAS_SRAM, if (in_sram(paddr)) { sram + paddr - CONFIG_SRAM_BASE; })
  IFDEF(
      CONFIG_HAS_PSRAM,
      if (in_psram(paddr)) { psram + paddr - CONFIG_PSRAM_BASE; })
  IFDEF(
      CONFIG_HAS_SDRAM,
      if (in_sdram(paddr)) { sdram + paddr - CONFIG_SDRAM_BASE; })
  TODO();
}
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR
        ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
#ifdef CONFIG_HAS_MROM
  panic("address = " FMT_PADDR " is out of bound of mrom [" FMT_PADDR
        ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, MROM_LEFT, MROM_RIGHT, cpu.pc);
#endif
#ifdef CONFIG_HAS_FLASH
  panic("address = " FMT_PADDR " is out of bound of flash [" FMT_PADDR
        ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, FLASH_LEFT, FLASH_RIGHT, cpu.pc);
#endif
#ifdef CONFIG_HAS_SRAM
  panic("address = " FMT_PADDR " is out of bound of sram [" FMT_PADDR
        ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, SRAM_LEFT, SRAM_RIGHT, cpu.pc);
#endif
#ifdef CONFIG_HAS_PSRAM
  panic("address = " FMT_PADDR " is out of bound of psram [" FMT_PADDR
        ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, PSRAM_LEFT, PSRAM_RIGHT, cpu.pc);
#endif
#ifdef CONFIG_HAS_SDRAM
  panic("address = " FMT_PADDR " is out of bound of sdram [" FMT_PADDR
        ", " FMT_PADDR "] at pc = " FMT_WORD,
        addr, SDRAM_LEFT, SDRAM_RIGHT, cpu.pc);
#endif
}

void init_mem() {
#if defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
  IFDEF(CONFIG_HAS_MROM, mrom = malloc(CONFIG_MROM_SIZE); assert(mrom));
  IFDEF(CONFIG_HAS_FLASH, flash = malloc(CONFIG_flash_SIZE); assert(flash));
  IFDEF(CONFIG_HAS_SRAM, sram = malloc(CONFIG_SRAM_SIZE); assert(sram));
  IFDEF(CONFIG_HAS_PSRAM, psram = malloc(CONFIG_PSRAM_SIZE); assert(psram));
  IFDEF(CONFIG_HAS_SDRAM, sdram = malloc(CONFIG_SDRAM_SIZE); assert(sdram));
#endif
#if defined(CONFIG_MEM_RANDOM)
  int randnum = rand();
  memset(pmem, randnum, CONFIG_MSIZE);
  IFDEF(CONFIG_HAS_MROM, memset(mrom, randnum, CONFIG_MROM_SIZE));
  IFDEF(CONFIG_HAS_FLASH, memset(flash, randnum, CONFIG_FLASH_SIZE));
  IFDEF(CONFIG_HAS_SRAM, memset(sram, randnum, CONFIG_SRAM_SIZE));
  IFDEF(CONFIG_HAS_PSRAM, memset(psram, randnum, CONFIG_PSRAM_SIZE));
  IFDEF(CONFIG_HAS_SDRAM, memset(sdram, randnum, CONFIG_SDRAM_SIZE));
#endif
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT,
      PMEM_RIGHT);
  IFDEF(CONFIG_HAS_MROM,
        Log("mrom area [" FMT_PADDR ", " FMT_PADDR "]", MROM_LEFT, MROM_RIGHT));
  IFDEF(CONFIG_HAS_FLASH, Log("flash area [" FMT_PADDR ", " FMT_PADDR "]",
                              FLASH_LEFT, FLASH_RIGHT));
  IFDEF(CONFIG_HAS_SRAM,
        Log("sram area [" FMT_PADDR ", " FMT_PADDR "]", SRAM_LEFT, SRAM_RIGHT));
  IFDEF(CONFIG_HAS_PSRAM, Log("psram area [" FMT_PADDR ", " FMT_PADDR "]",
                              PSRAM_LEFT, PSRAM_RIGHT));
  IFDEF(CONFIG_HAS_SDRAM, Log("sdram area [" FMT_PADDR ", " FMT_PADDR "]",
                              SDRAM_LEFT, SDRAM_RIGHT));
}

word_t paddr_read(paddr_t addr, int len) {
  if (likely(in_pmem(addr)))
    return pmem_read(addr, len);
  IFDEF(CONFIG_HAS_MROM,
        if (likely(in_mrom(addr))) return mrom_read(addr, len);)
  IFDEF(CONFIG_HAS_FLASH,
        if (likely(in_flash(addr))) return flash_read(addr, len);)
  IFDEF(CONFIG_HAS_SRAM,
        if (likely(in_sram(addr))) return sram_read(addr, len);)
  IFDEF(CONFIG_HAS_PSRAM,
        if (likely(in_psram(addr))) return psram_read(addr, len);)
  IFDEF(CONFIG_HAS_SDRAM,
        if (likely(in_sdram(addr))) return sdram_read(addr, len);)
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  if (likely(in_pmem(addr))) {
    pmem_write(addr, len, data);
    return;
  }
  IFDEF(
      CONFIG_HAS_MROM, if (likely(in_mrom(addr))) {
        mrom_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_FLASH, if (likely(in_flash(addr))) {
        flash_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_SRAM, if (likely(in_sram(addr))) {
        sram_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_PSRAM, if (likely(in_psram(addr))) {
        psram_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_SDRAM, if (likely(in_sdram(addr))) {
        sdram_write(addr, len, data);
        return;
      })
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
