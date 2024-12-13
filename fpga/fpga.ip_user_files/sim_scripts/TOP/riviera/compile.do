transcript off
onbreak {quit -force}
onerror {quit -force}
transcript on

vlib work
vlib riviera/xilinx_vip
vlib riviera/xpm
vlib riviera/xil_defaultlib
vlib riviera/axi_lite_ipif_v3_0_4
vlib riviera/lib_pkg_v1_0_4
vlib riviera/lib_srl_fifo_v1_0_4
vlib riviera/lib_cdc_v1_0_3
vlib riviera/axi_uartlite_v2_0_37
vlib riviera/interrupt_control_v3_1_5
vlib riviera/axi_gpio_v2_0_35
vlib riviera/generic_baseblocks_v2_1_2
vlib riviera/axi_infrastructure_v1_1_0
vlib riviera/axi_register_slice_v2_1_33
vlib riviera/fifo_generator_v13_2_11
vlib riviera/axi_data_fifo_v2_1_32
vlib riviera/axi_crossbar_v2_1_34
vlib riviera/proc_sys_reset_v5_0_16
vlib riviera/axi_vip_v1_1_19
vlib riviera/processing_system7_vip_v1_0_21
vlib riviera/axi_clock_converter_v2_1_32
vlib riviera/axi_protocol_converter_v2_1_33

vmap xilinx_vip riviera/xilinx_vip
vmap xpm riviera/xpm
vmap xil_defaultlib riviera/xil_defaultlib
vmap axi_lite_ipif_v3_0_4 riviera/axi_lite_ipif_v3_0_4
vmap lib_pkg_v1_0_4 riviera/lib_pkg_v1_0_4
vmap lib_srl_fifo_v1_0_4 riviera/lib_srl_fifo_v1_0_4
vmap lib_cdc_v1_0_3 riviera/lib_cdc_v1_0_3
vmap axi_uartlite_v2_0_37 riviera/axi_uartlite_v2_0_37
vmap interrupt_control_v3_1_5 riviera/interrupt_control_v3_1_5
vmap axi_gpio_v2_0_35 riviera/axi_gpio_v2_0_35
vmap generic_baseblocks_v2_1_2 riviera/generic_baseblocks_v2_1_2
vmap axi_infrastructure_v1_1_0 riviera/axi_infrastructure_v1_1_0
vmap axi_register_slice_v2_1_33 riviera/axi_register_slice_v2_1_33
vmap fifo_generator_v13_2_11 riviera/fifo_generator_v13_2_11
vmap axi_data_fifo_v2_1_32 riviera/axi_data_fifo_v2_1_32
vmap axi_crossbar_v2_1_34 riviera/axi_crossbar_v2_1_34
vmap proc_sys_reset_v5_0_16 riviera/proc_sys_reset_v5_0_16
vmap axi_vip_v1_1_19 riviera/axi_vip_v1_1_19
vmap processing_system7_vip_v1_0_21 riviera/processing_system7_vip_v1_0_21
vmap axi_clock_converter_v2_1_32 riviera/axi_clock_converter_v2_1_32
vmap axi_protocol_converter_v2_1_33 riviera/axi_protocol_converter_v2_1_33

vlog -work xilinx_vip  -incr "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi4stream_vip_axi4streampc.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi_vip_axi4pc.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/xil_common_vip_pkg.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi4stream_vip_pkg.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi_vip_pkg.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi4stream_vip_if.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi_vip_if.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/clk_vip_if.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/rst_vip_if.sv" \

vlog -work xpm  -incr "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"/opt/Xilinx/Vivado/2024.2/data/ip/xpm/xpm_cdc/hdl/xpm_cdc.sv" \
"/opt/Xilinx/Vivado/2024.2/data/ip/xpm/xpm_memory/hdl/xpm_memory.sv" \

vcom -work xpm -93  -incr \
"/opt/Xilinx/Vivado/2024.2/data/ip/xpm/xpm_VCOMP.vhd" \

vlog -work xil_defaultlib  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../bd/TOP/ip/TOP_clk_wiz_0_0/TOP_clk_wiz_0_0_clk_wiz.v" \
"../../../bd/TOP/ip/TOP_clk_wiz_0_0/TOP_clk_wiz_0_0.v" \

vcom -work axi_lite_ipif_v3_0_4 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/66ea/hdl/axi_lite_ipif_v3_0_vh_rfs.vhd" \

vcom -work lib_pkg_v1_0_4 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/8c68/hdl/lib_pkg_v1_0_rfs.vhd" \

vcom -work lib_srl_fifo_v1_0_4 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/1e5a/hdl/lib_srl_fifo_v1_0_rfs.vhd" \

vcom -work lib_cdc_v1_0_3 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/2a4f/hdl/lib_cdc_v1_0_rfs.vhd" \

vcom -work axi_uartlite_v2_0_37 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/9a87/hdl/axi_uartlite_v2_0_vh_rfs.vhd" \

