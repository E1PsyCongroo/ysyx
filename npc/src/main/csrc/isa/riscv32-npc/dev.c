#include "local-include/dev.h"
#include <isa.h>
#include <memory/host.h>
#include <memory/paddr.h>

#if defined(CONFIG_MEM_MALLOC)
IFDEF(CONFIG_HAS_MROM, static uint8_t *mrom = NULL);
IFDEF(CONFIG_HAS_FLASH, static uint8_t *flash = NULL);
IFDEF(CONFIG_HAS_PSRAM, static uint8_t *psram = NULL);
IFDEF(CONFIG_HAS_SDRAM, static uint8_t *sdram = NULL);
#else // CONFIG_MEM_GARRAY
IFDEF(CONFIG_HAS_MROM, static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_FLASH, static uint8_t flash[CONFIG_FLASH_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_PSRAM, static uint8_t psram[CONFIG_PSRAM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_SDRAM, static uint8_t sdram[CONFIG_SDRAM_SIZE] PG_ALIGN = {});
#endif

#ifdef CONFIG_HAS_MROM
uint8_t *dev_mrom_to_host(paddr_t addr) {
  return mrom + addr - CONFIG_MROM_BASE;
}

word_t dev_mrom_read(paddr_t addr, int len) {
  word_t data = host_read(dev_mrom_to_host(addr), len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) {
    log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD
                       "\n",
                       ANSI_FG_CYAN),
              "read mrom", addr, len, data);
  }
#endif
  return data;
}

void dev_mrom_write(paddr_t addr, int len, word_t data) {
  panic("mrom can't be writen");
  host_write(dev_mrom_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_HAS_FLASH
uint8_t *dev_flash_to_host(paddr_t addr) {
  return flash + addr - CONFIG_FLASH_BASE;
}

word_t dev_flash_read(paddr_t addr, int len) {
  word_t data = host_read(dev_flash_to_host(addr), len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) {
    log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD
                       "\n",
                       ANSI_FG_CYAN),
              "read flash", addr, len, data);
  }
#endif
  return data;
}

void dev_flash_write(paddr_t addr, int len, word_t data) {
  panic("flash can't be writen");
  host_write(dev_flash_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_HAS_PSRAM
uint8_t *dev_psram_to_host(paddr_t addr) {
  return psram + addr - CONFIG_PSRAM_BASE;
}

word_t dev_psram_read(paddr_t addr, int len) {
  word_t data = host_read(dev_psram_to_host(addr), len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) {
    log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD
                       "\n",
                       ANSI_FG_CYAN),
              "read psram", addr, len, data);
  }
#endif
  return data;
}

void dev_psram_write(paddr_t addr, int len, word_t data) {
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) {
    log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD
                       "\n",
                       ANSI_FG_CYAN),
              "write psram", addr, len, data);
  }
#endif
  host_write(dev_psram_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_HAS_SDRAM
uint8_t *dev_sdram_to_host(paddr_t addr) {
  return sdram + addr - CONFIG_SDRAM_BASE;
}

word_t dev_sdram_read(paddr_t addr, int len) {
  word_t data = host_read(dev_sdram_to_host(addr), len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) {
    log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD
                       "\n",
                       ANSI_FG_CYAN),
              "read sdram", addr, len, data);
  }
#endif
  return data;
}

void dev_sdram_write(paddr_t addr, int len, word_t data) {
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) {
    log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD
                       "\n",
                       ANSI_FG_CYAN),
              "write sdram", addr, len, data);
  }
#endif
  host_write(dev_sdram_to_host(addr), len, data);
}
#endif

static void out_of_bound(paddr_t addr) {
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

void init_isa_dev() {
#if defined(CONFIG_MEM_MALLOC)
  IFDEF(CONFIG_HAS_MROM, mrom = malloc(CONFIG_MROM_SIZE); assert(mrom));
  IFDEF(CONFIG_HAS_FLASH, flash = malloc(CONFIG_FLASH_SIZE); assert(flash));
  IFDEF(CONFIG_HAS_PSRAM, psram = malloc(CONFIG_PSRAM_SIZE); assert(flash));
  IFDEF(CONFIG_HAS_SDRAM, sdram = malloc(CONFIG_SDRAM_SIZE); assert(flash));
#endif
#if defined(CONFIG_MEM_RANDOM)
  int randnum = rand();
  IFDEF(CONFIG_HAS_MROM, memset(mrom, randnum, CONFIG_MROM_SIZE));
  IFDEF(CONFIG_HAS_FLASH, memset(flash, randnum, CONFIG_FLASH_SIZE));
  IFDEF(CONFIG_HAS_PSRAM, memset(psram, randnum, CONFIG_PSRAM_SIZE));
  IFDEF(CONFIG_HAS_SDRAM, memset(sdram, randnum, CONFIG_SDRAM_SIZE));
#endif
  IFDEF(CONFIG_HAS_MROM,
        Log("mrom area [" FMT_PADDR ", " FMT_PADDR "]", MROM_LEFT, MROM_RIGHT));
  IFDEF(CONFIG_HAS_FLASH, Log("flash area [" FMT_PADDR ", " FMT_PADDR "]",
                              FLASH_LEFT, FLASH_RIGHT));
  IFDEF(CONFIG_HAS_PSRAM, Log("psram area [" FMT_PADDR ", " FMT_PADDR "]",
                              PSRAM_LEFT, PSRAM_RIGHT));
  IFDEF(CONFIG_HAS_SDRAM, Log("sdram area [" FMT_PADDR ", " FMT_PADDR "]",
                              SDRAM_LEFT, SDRAM_RIGHT));
}

word_t isa_dev_read(paddr_t addr, int len) {
  IFDEF(CONFIG_HAS_MROM,
        if (likely(in_dev_mrom(addr))) return dev_mrom_read(addr, len);)
  IFDEF(CONFIG_HAS_FLASH,
        if (likely(in_dev_flash(addr))) return dev_flash_read(addr, len);)
  IFDEF(CONFIG_HAS_PSRAM,
        if (likely(in_dev_psram(addr))) return dev_psram_read(addr, len);)
  IFDEF(CONFIG_HAS_SDRAM,
        if (likely(in_dev_sdram(addr))) return dev_sdram_read(addr, len);)
  out_of_bound(addr);
  return 0;
}

void isa_dev_write(paddr_t addr, int len, word_t data) {
  IFDEF(
      CONFIG_HAS_MROM, if (likely(in_dev_mrom(addr))) {
        dev_mrom_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_FLASH, if (likely(in_dev_flash(addr))) {
        dev_flash_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_PSRAM, if (likely(in_dev_psram(addr))) {
        dev_psram_write(addr, len, data);
        return;
      })
  IFDEF(
      CONFIG_HAS_SDRAM, if (likely(in_dev_sdram(addr))) {
        dev_sdram_write(addr, len, data);
        return;
      })
  out_of_bound(addr);
}
