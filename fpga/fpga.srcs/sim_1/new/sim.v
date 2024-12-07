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
  wire tx;
  integer i = 0;

  FPGA dut(.clock(clk), .reset(reset), .io_rx(rx), .io_tx(tx));

  always #(`CLK_PERIOD/2) clk <= ~clk;
  initial begin
    repeat(10) @(posedge clk);
    reset = 1;
    repeat(10) @(posedge clk);
    reset = 0;
  end

  initial begin
    #100_000_0000;
    $error("Timeout!");
    $fatal();
    $finish();
  end
endmodule
