#include <am.h>
#include <fpga.h>

void __am_uart_rx(AM_UART_RX_T *uart) {
  if (inb(UART_CTRL) & 0x01) {
    uart->data = inb(UART_RX_FIFO);
  } else {
    uart->data = 0xff;
  }
}
