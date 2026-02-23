################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../System/Modules/BasicAES/baes_ccm.c \
../System/Modules/BasicAES/baes_cmac.c \
../System/Modules/BasicAES/baes_ecb.c 

C_DEPS += \
./System/Modules/BasicAES/baes_ccm.d \
./System/Modules/BasicAES/baes_cmac.d \
./System/Modules/BasicAES/baes_ecb.d 

OBJS += \
./System/Modules/BasicAES/baes_ccm.o \
./System/Modules/BasicAES/baes_cmac.o \
./System/Modules/BasicAES/baes_ecb.o 


# Each subdirectory must supply rules for building sources it contributes
System/Modules/BasicAES/%.o System/Modules/BasicAES/%.su System/Modules/BasicAES/%.cyclo: ../System/Modules/BasicAES/%.c System/Modules/BasicAES/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu11 -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../System/Modules/BasicAES -I../System/Modules/Flash -I../System/Modules/MemoryManager -I../System/Modules/Nvm -I../System/Modules/RTDebug -I../System/Modules/SerialCmdInterpreter -I../System/Config/Flash -I../System/Config/CRC_Ctrl -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-System-2f-Modules-2f-BasicAES

clean-System-2f-Modules-2f-BasicAES:
	-$(RM) ./System/Modules/BasicAES/baes_ccm.cyclo ./System/Modules/BasicAES/baes_ccm.d ./System/Modules/BasicAES/baes_ccm.o ./System/Modules/BasicAES/baes_ccm.su ./System/Modules/BasicAES/baes_cmac.cyclo ./System/Modules/BasicAES/baes_cmac.d ./System/Modules/BasicAES/baes_cmac.o ./System/Modules/BasicAES/baes_cmac.su ./System/Modules/BasicAES/baes_ecb.cyclo ./System/Modules/BasicAES/baes_ecb.d ./System/Modules/BasicAES/baes_ecb.o ./System/Modules/BasicAES/baes_ecb.su

.PHONY: clean-System-2f-Modules-2f-BasicAES

