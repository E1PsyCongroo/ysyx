transcript off
onbreak {quit -force}
onerror {quit -force}
transcript on

asim +access +r +m+RAM_IP  -L xil_defaultlib -L xilinx_vip -L xpm -L blk_mem_gen_v8_4_9 -L xilinx_vip -L unisims_ver -L unimacro_ver -L secureip -O5 xil_defaultlib.RAM_IP xil_defaultlib.glbl

do {RAM_IP.udo}

run 1000ns

endsim

quit -force
