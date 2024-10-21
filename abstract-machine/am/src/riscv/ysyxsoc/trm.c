#include <am.h>
#include <klib.h>
#include <ysyxsoc.h>

extern char _heap_start[], _stack_pointer[];

int main(const char *args);

Area heap = RANGE(_heap_start, _stack_pointer);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

#define init_uart1650()                                                        \
  do {                                                                         \
    *(volatile uint8_t *)SERIAL_LING_CONTROL = 0b10000011;                     \
    *(volatile uint8_t *)SERIAL_DIV_MSB = 0b00000000;                          \
    *(volatile uint8_t *)SERIAL_DIV_LSB = 0b00000001;                          \
    *(volatile uint8_t *)SERIAL_LING_CONTROL = 0b00000011;                     \
  } while (0)

void putch(char ch) {
  while (!(inb(SERIAL_LING_STATUS) & 0x20))
    ;
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  ysyxsoc_trap(code);
  while (1)
    ;
}

static void init_hello() {
  uint32_t mvendorid = 0, marchid = 0;
  asm volatile("csrr %0, mvendorid\n\t"
               "csrr %1, marchid"
               : "+r"(mvendorid), "+r"(marchid)
               :);
  char *ch = (void *)&mvendorid;
  putch(ch[3]);
  putch(ch[2]);
  putch(ch[1]);
  putch(ch[0]);
  printf("-%08d\n", marchid);
}

void _trm_init() {
  init_hello();
  int ret = main(mainargs);
  halt(ret);
}

#define macro_putch(ch)                                                        \
  do {                                                                         \
    while (!(*(volatile uint8_t *)SERIAL_LING_STATUS & 0x20))                  \
      ;                                                                        \
    *(volatile uint8_t *)SERIAL_PORT = ch;                                     \
  } while (0)

#define macro_putstr(str)                                                      \
  do {                                                                         \
    for (const char *ch = str; *ch; ch++) {                                    \
      macro_putch(*ch);                                                        \
    }                                                                          \
  } while (0)

#define macro_memcpy(out, in, n)                                               \
  do {                                                                         \
    unsigned char *out_p = (void *)(out);                                      \
    const unsigned char *in_p = (void *)(in);                                  \
    size_t size = (n);                                                         \
    while (size--) {                                                           \
      *(out_p++) = *(in_p++);                                                  \
    }                                                                          \
  } while (0)

#define macro_memset(s, c, n)                                                  \
  do {                                                                         \
    unsigned char *p = (void *)(s);                                            \
    size_t size = (n);                                                         \
    while (size--) {                                                           \
      *(p++) = (unsigned char)(c);                                             \
    }                                                                          \
  } while (0)

__attribute__((section(".bootloader.ssbl"))) void _bootloader_ssbl(void) {
  extern char _text_vma_start[], _text_lma_start[], _text_lma_end[];
  extern char _rodata_vma_start[], _rodata_lma_start[], _rodata_lma_end[];
  extern char _data_extra_vma_start[], _data_extra_lma_start[],
      _data_extra_lma_end[];
  extern char _data_vma_start[], _data_lma_start[], _data_lma_end[];
  extern char _bss_start[], _end[];
  macro_memcpy(_text_vma_start, _text_lma_start,
               _text_lma_end - _text_lma_start);
  macro_memcpy(_rodata_vma_start, _rodata_lma_start,
               _rodata_lma_end - _rodata_lma_start);
  macro_memcpy(_data_extra_vma_start, _data_extra_lma_start,
               _data_extra_lma_end - _data_extra_lma_start);
  macro_memcpy(_data_vma_start, _data_lma_start,
               _data_lma_end - _data_lma_start);
  macro_memset(_bss_start, 0, _end - _bss_start);
  const char boot_end_str[] = "bootend\n";
  macro_putstr(boot_end_str);
  _trm_init();
}

__attribute__((section(".bootloader.fsbl"))) void _bootloader_fsbl(void) {
  init_uart1650();
  const char boot_start_str[] = "boot\n";
  macro_putstr(boot_start_str);
  extern char _bootloader_vma_start[], _bootloader_lma_start[],
      _bootloader_lma_end[];
  macro_memcpy(_bootloader_vma_start, _bootloader_lma_start,
               _bootloader_lma_end - _bootloader_lma_start);
  _bootloader_ssbl();
}
