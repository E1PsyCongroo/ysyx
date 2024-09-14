import "DPI-C" function void npc_difftest_skipi_ref();
module SkipDifftest(
  input clock,
  input skip
);
  always @(posedge clock) begin
    if (skip) npc_difftest_skipi_ref();
  end
endmodule
