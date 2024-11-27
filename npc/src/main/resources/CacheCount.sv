import "DPI-C" function void count_cache_hit(input int hit, input int unsigned access_cycle, input int unsigned miss_penalty);

module CacheCount(
  input clock,
  input enable,
  input [31:0] hit,
  input [31:0] access_cycle,
  input [31:0] miss_penalty
);

always @(posedge clock) begin
  if (enable) count_cache_hit(hit, access_cycle, miss_penalty);
end

endmodule
