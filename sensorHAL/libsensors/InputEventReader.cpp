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

#include <stdint.h>
#include <errno.h>
#include <unistd.h>
#include <poll.h>

#include <sys/cdefs.h>
#include <sys/types.h>

#include <linux/input.h>

#include <cutils/log.h>

#include "InputEventReader.h"

/*****************************************************************************/

InputEventCircularReader::InputEventCircularReader(size_t numEvents) :
		mBuffer(new RecSensorData[numEvents * 2]), mBufferEnd(mBuffer + numEvents),
		mHead(mBuffer),
		mCurr(mBuffer),
		mFreeSpace(numEvents),
		mUdpSocket(SocketUtils::getInstance())
		{

		}

InputEventCircularReader::~InputEventCircularReader() {
	delete[] mBuffer;
}

/*
 * fill the socket fd
 */
ssize_t InputEventCircularReader::fill(int type) {
	size_t numEventsRead = 0;
	if (mFreeSpace) {

		const ssize_t nread = mUdpSocket.readDataFromUdp(type, mHead,
				mFreeSpace * sizeof(RecSensorData));
		if (nread < 0 || nread % sizeof(RecSensorData)) {
			// we got a partial event!!
			return nread < 0 ? -errno : -EINVAL;
		}

		numEventsRead = nread / sizeof(RecSensorData);
		if (numEventsRead) {
			mHead += numEventsRead;
			mFreeSpace -= numEventsRead;
			if (mHead > mBufferEnd) {
				size_t s = mHead - mBufferEnd;
				memcpy(mBuffer, mBufferEnd, s * sizeof(RecSensorData));
				mHead = mBuffer + s;
			}
		}
	}

	return numEventsRead;

}

ssize_t InputEventCircularReader::readEvent(const RecSensorData ** events) {
	*events = mCurr;
	ssize_t available = (mBufferEnd - mBuffer) - mFreeSpace;
	return available ? 1 : 0;
}

void InputEventCircularReader::next() {
	mCurr++;
	mFreeSpace++;
	if (mCurr >= mBufferEnd) {
		mCurr = mBuffer;
	}
}

ssize_t InputEventCircularReader::getListFromTcp(const sensor_st ** list){
	return 0;
}
