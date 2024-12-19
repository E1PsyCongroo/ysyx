`timescale 1ns / 1ps
`define CLK_PERIOD 2
module simtop();
  reg clk = 0;
  reg reset = 0;
  wire sync, simEnd;
  integer i = 0;

  NPC dut(.clock(clk), .reset(reset), .io_sync(sync), .io_simEnd(simEnd));

  always #(`CLK_PERIOD/2) clk <= ~clk;
  initial begin
    $dumpfile("simtop.fst");
    $dumpvars(0, simtop);
    for (i = 0; i < 4194304; i = i + 1) begin
      dut.mem.Mem.mem_ext.Memory[i] = 0;
    end
    $readmemh(
      "/home/focused_xy/cs/ysyx/npc/iverilog/hex/microbench-riscv32e-npc.hex",
      dut.mem.Mem.mem_ext.Memory
    );
    repeat(10) @(posedge clk);
    reset = 1;
    repeat(10) @(posedge clk);
    reset = 0;
    wait(sync === 1 && simEnd === 1);
    $finish();
  end

  initial begin
    #1000_0000;
    $error("Timeout!");
    $fatal();
    $finish();
  end
endmodule // simtop
