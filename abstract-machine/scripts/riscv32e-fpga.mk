include $(AM_HOME)/scripts/isa/riscv.mk
include $(AM_HOME)/scripts/platform/fpga.mk
CFLAGS  += -DISA_H=\"riscv/riscv.h\"
COMMON_CFLAGS += -march=rv32e_zicsr_zifencei -mabi=ilp32e  # overwrite
LDFLAGS       += -melf32lriscv                    # overwrite

AM_SRCS += riscv/fpga/start.S \
           riscv/fpga/cte.c \
           riscv/fpga/trap.S \
           riscv/fpga/libgcc/div.S \
           riscv/fpga/libgcc/muldi3.S \
           riscv/fpga/libgcc/multi3.c \
           riscv/fpga/libgcc/ashldi3.c \
           riscv/fpga/libgcc/unused.c
