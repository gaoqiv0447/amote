################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../tools/nusensors/nusensors.cpp 

OBJS += \
./tools/nusensors/nusensors.o 

CPP_DEPS += \
./tools/nusensors/nusensors.d 


# Each subdirectory must supply rules for building sources it contributes
tools/nusensors/%.o: ../tools/nusensors/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: Cross G++ Compiler'
	g++ -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


