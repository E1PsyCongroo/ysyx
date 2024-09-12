#include <am.h>
#include <klib.h>
#include <ysyxsoc.h>

extern char _heap_start;
int main(const char *args);

Area heap = RANGE(&_heap_start, SRAM_END);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  ysyxsoc_trap(code);
  while (1);
}

void _trm_init() {
  extern char _data_vma_start [], _data_lma_start [], _data_lma_end [];
  memcpy(_data_vma_start, _data_lma_start, _data_lma_end - _data_lma_start);
  extern char _bss_start [], _end [];
  memset(_bss_start, 0, _end - _bss_start);
  int ret = main(mainargs);
  halt(ret);
}
