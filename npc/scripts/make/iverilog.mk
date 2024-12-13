IVERILOG_DIR        := $(WORK_DIR)/iverilog
IVERILOG_RTL        ?= $(wildcard $(IVERILOG_DIR)/vsrc/*.sv)
IVERILOG_SIM_RTL    ?= $(wildcard $(IVERILOG_DIR)/vsim/*.sv)
IVERILOG_SIM_MODELS :=

IVERILOG            := iverilog
IVERILOG_OPTS       := -Ttyp -g2012 -gassertions -Wall -Wno-timescale $(IVERILOG_DIR)/vsim
VVP                 := vvp

$(IVERILOG_DIR)/.stamp.verilog.iverilog: $(CHISELSRCS) $(RESOURCES)
	@echo + VERILOG $^
	@mkdir -p $(IVERILOG_DIR)/vsrc
	@mill -i $(PRJ).runMain IVerilog --target-dir $(IVERILOG_DIR)/vsrc --split-verilog
	@touch $@

$(IVERILOG_DIR)/vsim/%.tbi: $(IVERILOG_DIR)/vsim/%.sv $(IVERILOG_DIR)/.stamp.verilog.iverilog $(IVERILOG_RTL)
	$(IVERILOG) $(IVERILOG_OPTS) -o $(IVERILOG_DIR)/vsim/$*.tbi $(IVERILOG_DIR)/vsim/$*.sv $(IVERILOG_RTL) $(IVERILOG_SIM_MODELS)

$(IVERILOG_DIR)/vsim/%.fst: $(IVERILOG_DIR)/vsim/%.tbi
	cd $(@D) && $(VVP) $*.tbi -fst |& tee $*.log

.PRECIOUS: $(IVERILOG_DIR)/vsim/%.tbi $(IVERILOG_DIR)/vsim/%.fst
