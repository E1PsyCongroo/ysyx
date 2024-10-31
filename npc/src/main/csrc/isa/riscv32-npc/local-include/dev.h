#ifndef __ISA_DEV_H__
#define __ISA_DEV_H__

#include <common.h>

#ifdef CONFIG_ISA_HAS_CLINT
#define CLINT_LEFT  ((paddr_t)CONFIG_CLINT_BASE)
#define CLINT_RIGHT ((paddr_t)CONFIG_CLINT_BASE + CONFIG_CLINT_SIZE - 1)
static inline bool in_dev_clint(paddr_t addr) {
  return addr - CONFIG_CLINT_BASE < CONFIG_CLINT_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_SRAM
#define SRAM_LEFT  ((paddr_t)CONFIG_SRAM_BASE)
#define SRAM_RIGHT ((paddr_t)CONFIG_SRAM_BASE + CONFIG_SRAM_SIZE - 1)
static inline bool in_dev_sram(paddr_t addr) {
  return addr - CONFIG_SRAM_BASE < CONFIG_SRAM_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_UART16550
#define UART16550_LEFT  ((paddr_t)CONFIG_UART16550_BASE)
#define UART16550_RIGHT ((paddr_t)CONFIG_UART16550_BASE + CONFIG_UART16550_SIZE - 1)
static inline bool in_dev_uart16550(paddr_t addr) {
  return addr - CONFIG_UART16550_BASE < CONFIG_UART16550_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_SPI
#define SPI_LEFT  ((paddr_t)CONFIG_SPI_BASE)
#define SPI_RIGHT ((paddr_t)CONFIG_SPI_BASE + CONFIG_SPI_SIZE - 1)
static inline bool in_dev_spi(paddr_t addr) {
  return addr - CONFIG_SPI_BASE < CONFIG_SPI_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_GPIO
#define GPIO_LEFT  ((paddr_t)CONFIG_GPIO_BASE)
#define GPIO_RIGHT ((paddr_t)CONFIG_GPIO_BASE + CONFIG_GPIO_SIZE - 1)
static inline bool in_dev_gpio(paddr_t addr) {
  return addr - CONFIG_GPIO_BASE < CONFIG_GPIO_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_PS2
#define PS2_LEFT  ((paddr_t)CONFIG_GPIO_BASE)
#define PS2_RIGHT ((paddr_t)CONFIG_GPIO_BASE + CONFIG_GPIO_SIZE - 1)
static inline bool in_dev_ps2(paddr_t addr) {
  return addr - CONFIG_PS2_BASE < CONFIG_GPIO_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_MROM
#define MROM_LEFT  ((paddr_t)CONFIG_MROM_BASE)
#define MROM_RIGHT ((paddr_t)CONFIG_MROM_BASE + CONFIG_MROM_SIZE - 1)
uint8_t* dev_mrom_to_host(paddr_t addr);
static inline bool in_dev_mrom(paddr_t addr) {
  return addr - CONFIG_MROM_BASE < CONFIG_MROM_SIZE;
}
word_t dev_mrom_read(paddr_t addr, int len);
void dev_mrom_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_ISA_HAS_VGA
#define VGA_LEFT  ((paddr_t)CONFIG_VGA_BASE)
#define VGA_RIGHT ((paddr_t)CONFIG_VGA_BASE + CONFIG_VGA_SIZE - 1)
static inline bool in_dev_vga(paddr_t addr) {
  return addr - CONFIG_VGA_BASE < CONFIG_VGA_SIZE;
}
#endif

#ifdef CONFIG_ISA_HAS_FLASH
#define FLASH_LEFT  ((paddr_t)CONFIG_FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)CONFIG_FLASH_BASE + CONFIG_FLASH_SIZE - 1)
uint8_t* dev_flash_to_host(paddr_t addr);
static inline bool in_dev_flash(paddr_t addr) {
  return addr - CONFIG_FLASH_BASE < CONFIG_FLASH_SIZE;
}
word_t dev_flash_read(paddr_t addr, int len);
void dev_flash_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_ISA_HAS_PSRAM
#define PSRAM_LEFT  ((paddr_t)CONFIG_PSRAM_BASE)
#define PSRAM_RIGHT ((paddr_t)CONFIG_PSRAM_BASE + CONFIG_PSRAM_SIZE - 1)
uint8_t* dev_psram_to_host(paddr_t addr);
static inline bool in_dev_psram(paddr_t addr) {
  return addr - CONFIG_PSRAM_BASE < CONFIG_PSRAM_SIZE;
}
word_t dev_psram_read(paddr_t addr, int len);
void dev_psram_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_ISA_HAS_SDRAM
#define SDRAM_LEFT  ((paddr_t)CONFIG_SDRAM_BASE)
#define SDRAM_RIGHT ((paddr_t)CONFIG_SDRAM_BASE + CONFIG_SDRAM_SIZE - 1)
uint8_t* dev_sdram_to_host(paddr_t addr);
static inline bool in_dev_sdram(paddr_t addr) {
  return addr - CONFIG_SDRAM_BASE < CONFIG_SDRAM_SIZE;
}
word_t dev_sdram_read(paddr_t addr, int len);
void dev_sdram_write(paddr_t addr, int len, word_t data);
#endif

#ifdef CONFIG_ISA_HAS_CHIPLINK
#define CHIPLINK_MMIO_LEFT  ((paddr_t)CONFIG_CHIPLINK_MMIO_BASE)
#define CHIPLINK_MMIO_RIGHT ((paddr_t)CONFIG_CHIPLINK_MMIO_BASE + CONFIG_CHIPLINK_MMIO_SIZE - 1)
static inline bool in_dev_chiplink_mmio(paddr_t addr) {
  return addr - CONFIG_CHIPLINK_MMIO_BASE < CONFIG_CHIPLINK_MMIO_SIZE;
}

#define CHIPLINK_MEM_LEFT  ((paddr_t)CONFIG_CHIPLINK_MEM_BASE)
#define CHIPLINK_MEM_RIGHT ((paddr_t)CONFIG_CHIPLINK_MEM_BASE + CONFIG_CHIPLINK_MEM_SIZE - 1)
static inline bool in_dev_chiplink_mem(paddr_t addr) {
  return addr - CONFIG_CHIPLINK_MEM_BASE < CONFIG_CHIPLINK_MEM_SIZE;
}
#endif

void init_isa_dev();

#endif
