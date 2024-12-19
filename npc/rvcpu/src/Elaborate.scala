import rvcpu._
import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._

object RVCPU extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).mkString(",")
  )

  val axiParams = AXI4BundleParameters(
    addrBits          = 32,
    dataBits          = 32,
    idBits            = 4,
    regionPresent     = false,
    wlastPresent      = true,
    rlastPresent      = true,
    sizePresent       = true,
    lenPrenset        = true,
    burstPresent      = true,
    wstrbPresent      = true,
    brespPresent      = true,
    rrespPresent      = true,
    cachePrenset      = false,
    protPrensent      = false,
    qosPrensent       = false,
    exclusiveAccesses = false
  )
  val cpuConfig = CPUConfig(
    frequency  = BigInt("50000000"),
    xlen       = 32,
    extentionE = true,
    PCReset    = BigInt("80000000", 16),
    icacheConfig = ICacheConfig(
      totalWidth         = 6,
      blockWidth         = 5,
      associativityWidth = 0,
      icacheNeed         = Seq(Dev.npc.PMEMAddr)
    ),
    sim = true
  )
  circt.stage.ChiselStage.emitSystemVerilogFile(
    new rvcpu.core.RVCPU(cpuConfig, axiParams),
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
    new rvcpu.NPC(useDPIC = true),
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
    new rvcpu.NPC(useDPIC = false),
    args,
    firtoolOptions
  )
}

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
    new rvcpu.SoCCore(useDPIC = true),
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
    new rvcpu.SoCCore(useDPIC = false),
    args,
    firtoolOptions
  )
}

// object FPGA_RVCPU_SIM extends App {
//   val firtoolOptions = Array(
//     "--lowering-options=" + List(
//       "disallowLocalVariables",
//       "disallowPackedArrays",
//       "locationInfoStyle=wrapInAtSquareBracket",
//       "mitigateVivadoArrayIndexConstPropBug"
//     ).mkString(",")
//   )

//   circt.stage.ChiselStage.emitSystemVerilogFile(
//     new rvcpu.FPGA_RVCPU(
//       awidth     = 32,
//       xlen       = 32,
//       extentionE = true,
//       PCReset    = BigInt("0f000000", 16),
//       sim        = true
//     ),
//     args,
//     firtoolOptions
//   )
// }

// object FPGA_RVCPU extends App {
//   val firtoolOptions = Array(
//     "--lowering-options=" + List(
//       "disallowLocalVariables",
//       "disallowPackedArrays",
//       "locationInfoStyle=wrapInAtSquareBracket",
//       "mitigateVivadoArrayIndexConstPropBug"
//     ).mkString(",")
//   )

//   circt.stage.ChiselStage.emitSystemVerilogFile(
//     new rvcpu.FPGA_RVCPU(
//       awidth     = 32,
//       xlen       = 32,
//       extentionE = true,
//       PCReset    = BigInt("0f000000", 16),
//       sim        = false
//     ),
//     args,
//     firtoolOptions
//   )
// }
