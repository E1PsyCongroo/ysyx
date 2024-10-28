#ifndef __ISA_DEV_H__
#define __ISA_DEV_H__

#include <common.h>

#ifdef CONFIG_HAS_MROM
#define MROM_LEFT  ((paddr_t)CONFIG_MROM_BASE)
#define MROM_RIGHT ((paddr_t)CONFIG_MROM_BASE + CONFIG_MROM_SIZE - 1)
uint8_t* dev_mrom_to_host(paddr_t addr);
static inline bool in_dev_mrom(paddr_t addr) {
  return addr - CONFIG_MROM_BASE < CONFIG_MROM_SIZE;
}
word_t dev_mrom_read(paddr_t addr, int len);
void dev_mrom_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_HAS_FLASH
#define FLASH_LEFT  ((paddr_t)CONFIG_FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)CONFIG_FLASH_BASE + CONFIG_FLASH_SIZE - 1)
uint8_t* dev_flash_to_host(paddr_t addr);
static inline bool in_dev_flash(paddr_t addr) {
  return addr - CONFIG_FLASH_BASE < CONFIG_FLASH_SIZE;
}
word_t dev_flash_read(paddr_t addr, int len);
void dev_flash_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_HAS_PSRAM
#define PSRAM_LEFT  ((paddr_t)CONFIG_PSRAM_BASE)
#define PSRAM_RIGHT ((paddr_t)CONFIG_PSRAM_BASE + CONFIG_PSRAM_SIZE - 1)
uint8_t* dev_psram_to_host(paddr_t addr);
static inline bool in_dev_psram(paddr_t addr) {
  return addr - CONFIG_PSRAM_BASE < CONFIG_PSRAM_SIZE;
}
word_t dev_psram_read(paddr_t addr, int len);
void dev_psram_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_HAS_SDRAM
#define SDRAM_LEFT  ((paddr_t)CONFIG_SDRAM_BASE)
#define SDRAM_RIGHT ((paddr_t)CONFIG_SDRAM_BASE + CONFIG_SDRAM_SIZE - 1)
uint8_t* dev_sdram_to_host(paddr_t addr);
static inline bool in_dev_sdram(paddr_t addr) {
  return addr - CONFIG_SDRAM_BASE < CONFIG_SDRAM_SIZE;
}
word_t dev_sdram_read(paddr_t addr, int len);
void dev_sdram_write(paddr_t addr, int len, word_t data);
#endif

void init_isa_dev();
word_t isa_dev_read(paddr_t addr, int len);
void isa_dev_write(paddr_t addr, int len, word_t data);

#endif
