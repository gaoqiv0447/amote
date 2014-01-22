################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../libsensors/FileProcess.cpp \
../libsensors/GeneralSensor.cpp \
../libsensors/InputEventReader.cpp \
../libsensors/SensorBase.cpp \
../libsensors/SocketUtils.cpp \
../libsensors/nusensors.cpp 

C_SRCS += \
../libsensors/sensors.c 

OBJS += \
./libsensors/FileProcess.o \
./libsensors/GeneralSensor.o \
./libsensors/InputEventReader.o \
./libsensors/SensorBase.o \
./libsensors/SocketUtils.o \
./libsensors/nusensors.o \
./libsensors/sensors.o 

C_DEPS += \
./libsensors/sensors.d 

CPP_DEPS += \
./libsensors/FileProcess.d \
./libsensors/GeneralSensor.d \
./libsensors/InputEventReader.d \
./libsensors/SensorBase.d \
./libsensors/SocketUtils.d \
./libsensors/nusensors.d 


# Each subdirectory must supply rules for building sources it contributes
libsensors/%.o: ../libsensors/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: Cross G++ Compiler'
	g++ -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

libsensors/%.o: ../libsensors/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: Cross GCC Compiler'
	gcc -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


