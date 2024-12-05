-include $(NPC_HOME)/../Makefile
include $(NPC_HOME)/scripts/make/build.mk

include $(NPC_HOME)/tools/difftest.mk

compile_git:
	$(call git_commit, "compile NPC")
$(BINARY):: compile_git

# Some convenient rules

override ARGS ?= --log=$(BUILD_DIR)/npc-log.txt --wave=$(BUILD_DIR)/npc-wave.fst
ifdef ELF
override ARGS += --elf=$(ELF)
endif
override ARGS += $(ARGS_DIFF)

# Command to execute NPC
IMG ?=
NPC_EXEC := $(BINARY) $(ARGS) $(IMG)

test:
	mill -i $(PRJ).test

mill-help:
	mill -i $(PRJ).runMain $(PLATFORM) --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

verilog: $(BUILD_DIR)/.stamp.verilog$(PLATFORM)

verilator: $(VERILATOR_DIR)/lib$(PRJ).so

lint: verilog
	@$(VERILATOR) --lint-only -Wall -Wno-DECLFILENAME $(VERILATOR_INCLUDES) \
		--top-module $(TOP_MODULE) $(VSRCS) \

run-env: $(BINARY) $(DIFF_REF_SO)

run: run-env
	$(call git_commit, "run NPC")
	$(NPC_EXEC)

gdb: run-env
	$(call git_commit, "gdb NPC")
	gdb -s $(BINARY) --args $(NPC_EXEC)

perf: run-env
	@make -C $(NPC_HOME)/../am-kernels/benchmarks/microbench ARCH=riscv32e-ysyxsoc NPCFLAGS="-b" mainargs=train run | tee perf-log/log_$(shell date +"%Y-%m-%dT%H_%M_%S").txt

sta: $(YOSYS_V_FILE_GEN)
	@make -C $(YOSYS_DIR) sta

clean-yosys:
	-rm -rf $(YOSYS_DIR)/result *.sv

iverilog: $(IVERILOG_DIR)/vsim/simtop.tbi

vsim: $(IVERILOG_DIR)/vsim/simtop.fst

clean-vsim:
	-rm -rf $(IVERILOG_DIR)/vsim//*.vpd $(IVERILOG_DIR)/vsim/*.vcd \
	$(IVERILOG_DIR)/vsim/*.tbi $(IVERILOG_DIR)/vsim/*.fst \
	$(IVERILOG_DIR)/vsim/*.jou $(IVERILOG_DIR)/vsim/*.log \
	$(IVERILOG_DIR)/vsim/*.out

clean-tools = $(dir $(shell find ./tools -maxdepth 2 -mindepth 2 -name "Makefile"))
$(clean-tools):
	-@$(MAKE) -s -C $@ clean
clean-tools: $(clean-tools)
clean-all: clean distclean clean-tools clean-vsim

.PHONY: test mill-help reformat checkformat bsp idea \
        verilog verilator lint run-env run gdb \
        clean-tools clean-all $(clean-tools) sta perf \
        iverilog vsim clean-vsim
