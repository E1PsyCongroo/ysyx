vlib modelsim_lib/work
vlib modelsim_lib/msim

vlib modelsim_lib/msim/xilinx_vip
vlib modelsim_lib/msim/xpm
vlib modelsim_lib/msim/xil_defaultlib
vlib modelsim_lib/msim/axi_lite_ipif_v3_0_4
vlib modelsim_lib/msim/lib_pkg_v1_0_4
vlib modelsim_lib/msim/lib_srl_fifo_v1_0_4
vlib modelsim_lib/msim/lib_cdc_v1_0_3
vlib modelsim_lib/msim/axi_uartlite_v2_0_37
vlib modelsim_lib/msim/interrupt_control_v3_1_5
vlib modelsim_lib/msim/axi_gpio_v2_0_35
vlib modelsim_lib/msim/generic_baseblocks_v2_1_2
vlib modelsim_lib/msim/axi_infrastructure_v1_1_0
vlib modelsim_lib/msim/axi_register_slice_v2_1_33
vlib modelsim_lib/msim/fifo_generator_v13_2_11
vlib modelsim_lib/msim/axi_data_fifo_v2_1_32
vlib modelsim_lib/msim/axi_crossbar_v2_1_34
vlib modelsim_lib/msim/proc_sys_reset_v5_0_16
vlib modelsim_lib/msim/axi_vip_v1_1_19
vlib modelsim_lib/msim/processing_system7_vip_v1_0_21
vlib modelsim_lib/msim/axi_clock_converter_v2_1_32
vlib modelsim_lib/msim/axi_protocol_converter_v2_1_33

vmap xilinx_vip modelsim_lib/msim/xilinx_vip
vmap xpm modelsim_lib/msim/xpm
vmap xil_defaultlib modelsim_lib/msim/xil_defaultlib
vmap axi_lite_ipif_v3_0_4 modelsim_lib/msim/axi_lite_ipif_v3_0_4
vmap lib_pkg_v1_0_4 modelsim_lib/msim/lib_pkg_v1_0_4
vmap lib_srl_fifo_v1_0_4 modelsim_lib/msim/lib_srl_fifo_v1_0_4
vmap lib_cdc_v1_0_3 modelsim_lib/msim/lib_cdc_v1_0_3
vmap axi_uartlite_v2_0_37 modelsim_lib/msim/axi_uartlite_v2_0_37
vmap interrupt_control_v3_1_5 modelsim_lib/msim/interrupt_control_v3_1_5
vmap axi_gpio_v2_0_35 modelsim_lib/msim/axi_gpio_v2_0_35
vmap generic_baseblocks_v2_1_2 modelsim_lib/msim/generic_baseblocks_v2_1_2
vmap axi_infrastructure_v1_1_0 modelsim_lib/msim/axi_infrastructure_v1_1_0
vmap axi_register_slice_v2_1_33 modelsim_lib/msim/axi_register_slice_v2_1_33
vmap fifo_generator_v13_2_11 modelsim_lib/msim/fifo_generator_v13_2_11
vmap axi_data_fifo_v2_1_32 modelsim_lib/msim/axi_data_fifo_v2_1_32
vmap axi_crossbar_v2_1_34 modelsim_lib/msim/axi_crossbar_v2_1_34
vmap proc_sys_reset_v5_0_16 modelsim_lib/msim/proc_sys_reset_v5_0_16
vmap axi_vip_v1_1_19 modelsim_lib/msim/axi_vip_v1_1_19
vmap processing_system7_vip_v1_0_21 modelsim_lib/msim/processing_system7_vip_v1_0_21
vmap axi_clock_converter_v2_1_32 modelsim_lib/msim/axi_clock_converter_v2_1_32
vmap axi_protocol_converter_v2_1_33 modelsim_lib/msim/axi_protocol_converter_v2_1_33

vlog -work xilinx_vip -64 -incr -mfcu  -sv -L axi_vip_v1_1_19 -L processing_system7_vip_v1_0_21 -L xilinx_vip "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi4stream_vip_axi4streampc.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi_vip_axi4pc.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/xil_common_vip_pkg.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi4stream_vip_pkg.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi_vip_pkg.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi4stream_vip_if.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/axi_vip_if.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/clk_vip_if.sv" \
"/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/hdl/rst_vip_if.sv" \

vlog -work xpm -64 -incr -mfcu  -sv -L axi_vip_v1_1_19 -L processing_system7_vip_v1_0_21 -L xilinx_vip "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"/opt/Xilinx/Vivado/2024.2/data/ip/xpm/xpm_cdc/hdl/xpm_cdc.sv" \
"/opt/Xilinx/Vivado/2024.2/data/ip/xpm/xpm_memory/hdl/xpm_memory.sv" \

vcom -work xpm -64 -93  \
"/opt/Xilinx/Vivado/2024.2/data/ip/xpm/xpm_VCOMP.vhd" \

vlog -work xil_defaultlib -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../bd/TOP/ip/TOP_clk_wiz_0_0/TOP_clk_wiz_0_0_clk_wiz.v" \
"../../../bd/TOP/ip/TOP_clk_wiz_0_0/TOP_clk_wiz_0_0.v" \

vcom -work axi_lite_ipif_v3_0_4 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/66ea/hdl/axi_lite_ipif_v3_0_vh_rfs.vhd" \

