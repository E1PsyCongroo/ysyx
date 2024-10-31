import "DPI-C" function void count_data_fetch(input int cycle);
module TracerDataFetch(
  input clock,
  input reset,
  input start,
  input finish
);
  reg [31:0] cycle_count;
  reg start_flag;
  always @(posedge clock) begin
    if (reset) begin
      cycle_count <= 32'b0;
      start_flag <= 1'b0;
    end
    else if (start && !start_flag) begin
      cycle_count <= 32'b0;
      start_flag <= 1'b1;
    end
    else if (start_flag && finish) begin
      count_data_fetch(cycle_count);
      start_flag <= 1'b0;
    end
    else begin
      cycle_count <= cycle_count + 32'b1;
      start_flag <= start_flag;
    end
  end
endmodule
