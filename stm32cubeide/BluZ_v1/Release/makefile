################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (12.3.rel1)
################################################################################

-include ../makefile.init

RM := rm -rf

# All of the sources participating in the build are defined here
-include sources.mk
-include Utilities/trace/adv_trace/subdir.mk
-include Utilities/tim_serv/subdir.mk
-include Utilities/sequencer/subdir.mk
-include Utilities/misc/subdir.mk
-include Utilities/lpm/tiny_lpm/subdir.mk
-include System/Startup/subdir.mk
-include System/Modules/SerialCmdInterpreter/subdir.mk
-include System/Modules/RTDebug/subdir.mk
-include System/Modules/Nvm/subdir.mk
-include System/Modules/MemoryManager/subdir.mk
-include System/Modules/Flash/subdir.mk
-include System/Modules/BasicAES/subdir.mk
-include System/Modules/subdir.mk
-include System/Interfaces/subdir.mk
-include System/Config/LowPower/subdir.mk
-include System/Config/Log/subdir.mk
-include System/Config/Flash/subdir.mk
-include System/Config/Debug_GPIO/subdir.mk
-include System/Config/CRC_Ctrl/subdir.mk
-include STM32_WPAN/Target/subdir.mk
-include STM32_WPAN/App/subdir.mk
-include Middlewares/ST/STM32_WPAN/link_layer/ll_sys/src/subdir.mk
-include Middlewares/ST/STM32_WPAN/ble/svc/Src/subdir.mk
-include Drivers/STM32WBAxx_HAL_Driver/Src/subdir.mk
-include Core/Startup/subdir.mk
-include Core/Src/subdir.mk
-include objects.mk

ifneq ($(MAKECMDGOALS),clean)
ifneq ($(strip $(S_DEPS)),)
-include $(S_DEPS)
endif
ifneq ($(strip $(S_UPPER_DEPS)),)
-include $(S_UPPER_DEPS)
endif
ifneq ($(strip $(C_DEPS)),)
-include $(C_DEPS)
endif
endif

-include ../makefile.defs

OPTIONAL_TOOL_DEPS := \
$(wildcard ../makefile.defs) \
$(wildcard ../makefile.init) \
$(wildcard ../makefile.targets) \


BUILD_ARTIFACT_NAME := BluZ_v1
BUILD_ARTIFACT_EXTENSION := elf
BUILD_ARTIFACT_PREFIX :=
BUILD_ARTIFACT := $(BUILD_ARTIFACT_PREFIX)$(BUILD_ARTIFACT_NAME)$(if $(BUILD_ARTIFACT_EXTENSION),.$(BUILD_ARTIFACT_EXTENSION),)

# Add inputs and outputs from these tool invocations to the build variables 
EXECUTABLES += \
BluZ_v1.elf \

MAP_FILES += \
BluZ_v1.map \

SIZE_OUTPUT += \
default.size.stdout \

OBJDUMP_LIST += \
BluZ_v1.list \


# All Target
all:
	+@$(MAKE) --no-print-directory main-build && $(MAKE) --no-print-directory post-build

# Main-build Target
main-build: BluZ_v1.elf secondary-outputs

# Tool invocations
BluZ_v1.elf BluZ_v1.map: $(OBJS) $(USER_OBJS) /home/ed/work/github/BluZ/stm32cubeide/BluZ_v1/STM32WBA54KGUX_FLASH.ld makefile objects.list $(OPTIONAL_TOOL_DEPS)
	arm-none-eabi-gcc  -z noexecstack -o "BluZ_v1.elf" @"objects.list" $(USER_OBJS) -Wl,--start-group $(LIBS) -Wl,--end-group -mcpu=cortex-m33 -T"/home/ed/work/github/BluZ/stm32cubeide/BluZ_v1/STM32WBA54KGUX_FLASH.ld" --specs=nosys.specs -Wl,-Map="BluZ_v1.map" -Wl,--gc-sections -static -L../Middlewares/ST/STM32_WPAN/link_layer/ll_cmd_lib/lib -L../Middlewares/ST/STM32_WPAN/ble/stack/lib --specs=nano.specs -mfpu=fpv5-sp-d16 -mfloat-abi=hard -mthumb -u _printf_float -Wl,--start-group -lc -lm -Wl,--end-group
	@echo 'Finished building target: $@'
	@echo ' '

default.size.stdout: $(EXECUTABLES) makefile objects.list $(OPTIONAL_TOOL_DEPS)
	arm-none-eabi-size  $(EXECUTABLES)
	@echo 'Finished building: $@'
	@echo ' '

BluZ_v1.list: $(EXECUTABLES) makefile objects.list $(OPTIONAL_TOOL_DEPS)
	arm-none-eabi-objdump -h -S $(EXECUTABLES) > "BluZ_v1.list"
	@echo 'Finished building: $@'
	@echo ' '

# Other Targets
clean:
	-$(RM) BluZ_v1.elf BluZ_v1.list BluZ_v1.map default.size.stdout
	-@echo ' '

post-build:
	arm-none-eabi-objcopy -O ihex BluZ_v1.elf BluZ_v1.hex
	-@echo ' '

secondary-outputs: $(SIZE_OUTPUT) $(OBJDUMP_LIST)

fail-specified-linker-script-missing:
	@echo 'Error: Cannot find the specified linker script. Check the linker settings in the build configuration.'
	@exit 2

warn-no-linker-script-specified:
	@echo 'Warning: No linker script specified. Check the linker settings in the build configuration.'

.PHONY: all clean dependents main-build fail-specified-linker-script-missing warn-no-linker-script-specified post-build

-include ../makefile.targets
