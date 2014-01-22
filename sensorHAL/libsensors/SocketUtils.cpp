/*
 * SocketUtils.cpp
 *
 *  Created on: 2012-3-9
 *      Author: root
 * Copyright (C) 2009 Aisino Corporation Inc.
 *
 * No.18A, Xingshikou street, Haidian District,Beijing
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Aisino Corporation Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Aisino.
 */

#include "SocketUtils.h"

#include <stdlib.h>
#include <string.h>
#include <cutils/log.h>
#include <unistd.h>
#include <fcntl.h>

#if 1
#define  D(...)  LOGD(__VA_ARGS__)
#endif


SocketUtils::SocketUtils() :
		sockfd(-1),sockfd_t(-1),mEnabledList(0), isUdpConnected(false) {

	D("socket: SocketUtils init \n");
	//create udp server
	int namelen, adrlen;
	namelen = sizeof(UDP_SOCKET);
	memset(mDatas, 0, sizeof(mDatas));
	if ((sockfd = socket(AF_UNIX, SOCK_DGRAM, 0)) == -1) {
		D("socket:opening udp socket failed %s \n", strerror(errno));
	}

	unlink(UDP_SOCKET);
	so_server.sun_family = AF_UNIX;
	strcpy(so_server.sun_path, UDP_SOCKET);
	D("socket: path=%s\n", so_server.sun_path);

	adrlen = sizeof(so_server.sun_family) + strlen(so_server.sun_path);
	if (bind(sockfd, (struct sockaddr *) &so_server, adrlen) == -1) {
		D("socket:binding socket %s \n", strerror(errno));
	}
	D("socket:binding socket success \n");

}

ssize_t SocketUtils::readDataFromUdp(int type, RecSensorData* buf,
		size_t count) {

	int read_n;
	int numEventReceived = 0;
	char readbuf[DATA_LEN];

	if (sockfd < 0) {
		D("socket:read data socket create failed! \n");
		isUdpConnected = false;
		return 0;
	}

	while (count != 0) {
		read_n = recvfrom(sockfd, readbuf, DATA_LEN, 0, NULL, 0);
//		D("socket:read data len=%d ", read_n);


		if (read_n == DATA_LEN) {
			processData(readbuf, DATA_LEN);
//			D(
//					"socket:read data type=%d timestamp =%lld \n", mDatas[0].type, mDatas[0].timestamp);
//			D(
//					"socket:read data values %f %f %f", mDatas[0].values[0], mDatas[0].values[1], mDatas[0].values[2]);
			for (int i = 0; i < PACKET_LEN; i++) {
				*buf++ = mDatas[i];
				count--;
				numEventReceived++;
			}
			count = 0;
		}else{
			if (read_n > 0 && readbuf[0] == 'o' && readbuf[1] == 'k') {
					isUdpConnected = true;
					D("socket:read data 'o' 'k' at nonfirst time restart the tcp client");
					sockfd_t = -1;
					createTcpClient();
			}
			if(read_n <= 0){

			}
			return 0;
		}
	}
	return numEventReceived * sizeof(RecSensorData);
}

void SocketUtils::processData(char* data, int len) {

	for (int pl = 0; pl < PACKET_LEN; pl++) {
		int offset = pl * UDP_LEN;
		int i = 2;
		mDatas[pl].type = data[offset + 1];
		for (; i < 10; i++) {
			ml_t.val[i - 1] = data[offset + i];
		}
		mDatas[pl].timestamp = ml_t.l;

		for (; i < 14; i++) {
			mf_t.val[i - 10] = data[offset + i];
		}
		mDatas[pl].values[0] = mf_t.f;

		for (; i < 18; i++) {
			mf_t.val[i - 14] = data[offset + i];
		}
		mDatas[pl].values[1] = mf_t.f;

		for (; i < 22; i++) {
			mf_t.val[i - 18] = data[offset + i];
		}
		mDatas[pl].values[2] = mf_t.f;
	}

}

ssize_t revcListFromTcp(sensor_st* list){
	return 0;
}

