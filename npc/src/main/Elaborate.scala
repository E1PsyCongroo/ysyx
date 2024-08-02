object Elaborate extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  val top = new RVCPU.RVCPU(
    xlen       = 32,
    extentionE = true,
    extentionC = false,
    PCReset   = BigInt("8000_0000", 16)
  )
  circt.stage.ChiselStage.emitSystemVerilogFile(top, args, firtoolOptions)
}
