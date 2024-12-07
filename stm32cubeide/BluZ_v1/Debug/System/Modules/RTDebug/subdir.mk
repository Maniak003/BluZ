################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../System/Modules/RTDebug/RTDebug.c \
../System/Modules/RTDebug/RTDebug_dtb.c 

OBJS += \
./System/Modules/RTDebug/RTDebug.o \
./System/Modules/RTDebug/RTDebug_dtb.o 

C_DEPS += \
./System/Modules/RTDebug/RTDebug.d \
./System/Modules/RTDebug/RTDebug_dtb.d 


# Each subdirectory must supply rules for building sources it contributes
System/Modules/RTDebug/%.o System/Modules/RTDebug/%.su System/Modules/RTDebug/%.cyclo: ../System/Modules/RTDebug/%.c System/Modules/RTDebug/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu11 -g3 -DDEBUG -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Modules/Flash -I../System/Modules/MemoryManager -I../System/Modules/Nvm -I../System/Modules/RTDebug -I../System/Modules/SerialCmdInterpreter -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../System/Config/Flash -I../System/Config/CRC_Ctrl -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../System/Modules/BasicAES -O0 -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-System-2f-Modules-2f-RTDebug

clean-System-2f-Modules-2f-RTDebug:
	-$(RM) ./System/Modules/RTDebug/RTDebug.cyclo ./System/Modules/RTDebug/RTDebug.d ./System/Modules/RTDebug/RTDebug.o ./System/Modules/RTDebug/RTDebug.su ./System/Modules/RTDebug/RTDebug_dtb.cyclo ./System/Modules/RTDebug/RTDebug_dtb.d ./System/Modules/RTDebug/RTDebug_dtb.o ./System/Modules/RTDebug/RTDebug_dtb.su

.PHONY: clean-System-2f-Modules-2f-RTDebug

