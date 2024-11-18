#include <assert.h>
#include <cache.h>
#include <common.h>
#include <inttypes.h>
#include <math.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void mem_read(uintptr_t block_num, uint32_t block_width, uint8_t *buf);
void mem_write(uintptr_t block_num, uint32_t block_width, const uint8_t *buf);

static inline uint32_t get_word_index(Cache_t *cache, uint32_t addr) {
  return ((addr) >> WORD_WIDTH) &
         mask_with_len(cache->block_width - WORD_WIDTH);
}

static inline uint32_t get_set_index(Cache_t *cache, uint32_t addr) {
  return ((addr) >> (cache->block_width)) & mask_with_len(cache->set_width);
}

static inline uint32_t get_cache_tag(Cache_t *cache, uint32_t addr) {
  return (addr) >> (cache->set_width + cache->block_width);
}

static size_t check_in_cache(Cache_t *cache, uintptr_t addr) {
  total_time_inc(cache, ACCESS_TIME);
  int set_index = get_set_index(cache, addr);
  int cache_tag = get_cache_tag(cache, addr);
  int invalid_index = -1;
  assert(set_index < cache->setnum);
  for (size_t i = 0; i < cache->associativity; i++) {
    if (!cache->cset[set_index][i].valid) {
      invalid_index = i;
    } else if (cache->cset[set_index][i].tag == cache_tag) {
      access_increase(cache, true);
      return i;
    }
  }

  access_increase(cache, false);
  total_time_inc(cache, MISS_TIME * (cache->block_width / WORD_WIDTH));
  if (invalid_index == -1) {
    invalid_index = rand() % cache->associativity;
    if (cache->cset[set_index][invalid_index].dirty) {
      int victim_tag = cache->cset[set_index][invalid_index].tag;
      mem_write((victim_tag << cache->set_width) | set_index,
                cache->block_width, cache->cset[set_index][invalid_index].data);
    }
  }
  mem_read(addr >> cache->block_width, cache->block_width,
           cache->cset[set_index][invalid_index].data);
  cache->cset[set_index][invalid_index].tag = cache_tag;
  cache->cset[set_index][invalid_index].valid = true;
  cache->cset[set_index][invalid_index].dirty = false;
  return invalid_index;
}

uint32_t cache_read(Cache_t *cache, uintptr_t addr) {
  int set_index = get_set_index(cache, addr);
  assert(set_index < cache->setnum);
  int block_index = check_in_cache(cache, addr);
  assert(cache->cset[set_index][block_index].valid);
  uint32_t *cdata = (uint32_t *)cache->cset[set_index][block_index].data;
  return cdata[get_word_index(cache, addr)];
}

void cache_write(Cache_t *cache, uintptr_t addr, uint32_t data,
                 uint32_t wmask) {
  int set_index = get_set_index(cache, addr);
  assert(set_index < cache->setnum);
  int block_index = check_in_cache(cache, addr);
  assert(cache->cset[set_index][block_index].valid);
  cache->cset[set_index][block_index].dirty = true;
  uint32_t *cdata = (uint32_t *)cache->cset[set_index][block_index].data;
  uint32_t word_index = get_word_index(cache, addr);
  cdata[word_index] = (cdata[word_index] & ~wmask) | (data & wmask);
}

Cache_t *init_cache(int total_size_width, int associativity_width,
                    int block_width) {
  assert(total_size_width >= block_width);
  assert(associativity_width >= 0);
  Cache_t *cache = malloc(sizeof(Cache_t));
  memset(cache, 0, sizeof(Cache_t));
  cache->block_width = block_width;
  cache->blocksz = pow(2, block_width);
  cache->blocknum = pow(2, total_size_width - block_width);
  cache->associativity = pow(2, associativity_width);
  if (cache->blocknum < cache->associativity) {
    cache->associativity = cache->blocknum;
  }
  cache->setnum = cache->blocknum / cache->associativity;
  cache->cset = malloc(cache->setnum * sizeof(CacheSet_t));
  assert(cache->cset);
  for (size_t i = 0; i < cache->setnum; i++) {
    cache->cset[i] = malloc(cache->associativity * sizeof(CacheBlock_t));
    assert(cache->cset[i]);
    for (size_t j = 0; j < cache->associativity; j++) {
      cache->cset[i][j].valid = 0;
      cache->cset[i][j].data = malloc(cache->blocksz);
      assert(cache->cset[i][j].data);
    }
  }
  cache->set_width = log2(cache->setnum);
  cache->tag_width = ADDR_WIDTH - cache->block_width - cache->set_width;
  return cache;
}

void destructs_cache(Cache_t *cache) {
  for (int i = 0; i < cache->setnum; i++) {
    for (int j = 0; j < cache->associativity; j++) {
      free(cache->cset[i][j].data);
    }
    free(cache->cset[i]);
  }
  free(cache->cset);
  free(cache);
}

void display_statistic(Cache_t *cache) {
  uint64_t hit_count = cache->access_cnt - cache->miss_cnt;
  printf("Access count: %lu\n", cache->access_cnt);
  printf("Hit count: %lu\n", hit_count);
  double hit_rate = (double)hit_count / cache->access_cnt;
  printf("Hit Rate: %f%%\n", hit_rate * 100);
  printf("AMAT: %f\n", cache->total_time / cache->access_cnt);
}
