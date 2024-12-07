ENTRY(_start)
PHDRS { entry PT_LOAD; text PT_LOAD; rodata PT_LOAD; data PT_LOAD; bss PT_LOAD; }

MEMORY {
  bram(rwx)   : ORIGIN = 0x0f000000, LENGTH = 512K
}

SECTIONS {
  . = ORIGIN(bram);
  .entry : ALIGN(0x4) {
    *(entry)
    *(.bootloader.fsbl*)
  } > bram AT> bram : entry

  .text : ALIGN(0x4) {
    _text_vma_start = .;
    *(.text*)
    etext = .;
    _etext = .;
    _text_vma_end = .;
  } > bram AT> bram : text
  _text_lma_start = LOADADDR(.text);
  _text_lma_end = LOADADDR(.text) + SIZEOF(.text);

  .rodata : ALIGN(0x4) {
    _rodata_vma_start = .;
    *(.rodata*)
    *(.srodata*)
    _rodata_vma_end = .;
  } > bram AT> bram : rodata
  _rodata_lma_start = LOADADDR(.rodata);
  _rodata_lma_end = LOADADDR(.rodata) + SIZEOF(.rodata);

  .data : ALIGN(0x4) {
    _data_vma_start = .;
    *(.data*)
    *(.sdata*)
    _data_vma_end = .;
    edata = .;
  } > bram AT> bram : data
  _data_lma_start = LOADADDR(.data);
  _data_lma_end = LOADADDR(.data) + SIZEOF(.data);

  .bss : ALIGN(0x4) {
	  _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    end = .;
    _end = .;
  } > bram : bss

  _heap_start = ALIGN(0x1000);
  _stack_pointer = ORIGIN(bram) + LENGTH(bram);
}
