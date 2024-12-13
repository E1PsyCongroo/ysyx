#ifndef __RISCV_VERILATING_H
#define __RISCV_VERILATING_H

#include <common.h>
#include <stdint.h>
#include <sys/types.h>

void rvcpu_init(const char *, int, char **);
void rvcpu_exit(void);
void rvcpu_single_cycle(void);
void rvcpu_single_exec(void);
void rvcpu_reset(void);
uint32_t rvcpu_ifetch(vaddr_t *pc, int len);

enum {
  RISCV_JMP,
  RISCV_BRANCH,
  RISCV_LOAD,
  RISCV_STORE,
  RISCV_AL,
  RISCV_SYSTEM,
  RISCV_CSR,
  RISCV_FENCE_I,
  RISCV_TOTAL_TYPE,
};

typedef struct {
  const char *inst_type;
  uint64_t total_inst_count;
  uint64_t total_icache_cost_cycle;
  uint64_t total_idu_cost_cycle;
  uint64_t total_exu_cost_cycle;
  uint64_t total_lsu_cost_cycle;
  uint64_t total_wbu_cost_cycle;
} InstStatistic;

extern uint32_t cur_inst;

extern uint64_t g_guest_cycle;
extern uint64_t g_nr_fetch_inst;

extern uint64_t g_nr_inst;
extern InstStatistic g_insts[RISCV_TOTAL_TYPE];

extern uint64_t g_nr_fetch_data;
extern uint64_t g_fetch_data_cycle;

extern uint64_t g_nr_cache_access;
extern uint64_t g_nr_cache_hit;
extern uint64_t g_cache_access_time;
extern uint64_t g_cache_miss_penalty;

#endif
