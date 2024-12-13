# 2024-12-10T18:21:39.958377
import vitis

client = vitis.create_client()
client.set_workspace(path="fpga.vitis")

platform = client.create_platform_component(name = "rvcpu",hw_design = "$COMPONENT_LOCATION/../top.xsa",os = "standalone",cpu = "ps7_cortexa9_0",domain_name = "standalone_ps7_cortexa9_0")

comp = client.create_app_component(name="rvcpu_boot",platform = "$COMPONENT_LOCATION/../rvcpu/export/rvcpu/rvcpu.xpfm",domain = "standalone_ps7_cortexa9_0",template = "hello_world")

platform = client.get_component(name="rvcpu")
status = platform.build()

status = platform.build()

comp = client.get_component(name="rvcpu_boot")
comp.build()

status = platform.build()

comp.build()

vitis.dispose()

