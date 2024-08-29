import "DPI-C" function void sim_end();

module EndControl(
  input clock,
  input reset,
  input isEnd
);

  always @(posedge clock) begin
    if (isEnd) begin
      sim_end();
    end
  end

endmodule