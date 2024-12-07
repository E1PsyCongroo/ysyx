AM_SRCS := platform/fpga/trm.c \
           platform/fpga/ioe/ioe.c \
           platform/fpga/ioe/timer.c \
           platform/fpga/ioe/input.c \
           platform/fpga/ioe/gpu.c \
           platform/fpga/ioe/audio.c \
           platform/fpga/ioe/disk.c \
           platform/fpga/ioe/uart.c \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/fpga-linker.ld
LDFLAGS   += --gc-sections -e _start

CFLAGS += -DMAINARGS=\"$(mainargs)\"
CFLAGS += -I$(AM_HOME)/am/src/platform/fpga/include
.PHONY: $(AM_HOME)/am/src/platform/fpga/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S -O binary --set-section-flags .bss=alloc,contents $(IMAGE).elf $(IMAGE).bin
