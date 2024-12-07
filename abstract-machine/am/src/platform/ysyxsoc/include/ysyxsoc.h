#ifndef YSYXSOC_H__
#define YSYXSOC_H__

#include <klib-macros.h>

#include ISA_H // the macro `ISA_H` is defined in CFLAGS
               // it will be expanded as "x86/x86.h", "mips/mips32.h", ...

#define ysyxsoc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

#define CLINT_BASE 0x02000000L
#define CLINT_MTIME CLINT_BASE
#define CLINT_SIZE 0x10000L
#define CLINT_END (CLINT_BASE + CLINT_SIZE)

#define SERIAL_BASE 0x10000000L
#define SERIAL_PORT         (SERIAL_BASE + 0x0)
#define SERIAL_LING_CONTROL (SERIAL_BASE + 0x3)
#define SERIAL_LING_STATUS  (SERIAL_BASE + 0x5)
#define SERIAL_DIV_LSB      (SERIAL_BASE + 0x0)
#define SERIAL_DIV_MSB      (SERIAL_BASE + 0x1)

#define MROM_BASE 0x20000000L
#define MROM_SIZE (4 * 1024)
#define MROM_END  (MROM_BASE + MROM_SIZE)

#define SRAM_BASE 0x0f000000L
#define SRAM_SIZE (8 * 1024)
#define SRAM_END  (SRAM_BASE + SRAM_SIZE)

#define PS2_BASE 0x10011000L
#define PS2_KEY (PS2_BASE + 0x0)
#define PS2_SIZE 8
#define PS2_END (PS2_BASE + PS2_SIZE)

#define VGACTL_BASE 0x21000000L
#define VGAFB_ADDR VGACTL_BASE
#define VGACTL_SIZE (640 * 480 * 3)
#define VGACTL_END (VGACTL_BASE + VGACTL_SIZE)

typedef uintptr_t PTE;

#define PGSIZE    4096

#endif
