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

#include <hardware/sensors.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <math.h>

#include <linux/input.h>

#include <cutils/atomic.h>
#include <cutils/log.h>

#include "nusensors.h"
#include "GeneralSensor.h"
#include "SocketUtils.h"

/*****************************************************************************/

struct sensors_poll_context_t {
    struct sensors_poll_device_t device; // must be first

        sensors_poll_context_t();
        ~sensors_poll_context_t();
    int activate(int handle, int enabled);
    int setDelay(int handle, int64_t ns);
    int pollEvents(sensors_event_t* data, int count);

private:
    enum {
    	akm             = 0,
    };
    SensorBase* mSensors;

    int handleToDriver(int handle) const {
    	if(handle >= ID_A && handle <= ID_PX)
                return akm;

        return -EINVAL;
    }
};

/*****************************************************************************/

sensors_poll_context_t::sensors_poll_context_t()
{
    LOGE("######sensors_poll_context_t::sensors_poll_context_t()");

    mSensors = new GeneralSensor();

    //1.init udp socket(weidawei)
    SocketUtils::getInstance();

}

sensors_poll_context_t::~sensors_poll_context_t() {
   // LOGE("######sensors_poll_context_t::~sensors_poll_context_t()");
        delete mSensors;
}

int sensors_poll_context_t::activate(int handle, int enabled) {
  //  LOGE("######sensors_poll_context_t::activate");
    int index = handleToDriver(handle);
    if (index < 0) return index;
    int err =  mSensors->enable(handle, enabled);
    if (enabled && !err) {

    }
    return err;
}

int sensors_poll_context_t::setDelay(int handle, int64_t ns) {
  //  LOGE("######sensors_poll_context_t::setDelay");
    int index = handleToDriver(handle);
    if (index < 0) return index;
    return mSensors->setDelay(handle, ns);
}

int sensors_poll_context_t::pollEvents(sensors_event_t* data, int count)
{
	//LOGE("######sensors_poll_context_t::pollEvents  count = %d",count);

    int nbEvents = 0;
    int n = 0;
    //wait for udp data "ok" and create Tcp client
    SocketUtils& mUdpSocket = SocketUtils::getInstance();
    if(mUdpSocket.waitClientOk()== 0){
    	mUdpSocket.createTcpClient();
    }
    do {

            SensorBase* const sensor(mSensors);
            if (mUdpSocket.getUdpstate()) {
                int nb = sensor->readEvents(data, count);
               // LOGE("######sensors_poll_context_t::pollEvents  nb = %d",nb);
                if (nb < count) {
                 //TODO:
                }
                count -= nb;
                nbEvents += nb;
                data += nb;
            }

        if (count) {
            // we still have some room, so try to see if we can get
            // some events immediately or just wait if we don't have
            // anything to return
           //TODO:
        }
        // if we have events and space, go read them
    } while (n && count);

    return nbEvents;
}

/*****************************************************************************/

static int poll__close(struct hw_device_t *dev)
{
   // LOGE("######poll__close ");
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    if (ctx) {
        delete ctx;
    }
    return 0;
}

static int poll__activate(struct sensors_poll_device_t *dev,
        int handle, int enabled) {
   //LOGE("#######poll__activate ");
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->activate(handle, enabled);
}

static int poll__setDelay(struct sensors_poll_device_t *dev,
        int handle, int64_t ns) {
    //LOGE("#######poll__setDelay ");
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->setDelay(handle, ns);
}

static int poll__poll(struct sensors_poll_device_t *dev,
        sensors_event_t* data, int count) {
   // LOGE("####### poll__poll  ");
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->pollEvents(data, count);
}

/*****************************************************************************/

int init_nusensors(hw_module_t const* module, hw_device_t** device)
{
   // LOGE("####### init_nusensors()  ");
    int status = -EINVAL;

    sensors_poll_context_t *dev = new sensors_poll_context_t();
    memset(&dev->device, 0, sizeof(sensors_poll_device_t));

    dev->device.common.tag = HARDWARE_DEVICE_TAG;
    dev->device.common.version  = 0;
    dev->device.common.module   = const_cast<hw_module_t*>(module);
    dev->device.common.close    = poll__close;
    dev->device.activate        = poll__activate;
    dev->device.setDelay        = poll__setDelay;
    dev->device.poll            = poll__poll;

    *device = &dev->device.common;
    status = 0;
    return status;
}