vcom -work xil_defaultlib -93  -incr \
"../../../bd/TOP/ip/TOP_axi_uartlite_0_0/sim/TOP_axi_uartlite_0_0.vhd" \

vcom -work interrupt_control_v3_1_5 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/d8cc/hdl/interrupt_control_v3_1_vh_rfs.vhd" \

vcom -work axi_gpio_v2_0_35 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6718/hdl/axi_gpio_v2_0_vh_rfs.vhd" \

vcom -work xil_defaultlib -93  -incr \
"../../../bd/TOP/ip/TOP_axi_gpio_0_0/sim/TOP_axi_gpio_0_0.vhd" \

vlog -work generic_baseblocks_v2_1_2  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/0c28/hdl/generic_baseblocks_v2_1_vl_rfs.v" \

vlog -work axi_infrastructure_v1_1_0  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl/axi_infrastructure_v1_1_vl_rfs.v" \

vlog -work axi_register_slice_v2_1_33  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/3ee4/hdl/axi_register_slice_v2_1_vl_rfs.v" \

vlog -work fifo_generator_v13_2_11  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6080/simulation/fifo_generator_vlog_beh.v" \

vcom -work fifo_generator_v13_2_11 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6080/hdl/fifo_generator_v13_2_rfs.vhd" \

vlog -work fifo_generator_v13_2_11  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6080/hdl/fifo_generator_v13_2_rfs.v" \

vlog -work axi_data_fifo_v2_1_32  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/65ce/hdl/axi_data_fifo_v2_1_vl_rfs.v" \

vlog -work axi_crossbar_v2_1_34  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/a7e3/hdl/axi_crossbar_v2_1_vl_rfs.v" \

vlog -work xil_defaultlib  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../bd/TOP/ip/TOP_axi_interconnect_upgraded_ipi_imp_xbar_0/sim/TOP_axi_interconnect_upgraded_ipi_imp_xbar_0.v" \

vcom -work proc_sys_reset_v5_0_16 -93  -incr \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/0831/hdl/proc_sys_reset_v5_0_vh_rfs.vhd" \

vcom -work xil_defaultlib -93  -incr \
"../../../bd/TOP/ip/TOP_rst_clk_wiz_0_100M_0/sim/TOP_rst_clk_wiz_0_100M_0.vhd" \

vlog -work xil_defaultlib  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../bd/TOP/ip/TOP_FPGA_RVCPU_wrapper_0_0/sim/TOP_FPGA_RVCPU_wrapper_0_0.v" \
"../../../bd/TOP/ip/TOP_BRAM_0_0/sim/TOP_BRAM_0_0.v" \

vlog -work axi_vip_v1_1_19  -incr "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/8c45/hdl/axi_vip_v1_1_vl_rfs.sv" \

vlog -work processing_system7_vip_v1_0_21  -incr "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl/processing_system7_vip_v1_0_vl_rfs.sv" \

vlog -work xil_defaultlib  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../bd/TOP/ip/TOP_processing_system7_0_1/sim/TOP_processing_system7_0_1.v" \
"../../../bd/TOP/sim/TOP.v" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_s00_data_fifo_0/sim/TOP_axi_interconnect_imp_s00_data_fifo_0.v" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_m00_data_fifo_0/sim/TOP_axi_interconnect_imp_m00_data_fifo_0.v" \

vlog -work axi_clock_converter_v2_1_32  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/a4e8/hdl/axi_clock_converter_v2_1_vl_rfs.v" \

vlog -work xil_defaultlib  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_auto_cc_0/sim/TOP_axi_interconnect_imp_auto_cc_0.v" \

vlog -work axi_protocol_converter_v2_1_33  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/27ae/hdl/axi_protocol_converter_v2_1_vl_rfs.v" \

vlog -work xil_defaultlib  -incr -v2k5 "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" -l xilinx_vip -l xpm -l xil_defaultlib -l axi_lite_ipif_v3_0_4 -l lib_pkg_v1_0_4 -l lib_srl_fifo_v1_0_4 -l lib_cdc_v1_0_3 -l axi_uartlite_v2_0_37 -l interrupt_control_v3_1_5 -l axi_gpio_v2_0_35 -l generic_baseblocks_v2_1_2 -l axi_infrastructure_v1_1_0 -l axi_register_slice_v2_1_33 -l fifo_generator_v13_2_11 -l axi_data_fifo_v2_1_32 -l axi_crossbar_v2_1_34 -l proc_sys_reset_v5_0_16 -l axi_vip_v1_1_19 -l processing_system7_vip_v1_0_21 -l axi_clock_converter_v2_1_32 -l axi_protocol_converter_v2_1_33 \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_auto_pc_0/sim/TOP_axi_interconnect_imp_auto_pc_0.v" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_auto_pc_1/sim/TOP_axi_interconnect_imp_auto_pc_1.v" \

vlog -work xil_defaultlib \
"glbl.v"
