#!/bin/bash

HEXFILE=/home/ed/work/github/BluZ/stm32cubeide/BluZ_LPM/Release/BluZ_LPM.elf

PGM=/opt/st/stm32cubeide_1.17.0/plugins/com.st.stm32cube.ide.mcu.externaltools.cubeprogrammer.linux64_2.2.0.202409170845/tools/bin/STM32_Programmer_CLI

$PGM -c port=SWD reset=HWrst ap=0 -d $HEXFILE -HardRst
