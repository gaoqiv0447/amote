/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "nusensors.h"

#include <stdio.h>
#include <cutils/sockets.h>
#include <cutils/logd.h>
#include <cutils/sockets.h>
#include <cutils/properties.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <netdb.h>

#if 1
#define  D(...)  LOGD(__VA_ARGS__)
#else
#define  D(...)  ((void)0)
#endif

#define  E(...)  LOGE(__VA_ARGS__)

/*****************************************************************************/

/*
 * The SENSORS Module
 */

/*
 * the AK8973 has a 8-bit ADC but the firmware seems to average 16 samples,
 * or at least makes its calibration on 12-bits values. This increases the
 * resolution by 4 bits.
 */

/** MODULE REGISTRATION SUPPORT
 **
 ** This is required so that hardware/libhardware/hardware.c
 ** will dlopen() this library appropriately.
 **/

/*
 * the following is the list of all supported sensors.
 * this table is used to build sSensorList declared below
 * according to which hardware sensors are reported as
 * available from the emulator (see get_sensors_list below)
 *
 * note: numerical values for maxRange/resolution/power were
 *       taken from the reference AK8976A implementation
 * weidawei tips:
 * maybe we have to get the data from the phone????
 */


static const struct sensor_t sSensorListInit[] = {
        {
	     .name = "aisino 3-axis Accelerometer",
             .vendor = "aisino",
             .version =1,
             .handle= SENSORS_HANDLE_BASE+ID_A,
             .type = SENSOR_TYPE_ACCELEROMETER,
             .maxRange = 4.0f*9.81f,
             .resolution = (4.0f*9.81f)/256.0f,
             .power = 0.2f,
             .minDelay = 0,
             .reserved = { }
	},
	{
	           .name = "aisino 3-axis Magnetic field sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_M,
             .type = SENSOR_TYPE_MAGNETIC_FIELD,
             .maxRange = 2000.0f,
             .resolution = 1.0f/16.0f,
             .power = 6.8f,
             .minDelay = 0,
             .reserved = { }
	},
	{
	           .name = "aisino 3-axis gyroscope sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_G,
             .type = SENSOR_TYPE_GYROSCOPE,
             .maxRange = 36.0f,
             .resolution = 6.28f/360.0f,
             .power = 6.8f,
             .minDelay = 0,
             .reserved = { }
	},
	{
	     	 .name = "aisino Orientation sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_O,
             .type = SENSOR_TYPE_ORIENTATION,
             .maxRange = 360.0f,
             .resolution = 1.0f,
             .power = 7.0f,
             .minDelay = 0,
             .reserved = { }
	},

	{
	     	 .name = "aisino Proximity sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_PX,
             .type = SENSOR_TYPE_PROXIMITY,
             .maxRange = PROXIMITY_THRESHOLD_CM,
             .resolution = PROXIMITY_THRESHOLD_CM,
             .power = 0.5f,
             .minDelay = 0,
             .reserved = { }
	},

	{
	     	 .name = "aisino pressure sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_PR,
             .type = SENSOR_TYPE_PRESSURE,
             .maxRange = 5000.0f,
             .resolution = 1.0f,
             .power = 0.5f,
             .minDelay = 0,
             .reserved = { }
	},

	{
	     	 .name = "aisino temperature sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_T,
             .type = SENSOR_TYPE_TEMPERATURE,
             .maxRange = 500.0f,
             .resolution = 0.1f,
             .power = 0.5f,
             .minDelay = 0,
             .reserved = { }
	},

	{
	     		.name = "aisino Light sensor",
             .vendor = "aisino",
             .version =1,
             .handle = SENSORS_HANDLE_BASE+ID_L,
             .type = SENSOR_TYPE_LIGHT,
             .maxRange = 10240.0f,
             .resolution = 1.0f,
             .power = 0.5f,
             .minDelay = 0,
             .reserved = { }
	},
};

//static struct sensor_t  sSensorList[MAX_NUM_SENSORS];

static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device);

static int sensors__get_sensors_list(struct sensors_module_t* module,
        struct sensor_t const** list)
{
    D(" sensors.c ######## sensors__get_sensors_list");
    *list = sSensorListInit;
    return ARRAY_SIZE(sSensorListInit);
}

static struct hw_module_methods_t sensors_module_methods = {
    .open = open_sensors
};

const struct sensors_module_t HAL_MODULE_INFO_SYM = {
    .common = {
        .tag = HARDWARE_MODULE_TAG,
        .version_major = 1,
        .version_minor = 0,
        .id = SENSORS_HARDWARE_MODULE_ID,
        .name = "aisino Sensors Module",
        .author = "The Android Open Source Project",
        .methods = &sensors_module_methods,
    },
    .get_sensors_list = sensors__get_sensors_list
};

/*****************************************************************************/

static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device)
{
    return init_nusensors(module, device);
}

