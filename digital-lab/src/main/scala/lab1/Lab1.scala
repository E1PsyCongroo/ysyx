import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

class Lab1 extends Module {
  val io = IO(new Bundle {
    val X0 = Input(UInt(2.W))
    val X1 = Input(UInt(2.W))
    val X2 = Input(UInt(2.W))
    val X3 = Input(UInt(2.W))
    val Y  = Input(UInt(2.W))
    val F  = Output(UInt(2.W))
  })
  io.F := MuxLookup(io.Y, io.X0)(
    Seq(
      "b00".U -> io.X0,
      "b01".U -> io.X1,
      "b10".U -> io.X2,
      "b11".U -> io.X3
    )
  )
}

object Lab1 extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  ChiselStage.emitSystemVerilogFile(new Lab1(), args, firtoolOptions)
}
