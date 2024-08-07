#ifndef __RISCV_VERILATING_H
#define __RISCV_VERILATING_H

#include <common.h>

void rvcpu_init(void);
void rvcpu_exit(void);
void rvcpu_single_cycle();
void rvcpu_reset(int n);
void rvcpu_to_cpu(void);

#endif
