#ifndef NPC_H__
#define NPC_H__

#include <klib-macros.h>

#include ISA_H // the macro `ISA_H` is defined in CFLAGS
               // it will be expanded as "x86/x86.h", "mips/mips32.h", ...

#define ysyxsoc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

#define SERIAL_BASE 0x10000000L
#define SERIAL_PORT (SERIAL_BASE + 0x0)

#define MROM_BASE 0x20000000L
#define MROM_SIZE (4 * 1024)
#define MROM_END  (MROM_BASE + MROM_SIZE)

#define SRAM_BASE 0x0f000000L
#define SRAM_SIZE (8 * 1024)
#define SRAM_END  (SRAM_BASE + SRAM_SIZE)

#define PGSIZE    4096

#endif
