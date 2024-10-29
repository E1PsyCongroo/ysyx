object Elaborate extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).mkString(",")
  )

  circt.stage.ChiselStage.emitSystemVerilogFile(
    new rvcpu.RVCPU(
      awidth     = 32,
      xlen       = 32,
      extentionE = true,
      PCReset    = BigInt("30000000", 16),
      sim        = true
    ),
    args,
    firtoolOptions
  )
}
