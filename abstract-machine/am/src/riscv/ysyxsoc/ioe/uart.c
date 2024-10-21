#include <am.h>
#include <ysyxsoc.h>

void __am_uart_rx(AM_UART_RX_T *uart) {
  if (inb(SERIAL_LING_STATUS) & 0x01) {
    uart->data = inb(SERIAL_PORT);
  } else {
    uart->data = 0xff;
  }
}
