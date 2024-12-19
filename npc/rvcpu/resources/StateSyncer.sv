import "DPI-C" function void rvcpu_trace_cache(
  input int hit,
  input longint unsigned access_cycle,
  input longint unsigned miss_penalty
);
import "DPI-C" function void rvcpu_sim_end(input int code);
import "DPI-C" function void rvcpu_decode_inst(
  input int unsigned inst,
  input int unsigned npc,
  input longint unsigned icache_cost_cycle,
  input longint unsigned idu_cost_cycle,
  input longint unsigned exu_cost_cycle,
  input longint unsigned lsu_cost_cycle,
  input longint unsigned wbu_cost_cycle
);
import "DPI-C" function void rvcpu_sync_csrs(input int unsigned csrState []);
import "DPI-C" function void rvcpu_sync_gprs(input int unsigned gprState []);
import "DPI-C" function void rvcpu_difftest_skip_ref();
import "DPI-C" function void rvcpu_trace_read(input int unsigned addr, input int len, input int unsigned data);
import "DPI-C" function void rvcpu_trace_write(input int unsigned addr, input int len, input int unsigned data);

module StateSyncer(
  input clock,
  input reset,
  input en,

  input [63:0] icacheCostCycle,
  input [63:0] icacheMissCost,
  input [63:0] iduCostCycle,
  input [63:0] exuCostCycle,
  input [63:0] lsuCostCycle,
  input [63:0] wbuCostCycle,

  input [31:0] pc,
  input [31:0] npc,
  input [31:0] inst,
  input icacheNeed,
  input icacheHit,
  input skipDifftest,
  input simEnd,
  input [31:0] exitCode,
  input [31:0] csrState_0,
  input [31:0] csrState_1,
  input [31:0] csrState_2,
  input [31:0] csrState_3,
  input [31:0] csrState_4,
  input [31:0] csrState_5,
  input memRen,
  input memWen,
  input [2:0] memAccessSize,
  input [31:0] memAddr,
  input [31:0] memData,
  input [31:0] gprState_0,
  input [31:0] gprState_1,
  input [31:0] gprState_2,
  input [31:0] gprState_3,
  input [31:0] gprState_4,
  input [31:0] gprState_5,
  input [31:0] gprState_6,
  input [31:0] gprState_7,
  input [31:0] gprState_8,
  input [31:0] gprState_9,
  input [31:0] gprState_10,
  input [31:0] gprState_11,
  input [31:0] gprState_12,
  input [31:0] gprState_13,
  input [31:0] gprState_14,
  input [31:0] gprState_15,
  input [31:0] gprState_16,
  input [31:0] gprState_17,
  input [31:0] gprState_18,
  input [31:0] gprState_19,
  input [31:0] gprState_20,
  input [31:0] gprState_21,
  input [31:0] gprState_22,
  input [31:0] gprState_23,
  input [31:0] gprState_24,
  input [31:0] gprState_25,
  input [31:0] gprState_26,
  input [31:0] gprState_27,
  input [31:0] gprState_28,
  input [31:0] gprState_29,
  input [31:0] gprState_30,
  input [31:0] gprState_31
);

  bit [31:0] csrState [0:5];
  assign csrState[0] = csrState_0;
  assign csrState[1] = csrState_1;
  assign csrState[2] = csrState_2;
  assign csrState[3] = csrState_3;
  assign csrState[4] = csrState_4;
  assign csrState[5] = csrState_5;
  bit [31:0] gprState [0:31];
  assign gprState[0] = gprState_0;
  assign gprState[1] = gprState_1;
  assign gprState[2] = gprState_2;
  assign gprState[3] = gprState_3;
  assign gprState[4] = gprState_4;
  assign gprState[5] = gprState_5;
  assign gprState[6] = gprState_6;
  assign gprState[7] = gprState_7;
  assign gprState[8] = gprState_8;
  assign gprState[9] = gprState_9;
  assign gprState[10] = gprState_10;
  assign gprState[11] = gprState_11;
  assign gprState[12] = gprState_12;
  assign gprState[13] = gprState_13;
  assign gprState[14] = gprState_14;
  assign gprState[15] = gprState_15;
  assign gprState[16] = gprState_16;
  assign gprState[17] = gprState_17;
  assign gprState[18] = gprState_18;
  assign gprState[19] = gprState_19;
  assign gprState[20] = gprState_20;
  assign gprState[21] = gprState_21;
  assign gprState[22] = gprState_22;
  assign gprState[23] = gprState_23;
  assign gprState[24] = gprState_24;
  assign gprState[25] = gprState_25;
  assign gprState[26] = gprState_26;
  assign gprState[27] = gprState_27;
  assign gprState[28] = gprState_28;
  assign gprState[29] = gprState_29;
  assign gprState[30] = gprState_30;
  assign gprState[31] = gprState_31;

  always @(posedge clock) begin
    if (en && !reset) begin
      if (icacheNeed) rvcpu_trace_cache({31'b0, icacheHit}, icacheCostCycle - icacheMissCost, icacheMissCost);
      if (simEnd) rvcpu_sim_end(exitCode);
      if (skipDifftest) rvcpu_difftest_skip_ref();
      rvcpu_trace_read(pc, 4, inst);
      if (memRen) rvcpu_trace_read(memAddr, {29'b0, memAccessSize}, memData);
      if (memWen) rvcpu_trace_write(memAddr, {29'b0, memAccessSize}, memData);
      rvcpu_decode_inst(inst, npc, icacheCostCycle, iduCostCycle, exuCostCycle, lsuCostCycle, wbuCostCycle);
      rvcpu_sync_csrs(csrState);
      rvcpu_sync_gprs(gprState);
    end
  end

endmodule //StateSyncer
