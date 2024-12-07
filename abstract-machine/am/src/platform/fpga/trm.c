#include "platform/fpga/include/fpga.h"
#include <am.h>
#include <fpga.h>

int main(const char *args);

extern char _heap_start[], _stack_pointer[];
Area heap = RANGE(_heap_start, _stack_pointer);
#ifndef MAINARGS
#define MAINARGS ""
#endif
static const char mainargs[] = MAINARGS;

#define init_uart() outl(UART_CTRL, 0x0);
#define uart_rx_flush() outl(UART_CTRL, 0x2)
#define uart_tx_flush() outl(UART_CTRL, 0x1)

void putch(char ch) {
  while (inb(UART_STATE) & 0x08)
    ;
  outb(UART_TX_FIFO, ch);
}

void halt(int code) {
  fpga_trap(code);

  // should not reach here
  while (1);
}

void _trm_init() {
  init_uart();
  int ret = main(mainargs);
  halt(ret);
}
