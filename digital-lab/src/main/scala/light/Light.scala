import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

class Light extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(16.W))
  })
  val count = RegInit(0.U(32.W))
  val ledReg = RegInit(1.U(16.W))
  count := Mux(count >= 5000000.U, 0.U, count + 1.U)
  ledReg := Mux(count === 0.U, ledReg(14, 0) ## ledReg(15), ledReg)
  io.led := ledReg
}

object Light extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new Light(), args, firtoolOptions)
}