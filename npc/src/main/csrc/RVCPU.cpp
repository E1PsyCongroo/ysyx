// #include <nvboard.h>
#include <isa.h>
#include <paddr.h>
#include <VRVCPU.h>

static TOP_NAME dut;
bool isEnd = false;

// void nvboard_bind_all_pins(TOP_NAME* top);

extern "C" void sim_end() {
  isEnd = true;
}

extern "C" word_t pmem_read(paddr_t raddr) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  return paddr_read(raddr & ~0x3u, 4);
}
extern "C" void pmem_write(paddr_t waddr, word_t wdata, char wmask) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  word_t bit_mask = 0;
  for (int i = 0; i < sizeof(word_t); i++) {
      if (wmask & (1 << i)) {
          bit_mask |= (0xFF << (i * 8));
      }
  }
  paddr_write(waddr & ~0x3u, 4, wdata & bit_mask);
}

static void single_cycle() {
  dut.clock = 0; dut.eval();
  dut.clock = 1; dut.eval();
}

static void reset(int n) {
  dut.reset = 1;
  while (n -- > 0) single_cycle();
  dut.reset = 0;
}

int main() {
  // nvboard_bind_all_pins(&dut);
  // nvboard_init();

  reset(10);

  while(1) {
    // nvboard_update();
    single_cycle();
  }
}
