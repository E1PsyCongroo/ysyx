#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>

#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

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
#undef RESET_VECTOR
#define RESET_VECTOR (FLASH_LEFT + CONFIG_PC_RESET_OFFSET)
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

#endif
