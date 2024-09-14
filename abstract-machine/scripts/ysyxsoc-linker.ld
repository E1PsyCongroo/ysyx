ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom(rx) : ORIGIN = 0x20000000, LENGTH = 4K
  flash(rx) : ORIGIN = 0x30000000, LENGTH = 16M
  sram(!x) : ORIGIN = 0x0f000000, LENGTH = 8K
}
SECTIONS {
  . = ORIGIN(flash);
  .text : {
    *(entry)
    *(.text*)
    etext = .;
    _etext = .;
  } > flash AT> flash : text

  .rodata : {
    *(.rodata*)
  } > flash AT> flash
  .srodata : {
    *(.srodata*)
  } > flash AT> flash

  .data : {
    _data_vma_start = .;
    *(.data*)
    *(.sdata*)
    _data_vma_end = .;
    edata = .;
  } > sram AT> flash : data
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
