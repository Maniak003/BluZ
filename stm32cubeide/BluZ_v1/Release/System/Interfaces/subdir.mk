################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../System/Interfaces/hw_aes.c \
../System/Interfaces/hw_otp.c \
../System/Interfaces/hw_pka.c \
../System/Interfaces/hw_pka_p256.c \
../System/Interfaces/hw_rng.c \
../System/Interfaces/stm32_lpm_if.c \
../System/Interfaces/timer_if.c \
../System/Interfaces/usart_if.c 

OBJS += \
./System/Interfaces/hw_aes.o \
./System/Interfaces/hw_otp.o \
./System/Interfaces/hw_pka.o \
./System/Interfaces/hw_pka_p256.o \
./System/Interfaces/hw_rng.o \
./System/Interfaces/stm32_lpm_if.o \
./System/Interfaces/timer_if.o \
./System/Interfaces/usart_if.o 

C_DEPS += \
./System/Interfaces/hw_aes.d \
./System/Interfaces/hw_otp.d \
./System/Interfaces/hw_pka.d \
./System/Interfaces/hw_pka_p256.d \
./System/Interfaces/hw_rng.d \
./System/Interfaces/stm32_lpm_if.d \
./System/Interfaces/timer_if.d \
./System/Interfaces/usart_if.d 


# Each subdirectory must supply rules for building sources it contributes
System/Interfaces/%.o System/Interfaces/%.su System/Interfaces/%.cyclo: ../System/Interfaces/%.c System/Interfaces/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu18 -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Modules/Flash -I../System/Modules/MemoryManager -I../System/Modules/Nvm -I../System/Modules/RTDebug -I../System/Modules/SerialCmdInterpreter -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../System/Config/Flash -I../System/Config/CRC_Ctrl -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../System/Modules/BasicAES -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-System-2f-Interfaces

clean-System-2f-Interfaces:
	-$(RM) ./System/Interfaces/hw_aes.cyclo ./System/Interfaces/hw_aes.d ./System/Interfaces/hw_aes.o ./System/Interfaces/hw_aes.su ./System/Interfaces/hw_otp.cyclo ./System/Interfaces/hw_otp.d ./System/Interfaces/hw_otp.o ./System/Interfaces/hw_otp.su ./System/Interfaces/hw_pka.cyclo ./System/Interfaces/hw_pka.d ./System/Interfaces/hw_pka.o ./System/Interfaces/hw_pka.su ./System/Interfaces/hw_pka_p256.cyclo ./System/Interfaces/hw_pka_p256.d ./System/Interfaces/hw_pka_p256.o ./System/Interfaces/hw_pka_p256.su ./System/Interfaces/hw_rng.cyclo ./System/Interfaces/hw_rng.d ./System/Interfaces/hw_rng.o ./System/Interfaces/hw_rng.su ./System/Interfaces/stm32_lpm_if.cyclo ./System/Interfaces/stm32_lpm_if.d ./System/Interfaces/stm32_lpm_if.o ./System/Interfaces/stm32_lpm_if.su ./System/Interfaces/timer_if.cyclo ./System/Interfaces/timer_if.d ./System/Interfaces/timer_if.o ./System/Interfaces/timer_if.su ./System/Interfaces/usart_if.cyclo ./System/Interfaces/usart_if.d ./System/Interfaces/usart_if.o ./System/Interfaces/usart_if.su

.PHONY: clean-System-2f-Interfaces

