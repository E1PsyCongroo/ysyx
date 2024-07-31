PRJ 														:= playground
ABS_TOP 												:= $(shell pwd)
BUILD_DIR 											:= $(ABS_TOP)/build
OBJ_DIR 												:= $(BUILD_DIR)/obj_dir
SRC_DIR													:= $(ABS_TOP)/src/main
VSRC_DIR 												:= $(SRC_DIR)/vsrc
CSRC_DIR 												:= $(SRC_DIR)/csrc
CONSTR_DIR											:= $(SRC_DIR)/constr

# project source
VSRCS 													+= $(shell find $(VSRC_DIR) -type f -name "*.v" -or -name "*.sv")
CSRCS 													+= $(shell find $(CSRC_DIR) -type f -name "*.c" -or -name "*.cc" -or -name "*.cpp")

# verilator flags
VERILATOR 											:= verilator
VERILATOR_CFLAGS 								?= --MMD --build -sv --cc -O3 --x-assign fast --x-initial fast --noassert --trace

# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# rules for verilator
INCFLAGS 												:= $(addprefix -I, $(INC_PATH))
CXXFLAGS 												+= $(INCFLAGS)

$(shell mkdir -p $(BUILD_DIR))

$(BUILD_DIR)/%_auto_bind.cpp: $(CONSTR_DIR)/%.nxdc
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

test:
	mill -i $(PRJ).test

$(VSRC_DIR)/%.sv:
	$(call git_commit, "generate verilog")
	mill -i $(PRJ).runMain $* --target-dir $(VSRC_DIR)

help/%:
	mill -i $(PRJ).runMain $* --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR) $(VSRC_DIR)/*

$(BUILD_DIR)/%: $(VSRC_DIR)/%.sv $(BUILD_DIR)/%_auto_bind.cpp $(CSRC_DIR)/%.cpp $(VSRCS) $(NVBOARD_ARCHIVE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $* $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		-CFLAGS -DTOP_NAME=V$* \
		--Mdir $(OBJ_DIR) --exe -o $(BUILD_DIR)/$*

sim/%: $(BUILD_DIR)/%
	$(BUILD_DIR)/$*

.PHONY: test help reformat checkformat clean sim/%
.PRECIOUS: $(BUILD_DIR)/%_auto_bind.cpp $(BUILD_DIR)/% $(VSRC_DIR)/%.sv
-include ../Makefile
