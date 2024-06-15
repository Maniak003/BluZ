#!/bin/bash

PGM=/opt/st/stm32cubeide_1.15.0/plugins/com.st.stm32cube.ide.mcu.externaltools.cubeprogrammer.linux64_2.1.200.202311302303/tools/bin/STM32_Programmer_CLI

#$PGM -c port=SWD reset=HWrst ap=0 -d ~/work/gitprojects/BluZ/stm32cubeide/BluZ_v1/Debug/BluZ_v1.hex -HardRst
$PGM -c port=SWD reset=HWrst ap=0 -d ~/work/gitprojects/BluZ/stm32cubeide/BluZ_v1/Release/BluZ_v1.hex -HardRst
