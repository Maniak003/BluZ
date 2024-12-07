################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
S_UPPER_SRCS += \
../System/Startup/stm32wbaxx_ResetHandler.S 

OBJS += \
./System/Startup/stm32wbaxx_ResetHandler.o 

S_UPPER_DEPS += \
./System/Startup/stm32wbaxx_ResetHandler.d 


# Each subdirectory must supply rules for building sources it contributes
System/Startup/%.o: ../System/Startup/%.S System/Startup/subdir.mk
	arm-none-eabi-gcc -mcpu=cortex-m33 -c -x assembler-with-cpp -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@" "$<"

clean: clean-System-2f-Startup

clean-System-2f-Startup:
	-$(RM) ./System/Startup/stm32wbaxx_ResetHandler.d ./System/Startup/stm32wbaxx_ResetHandler.o

.PHONY: clean-System-2f-Startup

