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

#include <fcntl.h>
#include <errno.h>
#include <math.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/select.h>
#include <cutils/log.h>

#include "GeneralSensor.h"

/*****************************************************************************/

GeneralSensor::GeneralSensor() :
		SensorBase("general", 0), mEnabled(0),mPendingMask(0), mInputReader(10) , mUdpSocket(SocketUtils::getInstance())

{
	memset(mPendingEvents, 0, sizeof(mPendingEvents));
	mPendingEvents[Accelerometer].version = sizeof(sensors_event_t);
	mPendingEvents[Accelerometer].sensor = ID_A;
	mPendingEvents[Accelerometer].type = SENSOR_TYPE_ACCELEROMETER;
	mPendingEvents[Accelerometer].acceleration.status =
			SENSOR_STATUS_ACCURACY_HIGH;

	mPendingEvents[MagneticField].version = sizeof(sensors_event_t);
	mPendingEvents[MagneticField].sensor = ID_M;
	mPendingEvents[MagneticField].type = SENSOR_TYPE_MAGNETIC_FIELD;
	mPendingEvents[MagneticField].magnetic.status = SENSOR_STATUS_ACCURACY_HIGH;

	mPendingEvents[Orientation].version = sizeof(sensors_event_t);
	mPendingEvents[Orientation].sensor = ID_O;
	mPendingEvents[Orientation].type = SENSOR_TYPE_ORIENTATION;
	mPendingEvents[Orientation].orientation.status =
			SENSOR_STATUS_ACCURACY_HIGH;

	mPendingEvents[Gyroscope].version = sizeof(sensors_event_t);
	mPendingEvents[Gyroscope].sensor = ID_O;
	mPendingEvents[Gyroscope].type = SENSOR_TYPE_GYROSCOPE;
	mPendingEvents[Gyroscope].gyro.status =
			SENSOR_STATUS_ACCURACY_HIGH;


	mPendingEvents[Proximity].version = sizeof(sensors_event_t);
	mPendingEvents[Proximity].sensor = ID_PX;
	mPendingEvents[Proximity].type = SENSOR_TYPE_PROXIMITY;

	mPendingEvents[Light].version = sizeof(sensors_event_t);
	mPendingEvents[Light].sensor = ID_L;
	mPendingEvents[Light].type = SENSOR_TYPE_LIGHT;

	mPendingEvents[Temperature].version = sizeof(sensors_event_t);
	mPendingEvents[Temperature].sensor = ID_T;
	mPendingEvents[Temperature].type = SENSOR_TYPE_TEMPERATURE;

	mPendingEvents[Pressure].version = sizeof(sensors_event_t);
	mPendingEvents[Pressure].sensor = ID_PR;
	mPendingEvents[Pressure].type = SENSOR_TYPE_PRESSURE;

	for (int i = 0; i < numSensors; i++) {
		mDelays[i] = 200000000; // 200 ms by default
	}

}

GeneralSensor::~GeneralSensor() {
}

int GeneralSensor::enable(int32_t handle, int en) {
	//LOGE("######AkmSensor::enable() handle=%d en=%d", handle, en);

	if (handle >= numSensors)
		return -EINVAL;

	uint32_t newState = en ? 1 : 0;
	if(!mUdpSocket.getUdpstate() && en){
		mUdpSocket.addEnableList(handle);
		return 0;
	}

	if ((uint32_t(newState) << handle) != (mEnabled & (1 << handle)) && mUdpSocket.getUdpstate()) {
		mEnabled &= ~(1 << handle);
		mEnabled |= newState << handle;
		//update_delay();
		LOGE("AkmSensor::socket state udp_state=%d tcp_state= %d",mUdpSocket.getUdpstate(),mUdpSocket.getTcpstate());
		if (mUdpSocket.getUdpstate() && mUdpSocket.getTcpstate()) {
			mUdpSocket.enableSensor(handle, en);
		} else {
			LOGE("AkmSensor::enable socket is not connected!");

		}
	}
	return 0;
}

