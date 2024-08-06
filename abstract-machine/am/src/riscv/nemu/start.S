.section entry, "ax"
.globl _start
.type _start, @function
.size _start, 12

_start:
  mv s0, zero
  la sp, _stack_pointer
  jal _trm_init
