FPGA_DIR                 := $(YSYX_HOME)/fpga
FPGA_SIM_DIR             := $(FPGA_DIR)/iverilog
FPGA_RTL                 := $(wildcard $(FPGA_SIM_DIR)/vsrc/*.sv)
FPGA_SIM_RTL             := $(wildcard $(FPGA_SIM_DIR)/vsim/*.sv)
FPGA_SIM_MODELS          := /opt/Xilinx/Vivado/2023.1/data/verilog/src/glbl.v $(wildcard $(FPGA_SIM_DIR)/vsim_models/*.v)

FPGA_IVERILOG            := iverilog
FPGA_IVERILOG_INC_PATH   := /opt/Xilinx/Vivado/2023.1/data/verilog/src/unisims $(FPGA_SIM_DIR)/vsim
FPGA_IVERILOG_INCLUDES   := $(addprefix -y , $(FPGA_IVERILOG_INC_PATH))
FPGA_IVERILOG_OPTS       := -Ttyp -g2012 -gassertions -Wall -Wno-timescale $(FPGA_IVERILOG_INCLUDES)
FPGA_VVP                 := vvp

$(FPGA_SIM_DIR)/.stamp.verilog.fpga.iverilog: $(CHISELSRCS) $(RESOURCES)
	@echo + VERILOG $^
	@mkdir -p $(FPGA_SIM_DIR)/vsrc
	@mill -i $(PRJ).runMain FPGASIM --target-dir $(FPGA_SIM_DIR)/vsrc --split-verilog
	@touch $@

$(FPGA_SIM_DIR)/vsim/%.tbi: $(FPGA_SIM_DIR)/vsim/%.sv $(FPGA_SIM_DIR)/.stamp.verilog.fpga.iverilog $(FPGA_RTL)
	$(FPGA_IVERILOG) $(FPGA_IVERILOG_OPTS) -o $(FPGA_SIM_DIR)/vsim/$*.tbi $(FPGA_SIM_DIR)/vsim/$*.sv $(FPGA_RTL) $(FPGA_SIM_MODELS)

$(FPGA_SIM_DIR)/vsim/%.fst: $(FPGA_SIM_DIR)/vsim/%.tbi
	cd $(@D) && $(FPGA_VVP) $*.tbi -fst |& tee $*.log

.PRECIOUS: $(FPGA_SIM_DIR)/vsim/%.tbi $(FPGA_SIM_DIR)/vsim/%.fst

