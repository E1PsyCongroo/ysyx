#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
IFDEF(CONFIG_HAS_MROM, static uint8_t *mrom = NULL);
IFDEF(CONFIG_HAS_FLASH, static uint8_t *flash = NULL);
#else // CONFIG_PMEM_GARRAY
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
IFDEF(CONFIG_HAS_MROM, static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {});
IFDEF(CONFIG_HAS_FLASH, static uint8_t flash[CONFIG_FLASH_SIZE] PG_ALIGN = {});
#endif

#ifdef CONFIG_HAS_MROM
uint8_t* dev_mrom_to_host(paddr_t addr) { return mrom + addr - CONFIG_MROM_BASE; }

word_t dev_mrom_read(paddr_t addr, int len) {
  word_t data = host_read(dev_mrom_to_host(addr), len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "Read Mrom", addr, 4, data); }
#endif
  return data;
}

void dev_mrom_write(paddr_t addr, int len, word_t data) {
  panic("mrom can't be writen");
}
#endif

#ifdef CONFIG_HAS_FLASH
uint8_t* dev_flash_to_host(paddr_t addr) { return flash + addr - CONFIG_FLASH_BASE; }

word_t dev_flash_read(paddr_t addr, int len) {
  word_t data = host_read(dev_flash_to_host(addr), len);
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "Read Flash", addr, 4, data); }
#endif
  return data;
}

void dev_flash_write(paddr_t addr, int len, word_t data) {
  panic("flash can't be writen");
  host_write(dev_flash_to_host(addr), len, data);
}
#endif

uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
  #ifdef CONFIG_HAS_MROM
  panic("address = " FMT_PADDR " is out of bound of mrom [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, MROM_LEFT, MROM_RIGHT, cpu.pc);
  #endif
  #ifdef CONFIG_HAS_FLASH
  panic("address = " FMT_PADDR " is out of bound of flash [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, FLASH_LEFT, FLASH_RIGHT, cpu.pc);
  #endif
}

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE); assert(pmem);
  IFDEF(CONFIG_HAS_MROM, mrom = malloc(CONFIG_MROM_SIZE); assert(mrom));
  IFDEF(CONFIG_HAS_FLASH, flash = malloc(CONFIG_FLASH_SIZE); assert(flash));
#endif
#if   defined(CONFIG_MEM_RANDOM)
  int randnum = rand();
  memset(pmem, randnum, CONFIG_MSIZE);
  IFDEF(CONFIG_HAS_MROM, memset(mrom, randnum, CONFIG_MROM_SIZE));
  IFDEF(CONFIG_HAS_FLASH, memset(flash, randnum, CONFIG_FLASH_SIZE));
#endif
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
  Log("mrom area [" FMT_PADDR ", " FMT_PADDR "]", MROM_LEFT, MROM_RIGHT);
  Log("flash area [" FMT_PADDR ", " FMT_PADDR "]", FLASH_LEFT, FLASH_RIGHT);
}

word_t paddr_read(paddr_t addr, int len) {
  if (likely(in_pmem(addr))) return pmem_read(addr, len);
  IFDEF(CONFIG_HAS_MROM, if (likely(in_dev_mrom(addr))) return dev_mrom_read(addr, len);)
  IFDEF(CONFIG_HAS_FLASH, if (likely(in_dev_flash(addr))) return dev_flash_read(addr, len);)
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  if (likely(in_pmem(addr))) { pmem_write(addr, len, data); return; }
  IFDEF(CONFIG_HAS_MROM, if (likely(in_dev_mrom(addr))) { dev_mrom_write(addr, len, data); return; })
  IFDEF(CONFIG_HAS_FLASH, if (likely(in_dev_flash(addr))) { dev_flash_write(addr, len, data); return; })
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
