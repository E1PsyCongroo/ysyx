ENTRY(_start)
PHDRS { bootloader PT_LOAD; entry PT_LOAD; text PT_LOAD; rodata PT_LOAD; data PT_LOAD; }

MEMORY {
  mrom(rx)    : ORIGIN = 0x20000000, LENGTH = 4K
  flash(rx)   : ORIGIN = 0x30000000, LENGTH = 16M
  sram(rwx)   : ORIGIN = 0x0f000000, LENGTH = 8K
  psram(rwx)  : ORIGIN = 0x80000000, LENGTH = 4M
  sdram(rwx)  : ORIGIN = 0xA0000000, LENGTH = 128M
}

SECTIONS {
  . = ORIGIN(flash);
  .entry : ALIGN(0x4) {
    *(entry)
    *(.bootloader.fsbl)
  } > flash AT> flash : entry

  .bootloader : ALIGN(0x4) {
    _bootloader_vma_start = .;
    *(.bootloader.ssbl)
    _bootloader_vma_end = .;
  } > sram AT> flash : bootloader
  _bootloader_lma_start = LOADADDR(.bootloader);
  _bootloader_lma_end = LOADADDR(.bootloader) + SIZEOF(.bootloader);

  .text : ALIGN(0x4) {
    _text_vma_start = .;
    *(.text*)
    etext = .;
    _etext = .;
    _text_vma_end = .;
  } > sdram AT> flash : text
  _text_lma_start = LOADADDR(.text);
  _text_lma_end = LOADADDR(.text) + SIZEOF(.text);

  .rodata : ALIGN(0x4) {
    _rodata_vma_start = .;
    *(.rodata*)
    *(.srodata*)
    _rodata_vma_end = .;
  } > sdram AT> flash : rodata
  _rodata_lma_start = LOADADDR(.rodata);
  _rodata_lma_end = LOADADDR(.rodata) + SIZEOF(.rodata);

  .data : ALIGN(0x4) {
    _data_vma_start = .;
    *(.data*)
    *(.sdata*)
    _data_vma_end = .;
    edata = .;
  } > sdram AT> flash : data
  _data_lma_start = LOADADDR(.data);
  _data_lma_end = LOADADDR(.data) + SIZEOF(.data);
  PROVIDE(_data_extra_vma_start = _data_vma_start);
  PROVIDE(_data_extra_vma_end = _data_vma_end);
  PROVIDE(_data_extra_lma_start = _data_lma_start);
  PROVIDE(_data_extra_lma_end = _data_lma_end);

  .bss : ALIGN(0x4) {
	  _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    end = .;
    _end = .;
  } > sdram

  _heap_start = ALIGN(0x1000);
  _stack_pointer = ORIGIN(sdram) + LENGTH(sdram);
}
