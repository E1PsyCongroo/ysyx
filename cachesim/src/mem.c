#include <assert.h>
#include <common.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static uint8_t *mem;
static uint8_t *mem_diff;

void init_mem(void) {
  mem = malloc(MEM_SIZE);
  mem_diff = malloc(MEM_SIZE);
  assert(mem);
  assert(mem_diff);
  for (int i = 0; i < MEM_SIZE; i++) {
    mem[i] = rand() & 0xff;
  }

  memcpy(mem_diff, mem, MEM_SIZE);
}

void mem_read(uintptr_t block_num, uint32_t block_width, uint8_t *buf) {
  uintptr_t addr = (block_num << block_width) % MEM_SIZE;
  memcpy(buf, mem + addr, macro_exp2(block_width));
}

void mem_write(uintptr_t block_num, uint32_t block_width, const uint8_t *buf) {
  uintptr_t addr = (block_num << block_width) % MEM_SIZE;
  memcpy(mem + addr, buf, macro_exp2(block_width));
}

uint32_t mem_uncache_read(uintptr_t addr) {
  addr %= MEM_SIZE;
  uint32_t *p = (void *)mem_diff + (addr & ~0x3);
  return *p;
}

void mem_uncache_write(uintptr_t addr, uint32_t data, uint32_t wmask) {
  addr %= MEM_SIZE;
  uint32_t *p = (void *)mem_diff + (addr & ~0x3);
  *p = (*p & ~wmask) | (data & wmask);
}
