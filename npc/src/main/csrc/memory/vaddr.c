#include <isa.h>
#include <memory/paddr.h>

word_t vaddr_ifetch(vaddr_t addr, int len) {
#ifdef config_mtrace
  if (MTRACE_COND) { log_write(ANSI_FMT("Read instruction: @" FMT_PADDR ", len=%2d, data=" FMT_WORD "\n", ANSI_FG_CYAN), addr, len, data); }
#endif
  return paddr_read(addr, len);
}

word_t vaddr_read(vaddr_t addr, int len) {
#ifdef CONFIG_MTRACE
  if (MTRACE_COND) { log_write(ANSI_FMT("Read pmem: @" FMT_PADDR ", len=%2d\n", ANSI_FG_CYAN), addr, len ); }
#endif
  return paddr_read(addr, len);
}

void vaddr_write(vaddr_t addr, int len, word_t data) {
#ifdef config_mtrace
  if (MTRACE_COND) { log_write(ANSI_FMT("write pmem: @" FMT_PADDR ", len=%2d, data=" FMT_WORD "\n", ANSI_FG_CYAN), addr, len, data); }
#endif
  paddr_write(addr, len, data);
}
