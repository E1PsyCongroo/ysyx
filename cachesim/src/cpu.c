#include <cache.h>
#include <common.h>
#include <stdint.h>

#define addr_offset_bit(addr) (((addr) & 0x3) * 8)

uint32_t mem_uncache_read(uintptr_t addr);
void mem_uncache_write(uintptr_t addr, uint32_t data, uint32_t wmask);

static const uint32_t len2datamask[] = {0x0, 0xff, 0xffff, 0xffffff,
                                        0xffffffff};

uint32_t cpu_read(Cache_t *cache, uintptr_t addr, int len) {
  return (cache_read(cache, addr) >> addr_offset_bit(addr)) & len2datamask[len];
}

void cpu_write(Cache_t *cache, uintptr_t addr, int len, uint32_t data) {
  cache_write(cache, addr, data << addr_offset_bit(addr),
              len2datamask[len] << addr_offset_bit(addr));
}

uint32_t cpu_uncache_read(uintptr_t addr, int len) {
  return (mem_uncache_read(addr) >> addr_offset_bit(addr)) & len2datamask[len];
}

void cpu_uncache_write(uintptr_t addr, int len, uint32_t data) {
  mem_uncache_write(addr, data << addr_offset_bit(addr),
                    len2datamask[len] << addr_offset_bit(addr));
}
