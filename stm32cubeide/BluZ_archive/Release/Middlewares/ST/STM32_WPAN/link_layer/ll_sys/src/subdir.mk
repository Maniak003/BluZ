################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.c \
../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.c \
../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.c \
../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.c 

OBJS += \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.o \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.o \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.o \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.o 

C_DEPS += \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.d \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.d \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.d \
./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.d 


# Each subdirectory must supply rules for building sources it contributes
Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/%.o Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/%.su Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/%.cyclo: ../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/%.c Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu18 -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Modules/baes -I../System/Modules/Flash -I../System/Modules/MemoryManager -I../System/Modules/Nvm -I../System/Modules/RTDebug -I../System/Modules/RFControl -I../System/Modules/SerialCmdInterpreter -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../System/Config/Flash -I../System/Config/CRC_Ctrl -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/porting -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-Middlewares-2f-ST-2f-STM32_WPAN-2f-link_layer-2f-ll_sys-2f-src

clean-Middlewares-2f-ST-2f-STM32_WPAN-2f-link_layer-2f-ll_sys-2f-src:
	-$(RM) ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.cyclo ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.d ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.o ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_cs.su ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.cyclo ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.d ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.o ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_dp_slp.su ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.cyclo ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.d ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.o ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_intf.su ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.cyclo ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.d ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.o ./Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/ll_sys_startup.su

.PHONY: clean-Middlewares-2f-ST-2f-STM32_WPAN-2f-link_layer-2f-ll_sys-2f-src

