#ifndef __CACHE_H__
#define __CACHE_H__

#include <debug.h>
#include <stdbool.h>
#include <stdint.h>
#include <utils.h>

typedef struct {
  uint32_t dirty : 1;
  uint32_t valid : 1;
  uint32_t tag : 30;
  uint8_t *data;
} CacheBlock_t;
typedef CacheBlock_t *CacheSet_t;

typedef struct {
  uint32_t setnum;
  uint32_t associativity;
  uint32_t blocksz;
  uint32_t blocknum;
  CacheSet_t *cset;

  uint32_t tag_width;
  uint32_t set_width;
  uint32_t block_width;

  uint64_t access_cnt;
  uint64_t miss_cnt;
} Cache_t;

uint32_t cache_read(Cache_t *cache, uintptr_t addr);
void cache_write(Cache_t *cache, uintptr_t addr, uint32_t data, uint32_t wmask);
Cache_t *init_cache(int total_size_width, int associativity_width,
                    int block_width);
void display_statistic(Cache_t *cache);
#endif
