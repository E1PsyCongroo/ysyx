import "DPI-C" function void count_data_fetch(input int cycle);
module TracerDataFetch(
  input clock,
  input start,
  input finish
);
  reg [31:0] cycle_count;
  always @(posedge clock) begin
    if (start) begin
      cycle_count <= 32'b0;
    end
    else if (finish) begin
      count_data_fetch(cycle_count);
    end
    else begin
      cycle_count <= cycle_count + 32'b1;
    end
  end
endmodule
