object Elaborate extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket",
    "split-verilog",
  ).reduce(_ + "," + _), "--split-verilog")
  circt.stage.ChiselStage.emitSystemVerilogFile(
    new RVCPU.RVCPU(
      xlen       = 32,
      extentionE = true,
      extentionC = false,
      PCReset   = BigInt("80000000", 16)
    ),
    args,
    firtoolOptions
  )
}
