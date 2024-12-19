YOSYS_DIR           := $(WORK_DIR)/yosys
YOSYS_V_FILE_GEN    := $(YOSYS_DIR)/SoCCore.sv

# Generating Verilog
$(YOSYS_V_FILE_GEN): $(CHISELSRCS) $(RESOURCES)
	@echo + VERILOG $^
	@mill -i $(PRJ).runMain Yosys --target-dir $(YOSYS_DIR)
	@sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $@
	@sed -i '/firrtl_black_box_resource_files.f/, $$d' $@
