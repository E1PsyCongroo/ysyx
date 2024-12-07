transcript off
onbreak {quit -force}
onerror {quit -force}
transcript on

asim +access +r +m+AXI4_TO_AXI4Lite_IP  -L xpm -L generic_baseblocks_v2_1_0 -L fifo_generator_v13_2_8 -L axi_data_fifo_v2_1_27 -L axi_infrastructure_v1_1_0 -L axi_register_slice_v2_1_28 -L axi_protocol_converter_v2_1_28 -L xil_defaultlib -L unisims_ver -L unimacro_ver -L secureip -O2 xil_defaultlib.AXI4_TO_AXI4Lite_IP xil_defaultlib.glbl

do {AXI4_TO_AXI4Lite_IP.udo}

run

endsim

quit -force
