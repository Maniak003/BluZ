################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../System/Modules/app_sys.c \
../System/Modules/ble_timer.c \
../System/Modules/crc_ctrl.c \
../System/Modules/otp.c \
../System/Modules/scm.c \
../System/Modules/stm_list.c 

OBJS += \
./System/Modules/app_sys.o \
./System/Modules/ble_timer.o \
./System/Modules/crc_ctrl.o \
./System/Modules/otp.o \
./System/Modules/scm.o \
./System/Modules/stm_list.o 

C_DEPS += \
./System/Modules/app_sys.d \
./System/Modules/ble_timer.d \
./System/Modules/crc_ctrl.d \
./System/Modules/otp.d \
./System/Modules/scm.d \
./System/Modules/stm_list.d 


# Each subdirectory must supply rules for building sources it contributes
System/Modules/%.o System/Modules/%.su System/Modules/%.cyclo: ../System/Modules/%.c System/Modules/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu18 -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Modules/Flash -I../System/Modules/MemoryManager -I../System/Modules/Nvm -I../System/Modules/RTDebug -I../System/Modules/SerialCmdInterpreter -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../System/Config/Flash -I../System/Config/CRC_Ctrl -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../System/Modules/BasicAES -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-System-2f-Modules

clean-System-2f-Modules:
	-$(RM) ./System/Modules/app_sys.cyclo ./System/Modules/app_sys.d ./System/Modules/app_sys.o ./System/Modules/app_sys.su ./System/Modules/ble_timer.cyclo ./System/Modules/ble_timer.d ./System/Modules/ble_timer.o ./System/Modules/ble_timer.su ./System/Modules/crc_ctrl.cyclo ./System/Modules/crc_ctrl.d ./System/Modules/crc_ctrl.o ./System/Modules/crc_ctrl.su ./System/Modules/otp.cyclo ./System/Modules/otp.d ./System/Modules/otp.o ./System/Modules/otp.su ./System/Modules/scm.cyclo ./System/Modules/scm.d ./System/Modules/scm.o ./System/Modules/scm.su ./System/Modules/stm_list.cyclo ./System/Modules/stm_list.d ./System/Modules/stm_list.o ./System/Modules/stm_list.su

.PHONY: clean-System-2f-Modules

