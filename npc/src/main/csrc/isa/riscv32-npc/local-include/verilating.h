#ifndef __RISCV_VERILATING_H
#define __RISCV_VERILATING_H

#include <common.h>

void rvcpu_init(const char*, int, char**);
void rvcpu_exit(void);
void rvcpu_single_cycle(void);
void rvcpu_single_exec(void);
void rvcpu_reset(void);
uint32_t rvcpu_ifetch(vaddr_t *pc, int len);

#endif
