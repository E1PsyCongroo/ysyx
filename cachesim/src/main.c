#include <assert.h>
#include <cache.h>
#include <common.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

uint32_t cpu_read(Cache_t *cache, uintptr_t addr, int len);
void cpu_write(Cache_t *cache, uintptr_t addr, int len, uint32_t data);
uint32_t cpu_uncache_read(uintptr_t addr, int len);
void cpu_uncache_write(uintptr_t addr, int len, uint32_t data);

void init_mem(void);

static uint32_t seed;
static char *tracefile;

static void init_rand(uint32_t seed) {
  printf("random seed = %u\n", seed);
  srand(seed);
}

static inline uint32_t choose(uint32_t n) { return rand() % n; }

static void trace_exec(Cache_t *cache, uintptr_t addr, bool is_check) {
  uint32_t ret = cpu_read(cache, addr, 4);
  if (is_check) {
    uint32_t ret_uncache = cpu_uncache_read(addr, 4);
    assert(ret == ret_uncache);
  }
}

static void random_trace(Cache_t *cache) {
  for (int i = 0; i < 1000000; i++) {
    uint32_t addr = choose(MEM_SIZE);
    trace_exec(cache, addr, true);
  }
}

static void check_diff(Cache_t *cache) {
  uintptr_t addr = 0;
  for (addr = 0; addr < MEM_SIZE; addr += 4) {
    uint32_t ret = cpu_read(cache, addr, 4);
    uint32_t ret_uncache = cpu_uncache_read(addr, 4);
    assert(ret == ret_uncache);
  }
}

static void parse_args(int argc, char *argv[]) {
  int o;
  bool has_seed = false;
  char *p;
  while ((o = getopt(argc, argv, "-r:")) != -1) {
    switch (o) {
    case 'r':
      seed = strtol(optarg, &p, 0);
      if (!(*optarg != '\0' && *p == '\0')) {
        printf("invalid seed\n");
      } else {
        has_seed = true;
      }
      break;
    case 1:
      if (tracefile != NULL)
        printf("too much argument '%s', ignored\n", optarg);
      else
        tracefile = optarg;
      break;
    default:
      printf("Usage: %s [-r seed] [trace_file]\n", argv[0]);
      assert(0);
    }
  }

  if (!has_seed) {
    seed = time(0);
  }
}

void replay_trace(Cache_t *cache) {
  if (tracefile == NULL) {
    random_trace(cache);
    check_diff(cache);
    printf("Random test pass!\n");
    return;
  }

  char cmd[80];
  sprintf(cmd, "bzcat %s", tracefile);
  FILE *fp = popen(cmd, "r");
  assert(fp);

  uint32_t count;
  assert(fread(&count, sizeof(count), 1, fp) == 1);
  while (count--) {
    uint32_t addr;
    assert(fread(&addr, sizeof(addr), 1, fp) == 1);
    trace_exec(cache, addr, false);
  }

  pclose(fp);
}

int main(int argc, char *argv[]) {
  parse_args(argc, argv);

  init_rand(seed);
  init_mem();

  Cache_t *cache = init_cache(6, 0, 2);
  printf("replay_trace total_size_width: %d associativity_width: %d "
         "block_width: %d:\n",
         6, 0, 2);
  replay_trace(cache);
  display_statistic(cache);
  free(cache);

  cache = init_cache(6, 0, 4);
  printf("replay_trace total_size_width: %d associativity_width: %d "
         "block_width: %d:\n",
         6, 0, 4);
  replay_trace(cache);
  display_statistic(cache);
  free(cache);

  cache = init_cache(6, 0, 5);
  printf("replay_trace total_size_width: %d associativity_width: %d "
         "block_width: %d:\n",
         6, 0, 5);
  replay_trace(cache);
  display_statistic(cache);
  free(cache);

  cache = init_cache(6, 0, 6);
  printf("replay_trace total_size_width: %d associativity_width: %d "
         "block_width: %d:\n",
         6, 0, 6);
  replay_trace(cache);
  display_statistic(cache);
  free(cache);

  cache = init_cache(6, 2, 4);
  printf("replay_trace total_size_width: %d associativity_width: %d "
         "block_width: %d:\n",
         6, 2, 4);
  replay_trace(cache);
  display_statistic(cache);
  free(cache);

  return 0;
}
