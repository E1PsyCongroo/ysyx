`timescale 1ns / 1ps
`define CLK_PERIOD 2
module simtop();
  reg clk = 0;
  reg reset = 0;
  integer i = 0;

  NPC dut(.clock(clk), .reset(reset));

  always #(`CLK_PERIOD/2) clk <= ~clk;
  initial begin
    $dumpfile("simtop.fst");
    $dumpvars(0, simtop);
    for (i = 0; i < 4194304; i = i + 1) begin
      dut.mem.readData_Mem.mem_ext.Memory[i] = 0;
    end
    $readmemh(
      "/home/focused_xy/cs/ysyx/npc/iverilog/hex/hello-riscv32e-npc.hex",
      dut.mem.readData_Mem.mem_ext.Memory
    );
    repeat(10) @(posedge clk);
    reset = 1;
    repeat(10) @(posedge clk);
    reset = 0;
    wait(dut.npc.WBU.io_out_valid === 1 && dut.npc.WBU.io_out_bits_isEnd === 1);
    $finish();
  end

  initial begin
    #1000_0000;
    $error("Timeout!");
    $fatal();
    $finish();
  end
endmodule // simtop
