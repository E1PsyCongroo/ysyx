#ifndef FPGA_H__
#define FPGA_H__

#include <klib-macros.h>

#include ISA_H // the macro `ISA_H` is defined in CFLAGS
               // it will be expanded as "x86/x86.h", "mips/mips32.h", ...

#define fpga_trap(code) asm volatile("mv a0, %0; ebreak" : : "r"(code))

#define CLINT_BASE 0x02000000L
#define CLINT_MTIME CLINT_BASE
#define CLINT_SIZE 0x10000L
#define CLINT_END (CLINT_BASE + CLINT_SIZE)

#define UART_BASE 0x10000000L
#define UART_RX_FIFO (UART_BASE + 0x0)
#define UART_TX_FIFO (UART_BASE + 0x4)
#define UART_STATE (UART_BASE + 0x8)
#define UART_CTRL (UART_BASE + 0xC)

#define BRAM_BASE 0x0f000000L
#define BRAM_SIZE (128 * 1024)
#define BRAM_END (BRAM_BASE + SRAM_SIZE)

typedef uintptr_t PTE;

#define PGSIZE 4096

#endif
