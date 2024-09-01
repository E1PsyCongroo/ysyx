import "DPI-C" function void difftest_skip_ref();
module SkipDifftest(
  input clock,
  input skip
);
  always @(posedge clock) begin
    if (skip) difftest_skip_ref();
  end
endmodule
