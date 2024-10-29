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
  RISCV_TOTAL_TYPE,
};

typedef struct {
  const char *inst_type;
  uint64_t total_inst_count;
  uint64_t total_exec_cycle;
} InstStatistic;

extern uint64_t g_nr_guest_cycle;
extern uint64_t g_nr_fetch_inst;
extern InstStatistic g_insts[RISCV_TOTAL_TYPE];
extern uint64_t g_nr_exec_inst;
extern uint64_t g_nr_fetch_data_cycle;
extern uint64_t g_nr_fetch_data_count;

#endif
