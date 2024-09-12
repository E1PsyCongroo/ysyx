#include <common.h>
#include <isa.h>

#define MROM_LEFT  ((int32_t)CONFIG_MROM_ADDR)
#define MROM_RIGHT ((int32_t)CONFIG_MROM_ADDR + CONFIG_MROM_SIZE - 1)
#define RESET_VECTOR (MROM_LEFT + CONFIG_PC_RESET_OFFSET)

#if   defined(CONFIG_MROM_MALLOC)
static uint8_t *mrom = NULL;
#else // CONFIG_mrom_GARRAY
static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {};
#endif

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of mrom [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, MROM_LEFT, MROM_RIGHT, cpu.pc);
}

static bool in_mrom(int32_t addr) {
  return (uint32_t)addr - CONFIG_MROM_ADDR < CONFIG_MROM_SIZE;
}

void init_mrom(char *img_file) {
#if   defined(CONFIG_MROM_MALLOC)
  mrom = malloc(CONFIG_MROM_SIZE);
  assert(mrom);
#endif
  if (img_file == NULL) {
    Log("No image is given. Use the default build-in image.");
  }

  FILE *fp = fopen(img_file, "rb");
  Assert(fp, "Can not open '%s'", img_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The image is %s, size = %ld", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(mrom + RESET_VECTOR - CONFIG_MROM_ADDR, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  Log("Add mrom at [" FMT_PADDR ", " FMT_PADDR "]", MROM_LEFT, MROM_RIGHT);
}

void mrom_read(int32_t addr, int32_t *data) {
  if (likely(in_mrom(addr))) {
    *data = *(int32_t*)(mrom + addr - CONFIG_MROM_ADDR);
  #ifdef CONFIG_MTRACE
    if (MTRACE_COND) { log_write(ANSI_FMT("%-16.16s: @" FMT_PADDR ", len = %2d, data = " FMT_WORD "\n", ANSI_FG_CYAN), "Read Mrom", addr, 4, *data); }
  #endif
  }
  else {
    out_of_bound(addr);
  }
}
