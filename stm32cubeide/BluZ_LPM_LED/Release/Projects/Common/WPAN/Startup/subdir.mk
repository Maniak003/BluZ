################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
S_SRCS += \
../Projects/Common/WPAN/Startup/stm32wbaxx_ResetHandler_GCC.s 

S_DEPS += \
./Projects/Common/WPAN/Startup/stm32wbaxx_ResetHandler_GCC.d 

OBJS += \
./Projects/Common/WPAN/Startup/stm32wbaxx_ResetHandler_GCC.o 


# Each subdirectory must supply rules for building sources it contributes
Projects/Common/WPAN/Startup/%.o: ../Projects/Common/WPAN/Startup/%.s Projects/Common/WPAN/Startup/subdir.mk
	arm-none-eabi-gcc -mcpu=cortex-m33 -c -x assembler-with-cpp -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -o "$@" "$<"

clean: clean-Projects-2f-Common-2f-WPAN-2f-Startup

clean-Projects-2f-Common-2f-WPAN-2f-Startup:
	-$(RM) ./Projects/Common/WPAN/Startup/stm32wbaxx_ResetHandler_GCC.d ./Projects/Common/WPAN/Startup/stm32wbaxx_ResetHandler_GCC.o

.PHONY: clean-Projects-2f-Common-2f-WPAN-2f-Startup

