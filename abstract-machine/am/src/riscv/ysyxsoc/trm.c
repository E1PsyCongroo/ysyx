#include <am.h>
#include <klib.h>
#include <ysyxsoc.h>

extern char _heap_start [], _stack_pointer [];

int main(const char *args);

Area heap = RANGE(_heap_start, _stack_pointer);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

#define SERIAL_PORT         (SERIAL_BASE + 0x0)
#define SERIAL_LING_CONTROL (SERIAL_BASE + 0x3)
#define SERIAL_LING_STATUS  (SERIAL_BASE + 0x5)
#define SERIAL_DIV_LSB      (SERIAL_BASE + 0x0)
#define SERIAL_DIV_MSB      (SERIAL_BASE + 0x1)

static void init_uart1650(void) {
  outb(SERIAL_LING_CONTROL, 0b10000011);
  outb(SERIAL_DIV_MSB, 0b00000000);
  outb(SERIAL_DIV_LSB, 0b00000001);
  outb(SERIAL_LING_CONTROL, 0b00000011);
}

void putch(char ch) {
  while (!(inb(SERIAL_LING_STATUS) & 0x20));
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  ysyxsoc_trap(code);
  while (1);
}

static void init_mem(void) {
  extern char _data_vma_start [], _data_lma_start [], _data_lma_end [];
  extern char _bss_start [], _end [];
  memcpy(_data_vma_start, _data_lma_start, _data_lma_end - _data_lma_start);
  memset(_bss_start, 0, _end - _bss_start);
}

static void init_hello() {
  uint32_t mvendorid = 0, marchid = 0;
  asm volatile(
    "csrr %0, mvendorid\n\t"
    "csrr %1, marchid"
    : "+r"(mvendorid), "+r"(marchid)
    :
  );
  char* ch = (void*)&mvendorid;
  putch(ch[3]); putch(ch[2]); putch(ch[1]); putch(ch[0]);
  printf("-%08d\n", marchid);
}

void _trm_init() {
  init_uart1650();
  init_mem();
  init_hello();
  int ret = main(mainargs);
  halt(ret);
}
