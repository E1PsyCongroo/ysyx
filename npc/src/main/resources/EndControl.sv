import "DPI-C" function void sim_end(input int code);

module EndControl(
  input clock,
  input reset,
  input [31:0] code,
  input isEnd
);

  always @(posedge clock) begin
    if (isEnd) begin
      sim_end(code);
    end
  end

endmodule
