#include "local-include/dev.h"
#include <isa.h>
#include <memory/host.h>
#include <memory/paddr.h>

#define ISA_DEVS_MAP(_)                                                        \
  _(CLINT)                                                                     \
  _(SRAM)                                                                      \
  _(UART16550)                                                                 \
  _(SPI)                                                                       \
  _(GPIO)                                                                      \
  _(PS2)                                                                       \
  _(MROM) _(VGA) _(FLASH) _(PSRAM) _(SDRAM) _(CHIPLINK_MMIO) _(CHIPLINK_MEM)

#define in_dev_CLINT in_dev_clint
#define in_dev_SRAM in_dev_sram
#define in_dev_UART16550 in_dev_uart16550
#define in_dev_SPI in_dev_spi
#define in_dev_GPIO in_dev_gpio
#define in_dev_PS2 in_dev_ps2
#define in_dev_MROM in_dev_mrom
#define in_dev_VGA in_dev_vga
#define in_dev_FLASH in_dev_flash
#define in_dev_CHIPLINK_MMIO in_dev_chiplink_mmio
#define in_dev_PSRAM in_dev_psram
#define in_dev_SDRAM in_dev_sdram
#define in_dev_CHIPLINK_MEM in_dev_chiplink_mem

#if defined(CONFIG_MEM_MALLOC)
IFDEF(CONFIG_ISA_HAS_MROM, static uint8_t *mrom = NULL);
IFDEF(CONFIG_ISA_HAS_FLASH, static uint8_t *flash = NULL);
IFDEF(CONFIG_ISA_HAS_PSRAM, static uint8_t *psram = NULL);
IFDEF(CONFIG_ISA_HAS_SDRAM, static uint8_t *sdram = NULL);
#else // CONFIG_MEM_GARRAY
IFDEF(CONFIG_ISA_HAS_MROM, static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_ISA_HAS_FLASH,
      static uint8_t flash[CONFIG_FLASH_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_ISA_HAS_PSRAM,
      static uint8_t psram[CONFIG_PSRAM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_ISA_HAS_SDRAM,
      static uint8_t sdram[CONFIG_SDRAM_SIZE] PG_ALIGN = {});
#endif

#ifdef CONFIG_ISA_HAS_MROM
uint8_t *dev_mrom_to_host(paddr_t addr) {
  return mrom + addr - CONFIG_MROM_BASE;
}

word_t dev_mrom_read(paddr_t addr, int len) {
  return host_read(dev_mrom_to_host(addr), len);
}

void dev_mrom_write(paddr_t addr, int len, word_t data) {
  panic("MROM can't be writen");
}
#endif

#ifdef CONFIG_ISA_HAS_FLASH
uint8_t *dev_flash_to_host(paddr_t addr) {
  return flash + addr - CONFIG_FLASH_BASE;
}

word_t dev_flash_read(paddr_t addr, int len) {
  return host_read(dev_flash_to_host(addr), len);
}

void dev_flash_write(paddr_t addr, int len, word_t data) {
  panic("FLASH can't be writen");
}
#endif

#ifdef CONFIG_ISA_HAS_PSRAM
uint8_t *dev_psram_to_host(paddr_t addr) {
  return psram + addr - CONFIG_PSRAM_BASE;
}

word_t dev_psram_read(paddr_t addr, int len) {
  return host_read(dev_psram_to_host(addr), len);
}

void dev_psram_write(paddr_t addr, int len, word_t data) {
  host_write(dev_psram_to_host(addr), len, data);
}
#endif

#ifdef CONFIG_ISA_HAS_SDRAM
uint8_t *dev_sdram_to_host(paddr_t addr) {
  return sdram + addr - CONFIG_SDRAM_BASE;
}

word_t dev_sdram_read(paddr_t addr, int len) {
  return host_read(dev_sdram_to_host(addr), len);
}

void dev_sdram_write(paddr_t addr, int len, word_t data) {
  host_write(dev_sdram_to_host(addr), len, data);
}
#endif

static void out_of_bound(paddr_t addr) {
#define ISA_DEV_OUT_OF_BOUND(dev)                                              \
  IFDEF(CONFIG_ISA_HAS_##dev,                                                  \
        printf(ANSI_FMT("address = " FMT_PADDR " is out of bound of " #dev     \
                        " [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,   \
                        ANSI_FG_RED) "\n",                                     \
               addr, dev##_LEFT, dev##_RIGHT, cpu.pc));

  ISA_DEVS_MAP(ISA_DEV_OUT_OF_BOUND);
#undef ISA_DEV_OUT_OF_BOUND
  panic("Out Of Bound");
}

void init_isa_dev() {
#if defined(CONFIG_MEM_MALLOC)
  IFDEF(CONFIG_ISA_HAS_MROM, mrom = malloc(CONFIG_MROM_SIZE); assert(mrom));
  IFDEF(CONFIG_ISA_HAS_FLASH, flash = malloc(CONFIG_FLASH_SIZE); assert(flash));
  IFDEF(CONFIG_ISA_HAS_PSRAM, psram = malloc(CONFIG_PSRAM_SIZE); assert(psram));
  IFDEF(CONFIG_ISA_HAS_SDRAM, sdram = malloc(CONFIG_SDRAM_SIZE); assert(sdram));
#endif
#if defined(CONFIG_MEM_RANDOM)
  int randnum = rand();
  IFDEF(CONFIG_ISA_HAS_MROM, memset(mrom, randnum, CONFIG_MROM_SIZE));
  IFDEF(CONFIG_ISA_HAS_FLASH, memset(flash, randnum, CONFIG_FLASH_SIZE));
  IFDEF(CONFIG_ISA_HAS_PSRAM, memset(psram, randnum, CONFIG_PSRAM_SIZE));
  IFDEF(CONFIG_ISA_HAS_SDRAM, memset(sdram, randnum, CONFIG_SDRAM_SIZE));
#endif

#define ISA_DEV_INIT_RANGE(dev)                                                \
  IFDEF(CONFIG_ISA_HAS_##dev, Log(#dev " area [" FMT_PADDR ", " FMT_PADDR "]", \
                                  dev##_LEFT, dev##_RIGHT));

  ISA_DEVS_MAP(ISA_DEV_INIT_RANGE);
#undef ISA_DEV_INIT_RANGE
}

void rvcpu_trace_read(paddr_t addr, int len, word_t data) {
#define ISA_DEV_TRACE_READ(DEV)                                                \
  IFDEF(                                                                       \
      CONFIG_ISA_HAS_##DEV, if (in_dev_##DEV(addr)) {                          \
        IFDEF(CONFIG_MTRACE, if (MTRACE_COND) {                                \
          log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR                           \
                             ", len = %2d, data = " FMT_WORD "\n",             \
                             ANSI_FG_CYAN),                                    \
                    "read " #DEV, addr, len, data);                            \
      })                                                                       \
  return;                                                                      \
  })

  ISA_DEVS_MAP(ISA_DEV_TRACE_READ)
#undef ISA_DEV_TRACE_READ
  out_of_bound(addr);
}

void rvcpu_trace_write(paddr_t addr, int len, word_t data) {
#define ISA_DEV_TRACE_WRITE(DEV)                                               \
  IFDEF(                                                                       \
      CONFIG_ISA_HAS_##DEV, if (in_dev_##DEV(addr)) {                          \
        IFDEF(CONFIG_MTRACE, if (MTRACE_COND) {                                \
          log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR                           \
                             ", len = %2d, data = " FMT_WORD "\n",             \
                             ANSI_FG_CYAN),                                    \
                    "write " #DEV, addr, len, data);                           \
      })                                                                       \
  return;                                                                      \
  })

  ISA_DEVS_MAP(ISA_DEV_TRACE_WRITE)
#undef ISA_DEV_TRACE_WRITE
  out_of_bound(addr);
}
