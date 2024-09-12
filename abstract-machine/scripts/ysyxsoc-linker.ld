ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom(rx) : ORIGIN = 0x20000000, LENGTH = 4K
  sram(!x) : ORIGIN = 0x0f000000, LENGTH = 8K
}
SECTIONS {
  . = ORIGIN(mrom);
  .text : {
    *(entry)
    *(.text*)
    etext = .;
    _etext = .;
  } > mrom AT> mrom : text

  .rodata : {
    *(.rodata*)
  } > mrom AT> mrom
  .srodata : {
    *(.srodata*)
  } > mrom AT> mrom

  .data : {
    _data_vma_start = .;
    *(.data*)
    _data_vma_end = .;
    edata = .;
  } > sram AT> mrom : data
  _data_lma_start = LOADADDR(.data);
  _data_lma_end = LOADADDR(.data) + SIZEOF(.data);

  .bss : {
	  _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    end = .;
    _end = .;
  } > sram

  _heap_start = .;
  _stack_pointer = ORIGIN(sram) + LENGTH(sram);
}
