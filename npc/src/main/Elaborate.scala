object ysyxsoc extends App {
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

object npc extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).mkString(",")
  )

  circt.stage.ChiselStage.emitSystemVerilogFile(
    new rvcpu.NPC(
      awidth     = 32,
      xlen       = 32,
      extentionE = true,
      PCReset    = BigInt("80000000", 16),
      sim        = true,
      useDPIC    = true
    ),
    args,
    firtoolOptions
  )
}

object Yosys extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
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
      sim        = false
    ),
    args,
    firtoolOptions
  )
}

object IVerilog extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).mkString(",")
  )

  circt.stage.ChiselStage.emitSystemVerilogFile(
    new rvcpu.NPC(
      awidth     = 32,
      xlen       = 32,
      extentionE = true,
      PCReset    = BigInt("80000000", 16),
      sim        = true,
      useDPIC    = false
    ),
    args,
    firtoolOptions
  )
}