int GeneralSensor::setDelay(int32_t handle, int64_t ns) {
#ifdef ECS_IOCTL_APP_SET_DELAY
	if (handle >= numSensors)
	return -EINVAL;

	if (ns < 0)
	return -EINVAL;

	mDelays[what] = ns;
	return update_delay();
#else
	return -1;
#endif
}

int GeneralSensor::update_delay() {
	//LOGE("######AkmSensor::update_delay() ");
	if (mEnabled) {
		uint64_t wanted = -1LLU;
		for (int i = 0; i < numSensors; i++) {
			if (mEnabled & (1 << i)) {
				uint64_t ns = mDelays[i];
				wanted = wanted < ns ? wanted : ns;
			}
		}
		short delay = int64_t(wanted) / 1000000;
		//weidawei add
		usleep(delay);
	}
	return 0;
}

int GeneralSensor::readEvents(sensors_event_t* data, int count) {

	//LOGE("######AkmSensor::readEvents() begin ");
	if (count < 1)
		return -EINVAL;

	ssize_t n = mInputReader.fill(sensor_type);
	if (n < 0)
		return n;

	int numEventReceived = 0;
	const RecSensorData * event;

	while (count && mInputReader.readEvent(&event)) {
		processEvent(event->type, event->timestamp, event->values);
		//LOGE("######AkmSensor::read one event mPendingMask = %d",mPendingMask);
		for (int j = 0; count && mPendingMask && j < numSensors; j++) {
			if (mPendingMask & (1 << j)) {
				mPendingMask &= ~(1 << j);
				if (mEnabled & (1 << j)) {
					*data++ = mPendingEvents[j];
					count--;
					numEventReceived++;
				}
			}
		}
		mInputReader.next();
	}

	//copy from sensors_qemud.c we may end-up in a busy loop, slow things down, just in case.
	usleep(100000);

	return numEventReceived;
}

void GeneralSensor::processEvent(int type, long time, const float value[3]) {
	switch (type) {
	case ID_A:
		mPendingMask |= 1 << Accelerometer;
		mPendingEvents[Accelerometer].timestamp = time;
		mPendingEvents[Accelerometer].acceleration.x = value[0];
		mPendingEvents[Accelerometer].acceleration.y = value[1];
		mPendingEvents[Accelerometer].acceleration.z = value[2];

		break;
	case ID_M:
		mPendingMask |= 1 << MagneticField;
		mPendingEvents[MagneticField].timestamp = time;
		mPendingEvents[MagneticField].magnetic.x = value[0];
		mPendingEvents[MagneticField].magnetic.y = value[1];
		mPendingEvents[MagneticField].magnetic.z = value[2];

		break;
	case ID_O:
		mPendingMask |= 1 << Orientation;
		mPendingEvents[Orientation].timestamp = time;
        mPendingEvents[Orientation].orientation.azimuth = value[0];
        mPendingEvents[Orientation].orientation.pitch = value[1] ;
        mPendingEvents[Orientation].orientation.roll = value[2];

		break;
	case ID_G:
		mPendingMask |= 1 << Gyroscope;
		mPendingEvents[Gyroscope].timestamp = time;
		mPendingEvents[Gyroscope].gyro.x = value[0];
		mPendingEvents[Gyroscope].gyro.y = value[1];
		mPendingEvents[Gyroscope].gyro.z = value[2];

		break;

	case ID_L:
		mPendingMask |= 1 << Light;
		mPendingEvents[Light].timestamp = time;
		mPendingEvents[Light].light = value[0];
		break;

	case ID_T:
		mPendingMask |= 1 << Temperature;
		mPendingEvents[Temperature].timestamp = time;
		mPendingEvents[Temperature].temperature = value[0];

		break;

	case ID_PR:
		mPendingMask |= 1 << Pressure;
		mPendingEvents[Pressure].timestamp = time;
		mPendingEvents[Pressure].pressure = value[0];

		break;

	case ID_PX:
		mPendingMask |= 1 << Proximity;
		mPendingEvents[Proximity].timestamp = time;
		mPendingEvents[Proximity].distance = value[0];

		break;
	}

}
