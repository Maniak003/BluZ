################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../Projects/Common/WPAN/Modules/RTDebug/RTDebug.c \
../Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.c 

C_DEPS += \
./Projects/Common/WPAN/Modules/RTDebug/RTDebug.d \
./Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.d 

OBJS += \
./Projects/Common/WPAN/Modules/RTDebug/RTDebug.o \
./Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.o 


# Each subdirectory must supply rules for building sources it contributes
Projects/Common/WPAN/Modules/RTDebug/%.o Projects/Common/WPAN/Modules/RTDebug/%.su Projects/Common/WPAN/Modules/RTDebug/%.cyclo: ../Projects/Common/WPAN/Modules/RTDebug/%.c Projects/Common/WPAN/Modules/RTDebug/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu11 -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../Projects/Common/WPAN/Interfaces -I../Projects/Common/WPAN/Modules -I../Projects/Common/WPAN/Modules/BasicAES -I../Projects/Common/WPAN/Modules/MemoryManager -I../Projects/Common/WPAN/Modules/Nvm -I../Projects/Common/WPAN/Modules/RTDebug -I../Projects/Common/WPAN/Modules/SerialCmdInterpreter -I../Projects/Common/WPAN/Modules/Log -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-Projects-2f-Common-2f-WPAN-2f-Modules-2f-RTDebug

clean-Projects-2f-Common-2f-WPAN-2f-Modules-2f-RTDebug:
	-$(RM) ./Projects/Common/WPAN/Modules/RTDebug/RTDebug.cyclo ./Projects/Common/WPAN/Modules/RTDebug/RTDebug.d ./Projects/Common/WPAN/Modules/RTDebug/RTDebug.o ./Projects/Common/WPAN/Modules/RTDebug/RTDebug.su ./Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.cyclo ./Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.d ./Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.o ./Projects/Common/WPAN/Modules/RTDebug/RTDebug_dtb.su

.PHONY: clean-Projects-2f-Common-2f-WPAN-2f-Modules-2f-RTDebug

