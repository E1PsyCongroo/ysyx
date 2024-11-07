#include <assert.h>
#include <common.h>
#include <inttypes.h>
#include <math.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void mem_read(uintptr_t block_num, uint8_t *buf);
void mem_write(uintptr_t block_num, const uint8_t *buf);

static uint64_t cycle_cnt = 0;

void cycle_increase(int n) { cycle_cnt += n; }

typedef struct {
  uint32_t dirty : 1;
  uint32_t valid : 1;
  uint32_t tag : 30;
  uint8_t data[BLOCK_SIZE];
} CacheBlock_t;
typedef CacheBlock_t *CacheSet_t;

typedef struct {
  uint32_t setnum;
  uint32_t associativity;
  CacheSet_t *cset;
} Cache_t;

static Cache_t cache;
static int set_index_width, cache_tag_width;

#define get_word_index(addr)                                                   \
  (((addr) >> byte_offset_width) & mask_with_len(block_offset_with))
#define get_set_index(addr)                                                    \
  (((addr) >> (block_offset_with + byte_offset_width)) &                       \
   mask_with_len(set_index_width))
#define get_cache_tag(addr)                                                    \
  ((addr) >> (set_index_width + block_offset_with + byte_offset_width))

static size_t check_in_cache(uintptr_t addr) {
  int set_index = get_set_index(addr);
  int cache_tag = get_cache_tag(addr);
  int invalid_index = -1;
  assert(set_index < cache.setnum);
  for (size_t i = 0; i < cache.associativity; i++) {
    if (!cache.cset[set_index][i].valid) {
      invalid_index = i;
    } else if (cache.cset[set_index][i].tag == cache_tag) {
      return i;
    }
  }
  if (invalid_index == -1) {
    invalid_index = rand() % cache.associativity;
    if (cache.cset[set_index][invalid_index].dirty) {
      int victim_tag = cache.cset[set_index][invalid_index].tag;
      mem_write((victim_tag << set_index_width) | set_index,
                cache.cset[set_index][invalid_index].data);
    }
  }
  mem_read(addr >> BLOCK_WIDTH, cache.cset[set_index][invalid_index].data);
  cache.cset[set_index][invalid_index].tag = cache_tag;
  cache.cset[set_index][invalid_index].valid = true;
  cache.cset[set_index][invalid_index].dirty = false;
  return invalid_index;
}

uint32_t cache_read(uintptr_t addr) {
  int set_index = get_set_index(addr);
  assert(set_index < cache.setnum);
  int block_index = check_in_cache(addr);
  assert(cache.cset[set_index][block_index].valid);
  uint32_t *cdata = (uint32_t *)cache.cset[set_index][block_index].data;
  return cdata[get_word_index(addr)];
}

void cache_write(uintptr_t addr, uint32_t data, uint32_t wmask) {
  int set_index = get_set_index(addr);
  assert(set_index < cache.setnum);
  int block_index = check_in_cache(addr);
  assert(cache.cset[set_index][block_index].valid);
  cache.cset[set_index][block_index].dirty = true;
  uint32_t *cdata = (uint32_t *)cache.cset[set_index][block_index].data;
  cdata[get_word_index(addr)] =
      (cdata[get_word_index(addr)] & ~wmask) | (data & wmask);
}

void init_cache(int total_size_width, int associativity_width) {
  assert(total_size_width > BLOCK_WIDTH);
  assert(associativity_width >= 0);
  cache.associativity = pow(2, associativity_width);
  int block_num = pow(2, total_size_width - BLOCK_WIDTH);
  if (block_num < cache.associativity) {
    cache.associativity = block_num;
  }
  cache.setnum = block_num / cache.associativity;
  cache.cset = malloc(cache.setnum * sizeof(CacheSet_t));
  for (size_t i = 0; i < cache.setnum; i++) {
    cache.cset[i] = malloc(cache.associativity * sizeof(CacheBlock_t));
    memset(cache.cset[i], 0, cache.associativity * sizeof(CacheBlock_t));
  }
  set_index_width = log2(cache.setnum);
  cache_tag_width =
      ADDRWIDTH - byte_offset_width - block_offset_with - set_index_width;
}

void display_statistic(void) { printf("Use Cycle: %lu\n", cycle_cnt); }
