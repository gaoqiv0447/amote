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

#ifndef ANDROID_INPUT_EVENT_READER_H
#define ANDROID_INPUT_EVENT_READER_H

#include <stdint.h>
#include <errno.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include "nusensors.h"
#include "common.h"
#include "SocketUtils.h"
/*****************************************************************************/
struct RecSensorData;

class InputEventCircularReader
{
    struct RecSensorData* const mBuffer;
    struct RecSensorData* const mBufferEnd;
    struct RecSensorData* mHead;
    struct RecSensorData* mCurr;
    ssize_t mFreeSpace;
    /*weidawei add*/
    SocketUtils& mUdpSocket;

public:
    InputEventCircularReader(size_t numEvents);
    ~InputEventCircularReader();
    ssize_t fill(int fd);
    ssize_t readEvent(const RecSensorData ** event);
    void next();
    ssize_t getListFromTcp(const sensor_st ** list);
};

/*****************************************************************************/

#endif  // ANDROID_INPUT_EVENT_READER_H
