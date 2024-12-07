transcript off
onbreak {quit -force}
onerror {quit -force}
transcript on

asim +access +r +m+CLK_WIZ_IP  -L xpm -L xil_defaultlib -L unisims_ver -L unimacro_ver -L secureip -O5 xil_defaultlib.CLK_WIZ_IP xil_defaultlib.glbl

do {CLK_WIZ_IP.udo}

run 1000ns

endsim

quit -force
