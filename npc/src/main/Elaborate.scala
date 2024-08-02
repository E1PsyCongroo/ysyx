object Elaborate extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  println(RV32E.ALUASrcFrom.fromPc.asUInt.isLit)
  // circt.stage.ChiselStage.emitSystemVerilogFile(new RV32E.Control(), args, firtoolOptions)
}