ssize_t SocketUtils::waitClientOk() {
	if (sockfd < 0) {
		D("socket: wait ok socket create failed! \n");
		isUdpConnected = false;
		return -1;
	}
	if (isUdpConnected) {
		return -1;
	}
	int read_n;
	char readbuf[3];
	read_n = recvfrom(sockfd, readbuf, 3, 0, NULL, 0);
	//D("socket:read data len=%d data =%s\n", read_n, readbuf);
	if (read_n > 0 && readbuf[0] == 'o' && readbuf[1] == 'k') {
		isUdpConnected = true;

		return 0;
	} else {
		isUdpConnected = false;
	}

	return -1;
}
ssize_t SocketUtils::createTcpClient() {

	if(sockfd_t > 0 ){
		return 0;
	}

	int namelen = strlen(TCP_SOCKET);
	if (sockfd_t < 0) {
		sockfd_t = socket_local_client(TCP_SOCKET,
				ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
		int flags = fcntl(sockfd_t, F_GETFL, 0);
	}

	if (sockfd_t < 0) {
		D("no amote_tcp_socket: %s", strerror(errno));
		return -1;
	}

	for (int handle = ID_A; handle < MAX_NUM_SENSORS; handle++) {
		if (mEnabledList && 1 << handle) {
			//D("HAL amote_tcp_socket send enable msg on start..");
			enableSensor(handle,1);
		}
	}

	D("HAL amote_tcp_socket create successfully sockfd_t= %d",sockfd_t);
	D("HAL amote_tcp_socket start tcp thread.");
	startTcpThread();
	return 0;
}

ssize_t SocketUtils::sendDataToTcpServer(const void* buff, int len) {

	D(" tcp_socket: enter sendDataToTcpServer %s", buff);

	int len2;
	do {
		len2 = write(sockfd_t, buff, len);

	} while (len2 < 0 && errno == EINTR);
	//D(" tcp_socket: leave sendDataToTcpServer %s", buff);
	return len2;

}

ssize_t SocketUtils::recvDataFromTcp(void* buff, int  len)
{
	D(" *****tcp_socket: enter recvDataFromTcp recvfrom");
    int  len2;
    do {
        //len2 = read(sockfd_t, buff, len);
        len2 = recvfrom(sockfd_t, buff, len, 0, NULL, 0);
       // D(" *****recvfrom return len2 =%d error=%s",len2,strerror(errno));
        if(0== len2 && (errno == ECONNRESET || errno == 0)){
        	pthread_exit(NULL);
        }
    } while (len2 < 0 && errno == EINTR);
    return len2;
}

ssize_t SocketUtils::revcListFromTcp(sensor_st* list){

	return 0;
}

ssize_t SocketUtils::enableSensor(int handle, int en) {
	char buf[3];
	if (en) {
		snprintf(buf, sizeof buf, "e%d", handle);
	} else {
		snprintf(buf,  sizeof buf, "d%d", handle);
	}
	if (sendDataToTcpServer(buf, sizeof(buf)) != sizeof(buf)) {
		return -1;
	}
	return 0;
}

ssize_t SocketUtils::addEnableList(int handle) {
	mEnabledList |= 1 << handle;
	return 0;
}

bool SocketUtils::getUdpstate() const {
	return isUdpConnected;
}

bool SocketUtils::getTcpstate() const {
	return sockfd_t < 0 ? 0 : 1;
}

void SocketUtils::worker(){
	while (true) {
		char *path = (char*) malloc(PATH_MAX);
		memset(path, 0, PATH_MAX);
		int len = recvDataFromTcp(path, PATH_MAX);
		//D(" *****worker--recvDataFromTcp len= %d path=%s\n", len, path);

		int pathLength = strlen(path);
		if (0 == pathLength) {
			free(path);
			continue;
		}
		char* pathBuffer = (char *) malloc(PATH_MAX + 1);
		if (!pathBuffer) {
			free(path);
			break;
		}
		memset(pathBuffer, 0, PATH_MAX + 1);

		int pathRemaining = PATH_MAX - pathLength;
		strcpy(pathBuffer, path);
		free(path);

		if (pathLength > 0 && pathBuffer[pathLength - 1] != '/') {
			pathBuffer[pathLength] = '/';
			pathBuffer[pathLength + 1] = 0;
			--pathRemaining;
		}

		char * json = FileProcess::doProcessDirectory(pathBuffer,
				pathRemaining);

		free(pathBuffer);
		pathBuffer = NULL;
		//D(" json4 = %s strlen=\n",json, strlen(json));
		if (json != NULL) {
			int json_len = strlen(json);

			mf_t.i = json_len;
			//D(" ---> mf_t.i=%d mf_t.val[0]=%d [1]%d  [2]%d  [3]%d",mf_t.i, mf_t.val[0],mf_t.val[1],mf_t.val[2],mf_t.val[3]);
			sendDataToTcpServer(mf_t.val, 4);
			// D(" ---> before send json");
			sendDataToTcpServer(json, strlen(json));
			// D(" ---> after send json");
			free(json);
		}else{
			mf_t.i = 0;
			sendDataToTcpServer(mf_t.val, 4);
		}
		json = NULL;

	}
		pthread_exit(NULL);
}

void SocketUtils::startTcpThread() {
	pthread_t thread_tcp;
	if (!getTcpstate()) {
		return;
	}
	thread_tcp = startThread();
}


ssize_t SocketUtils::ProcessDirFiles(char* path){

	return 0;
}
