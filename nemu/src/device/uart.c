#include <utils.h>
#include <device/map.h>

/* http://en.wikibooks.org/wiki/uart_Programming/8250_UART_Programming */
// NOTE: this is compatible to 16550

#define CH_OFFSET 0
#define INT_OFFSET 1
#define INTID_OFFSET 2
#define FIFOCTL_OFFSET 2
#define LINECTL_OFFSET 3
#define MODECTL_OFFSET 4
#define LINESTATUS_OFFSET 5
#define MODESTATUS_OFFSET 6

static uint8_t *uart_base = NULL;

static void uart_putc(char ch) {
  MUXDEF(CONFIG_TARGET_AM, putch(ch), putc(ch, stderr));
}

static void uart_io_handler(uint32_t offset, int len, bool is_write) {
  assert(len == 1);
  switch (offset) {
    /* We bind the uart port with the host stderr in NEMU. */
    case CH_OFFSET:
      if (is_write) uart_putc(uart_base[0]);
      else panic("do not support read");
      break;
    // default: panic("do not support offset = %d", offset);
  }
}

void init_uart() {
  uart_base = new_space(7*8);
#ifdef CONFIG_HAS_PORT_IO
  add_pio_map ("uart", CONFIG_UART_PORT, uart_base, 8, urat_io_handler);
#else
  add_mmio_map("uart", CONFIG_UART_MMIO, uart_base, 8, uart_io_handler);
#endif
  uart_base[LINESTATUS_OFFSET] = 0x20;
}