vcom -work lib_pkg_v1_0_4 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/8c68/hdl/lib_pkg_v1_0_rfs.vhd" \

vcom -work lib_srl_fifo_v1_0_4 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/1e5a/hdl/lib_srl_fifo_v1_0_rfs.vhd" \

vcom -work lib_cdc_v1_0_3 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/2a4f/hdl/lib_cdc_v1_0_rfs.vhd" \

vcom -work axi_uartlite_v2_0_37 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/9a87/hdl/axi_uartlite_v2_0_vh_rfs.vhd" \

vcom -work xil_defaultlib -64 -93  \
"../../../bd/TOP/ip/TOP_axi_uartlite_0_0/sim/TOP_axi_uartlite_0_0.vhd" \

vcom -work interrupt_control_v3_1_5 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/d8cc/hdl/interrupt_control_v3_1_vh_rfs.vhd" \

vcom -work axi_gpio_v2_0_35 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6718/hdl/axi_gpio_v2_0_vh_rfs.vhd" \

vcom -work xil_defaultlib -64 -93  \
"../../../bd/TOP/ip/TOP_axi_gpio_0_0/sim/TOP_axi_gpio_0_0.vhd" \

vlog -work generic_baseblocks_v2_1_2 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/0c28/hdl/generic_baseblocks_v2_1_vl_rfs.v" \

vlog -work axi_infrastructure_v1_1_0 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl/axi_infrastructure_v1_1_vl_rfs.v" \

vlog -work axi_register_slice_v2_1_33 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/3ee4/hdl/axi_register_slice_v2_1_vl_rfs.v" \

vlog -work fifo_generator_v13_2_11 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6080/simulation/fifo_generator_vlog_beh.v" \

vcom -work fifo_generator_v13_2_11 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6080/hdl/fifo_generator_v13_2_rfs.vhd" \

vlog -work fifo_generator_v13_2_11 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/6080/hdl/fifo_generator_v13_2_rfs.v" \

vlog -work axi_data_fifo_v2_1_32 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/65ce/hdl/axi_data_fifo_v2_1_vl_rfs.v" \

vlog -work axi_crossbar_v2_1_34 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/a7e3/hdl/axi_crossbar_v2_1_vl_rfs.v" \

vlog -work xil_defaultlib -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../bd/TOP/ip/TOP_axi_interconnect_upgraded_ipi_imp_xbar_0/sim/TOP_axi_interconnect_upgraded_ipi_imp_xbar_0.v" \

vcom -work proc_sys_reset_v5_0_16 -64 -93  \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/0831/hdl/proc_sys_reset_v5_0_vh_rfs.vhd" \

vcom -work xil_defaultlib -64 -93  \
"../../../bd/TOP/ip/TOP_rst_clk_wiz_0_100M_0/sim/TOP_rst_clk_wiz_0_100M_0.vhd" \

vlog -work xil_defaultlib -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../bd/TOP/ip/TOP_FPGA_RVCPU_wrapper_0_0/sim/TOP_FPGA_RVCPU_wrapper_0_0.v" \
"../../../bd/TOP/ip/TOP_BRAM_0_0/sim/TOP_BRAM_0_0.v" \

vlog -work axi_vip_v1_1_19 -64 -incr -mfcu  -sv -L axi_vip_v1_1_19 -L processing_system7_vip_v1_0_21 -L xilinx_vip "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/8c45/hdl/axi_vip_v1_1_vl_rfs.sv" \

vlog -work processing_system7_vip_v1_0_21 -64 -incr -mfcu  -sv -L axi_vip_v1_1_19 -L processing_system7_vip_v1_0_21 -L xilinx_vip "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl/processing_system7_vip_v1_0_vl_rfs.sv" \

vlog -work xil_defaultlib -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../bd/TOP/ip/TOP_processing_system7_0_1/sim/TOP_processing_system7_0_1.v" \
"../../../bd/TOP/sim/TOP.v" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_s00_data_fifo_0/sim/TOP_axi_interconnect_imp_s00_data_fifo_0.v" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_m00_data_fifo_0/sim/TOP_axi_interconnect_imp_m00_data_fifo_0.v" \

vlog -work axi_clock_converter_v2_1_32 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/a4e8/hdl/axi_clock_converter_v2_1_vl_rfs.v" \

vlog -work xil_defaultlib -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_auto_cc_0/sim/TOP_axi_interconnect_imp_auto_cc_0.v" \

vlog -work axi_protocol_converter_v2_1_33 -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../../fpga.gen/sources_1/bd/TOP/ipshared/27ae/hdl/axi_protocol_converter_v2_1_vl_rfs.v" \

vlog -work xil_defaultlib -64 -incr -mfcu  "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/3cbc" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/ec67/hdl" "+incdir+../../../../fpga.gen/sources_1/bd/TOP/ipshared/86fe/hdl" "+incdir+/opt/Xilinx/Vivado/2024.2/data/xilinx_vip/include" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_auto_pc_0/sim/TOP_axi_interconnect_imp_auto_pc_0.v" \
"../../../bd/TOP/ip/TOP_axi_interconnect_imp_auto_pc_1/sim/TOP_axi_interconnect_imp_auto_pc_1.v" \

vlog -work xil_defaultlib \
"glbl.v"

