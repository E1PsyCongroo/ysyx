import org.scalatest._

import rvcpu.core._
import rvcpu.axi4._
import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ICacheTest extends Module {
  val io = IO(new Bundle {
    val req = Flipped(DecoupledIO(new ICacheIn(7)))
    val block = Input(Bool())
  })

  val memSize = 128  // byte
  val mem = Mem(memSize / 4, UInt(32.W))
  val dut = Module(new ICache(7, 32, 6, 5, 0, addr => true.B, addr => false.B, false))

  val raddrReg = RegEnable(dut.io.master.araddr, dut.io.master.arvalid && dut.io.master.arready)

  dut.io.in <> io.req
  dut.io.out.ready := true.B
  dut.io.master <> AXI4.none
  dut.io.master.arready := true.B
  dut.io.master.rvalid := true.B
  dut.io.master.rdata := mem(raddrReg)
  dut.io.master.rresp := "b00".U

  val dutData = dut.io.out.bits.rdata
  val refData = RegEnable(mem(io.req.bits.raddr), io.req.fire)
  when (dut.io.out.fire) {
    assert(dutData === refData)
  }
}

class ICacheFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("ICache")
  it should "pass" in {
    verify(new ICacheTest, Seq(BoundedCheck(6), BtormcEngineAnnotation))
  }
}
