################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../Core/Src/LTC1662.c \
../Core/Src/app_entry.c \
../Core/Src/main.c \
../Core/Src/rw_FLASH.c \
../Core/Src/stm32wbaxx_hal_msp.c \
../Core/Src/stm32wbaxx_it.c \
../Core/Src/syscalls.c \
../Core/Src/sysmem.c \
../Core/Src/system_stm32wbaxx.c 

C_DEPS += \
./Core/Src/LTC1662.d \
./Core/Src/app_entry.d \
./Core/Src/main.d \
./Core/Src/rw_FLASH.d \
./Core/Src/stm32wbaxx_hal_msp.d \
./Core/Src/stm32wbaxx_it.d \
./Core/Src/syscalls.d \
./Core/Src/sysmem.d \
./Core/Src/system_stm32wbaxx.d 

OBJS += \
./Core/Src/LTC1662.o \
./Core/Src/app_entry.o \
./Core/Src/main.o \
./Core/Src/rw_FLASH.o \
./Core/Src/stm32wbaxx_hal_msp.o \
./Core/Src/stm32wbaxx_it.o \
./Core/Src/syscalls.o \
./Core/Src/sysmem.o \
./Core/Src/system_stm32wbaxx.o 


# Each subdirectory must supply rules for building sources it contributes
Core/Src/%.o Core/Src/%.su Core/Src/%.cyclo: ../Core/Src/%.c Core/Src/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m33 -std=gnu11 -DUSE_HAL_DRIVER -DSTM32WBA54xx -DUSE_FULL_LL_DRIVER -DBLE -c -I../Core/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc -I../Drivers/STM32WBAxx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32WBAxx/Include -I../Drivers/CMSIS/Include -I../System/Interfaces -I../System/Modules -I../System/Modules/BasicAES -I../System/Modules/Flash -I../System/Modules/MemoryManager -I../System/Modules/Nvm -I../System/Modules/RTDebug -I../System/Modules/SerialCmdInterpreter -I../System/Config/Log -I../System/Config/LowPower -I../System/Config/Debug_GPIO -I../System/Config/Flash -I../System/Config/CRC_Ctrl -I../STM32_WPAN/App -I../STM32_WPAN/Target -I../Utilities/trace/adv_trace -I../Utilities/misc -I../Utilities/sequencer -I../Utilities/tim_serv -I../Utilities/lpm/tiny_lpm -I../Middlewares/ST/STM32_WPAN -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/config/ble_basic -I../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/_40nm_reg_files -I../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/inc/ot_inc -I../Middlewares/ST/STM32_WPAN/link_layer/ll_sys/inc -I../Middlewares/ST/STM32_WPAN/ble -I../Middlewares/ST/STM32_WPAN/ble/stack/include -I../Middlewares/ST/STM32_WPAN/ble/stack/include/auto -I../Middlewares/ST/STM32_WPAN/ble/svc/Inc -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -fcyclomatic-complexity -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-Core-2f-Src

clean-Core-2f-Src:
	-$(RM) ./Core/Src/LTC1662.cyclo ./Core/Src/LTC1662.d ./Core/Src/LTC1662.o ./Core/Src/LTC1662.su ./Core/Src/app_entry.cyclo ./Core/Src/app_entry.d ./Core/Src/app_entry.o ./Core/Src/app_entry.su ./Core/Src/main.cyclo ./Core/Src/main.d ./Core/Src/main.o ./Core/Src/main.su ./Core/Src/rw_FLASH.cyclo ./Core/Src/rw_FLASH.d ./Core/Src/rw_FLASH.o ./Core/Src/rw_FLASH.su ./Core/Src/stm32wbaxx_hal_msp.cyclo ./Core/Src/stm32wbaxx_hal_msp.d ./Core/Src/stm32wbaxx_hal_msp.o ./Core/Src/stm32wbaxx_hal_msp.su ./Core/Src/stm32wbaxx_it.cyclo ./Core/Src/stm32wbaxx_it.d ./Core/Src/stm32wbaxx_it.o ./Core/Src/stm32wbaxx_it.su ./Core/Src/syscalls.cyclo ./Core/Src/syscalls.d ./Core/Src/syscalls.o ./Core/Src/syscalls.su ./Core/Src/sysmem.cyclo ./Core/Src/sysmem.d ./Core/Src/sysmem.o ./Core/Src/sysmem.su ./Core/Src/system_stm32wbaxx.cyclo ./Core/Src/system_stm32wbaxx.d ./Core/Src/system_stm32wbaxx.o ./Core/Src/system_stm32wbaxx.su

.PHONY: clean-Core-2f-Src

