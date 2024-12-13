`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2024/12/07 20:04:47
// Design Name: 
// Module Name: sim
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////

`define CLK_PERIOD 2
module sim();
  reg clk = 0;
  reg reset = 0;
  reg rx = 0;
  reg [1:0] switches = 0;
  wire tx;
  wire [5:0] leds;
  integer i = 0;

  TOP_wrapper dut(
    .DDR_addr(),
    .DDR_ba(),
    .DDR_cas_n(),
    .DDR_ck_n(),
    .DDR_ck_p(),
    .DDR_cke(),
    .DDR_cs_n(),
    .DDR_dm(),
    .DDR_dq(),
    .DDR_dqs_n(),
    .DDR_dqs_p(),
    .DDR_odt(),
    .DDR_ras_n(),
    .DDR_reset_n(),
    .DDR_we_n(),
    .FIXED_IO_ddr_vrn(),
    .FIXED_IO_ddr_vrp(),
    .FIXED_IO_mio(),
    .FIXED_IO_ps_clk(),
    .FIXED_IO_ps_porb(),
    .FIXED_IO_ps_srstb(),
//    .clock(clk),
    .reset(reset), 
    .uart_rxd(rx), 
    .uart_txd(tx), 
    .leds_tri_o(leds), 
    .switches_tri_i(switches)
  );

  always #(`CLK_PERIOD/2) clk <= ~clk;
  initial begin
    repeat(1000) @(posedge clk);
    reset = 1;
    repeat(1000) @(posedge clk);
    reset = 0;
  end

  initial begin
    #100_000_0000;
    $error("Timeout!");
    $fatal();
    $finish();
  end
endmodule
