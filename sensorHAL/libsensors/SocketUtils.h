/*
 * SocketUtils.h
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

#ifndef SOCKETUTILS_H_
#define SOCKETUTILS_H_

#include <cutils/sockets.h>
#include "Singleton.h"
#include <errno.h>
#include <sys/types.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <string.h>
#include <netdb.h>
#include <stdio.h>
#include <pthread.h>
#include "common.h"
#include "nusensors.h"
#include "FileProcess.h"
#include "MyThread.h"


static const ssize_t PACKET_LEN  = 5;
static const char* UDP_SOCKET = "/data/amote_udp";
static const char* TCP_SOCKET = "amote_tcp";
static const ssize_t UDP_LEN = 22;
static const ssize_t DATA_LEN = UDP_LEN* PACKET_LEN;
static const ssize_t TCP_LEN = 3;


class SocketUtils : public Singleton<SocketUtils>,MyThread {
	 friend class Singleton<SocketUtils>;
	 struct sockaddr_un so_server;
	 int sockfd;
	 int sockfd_t;
	 bool isUdpConnected;
	 uint32_t mEnabledList;
	 SocketUtils();
public:
	 /*
	  * Read data.
	  */
	 ssize_t readDataFromUdp(int type, RecSensorData* buf, size_t count);
	 ssize_t createTcpClient();
	 ssize_t sendDataToTcpServer(const void*  buff, int  len);
	 ssize_t enableSensor(int handle,int en);
	 ssize_t addEnableList(int handle);
	 ssize_t waitClientOk();
	 ssize_t revcListFromTcp(sensor_st* list);
	 ssize_t recvDataFromTcp(void*  buff, int  len);
	 bool getUdpstate() const;
	 bool getTcpstate() const ;
	 void processData(char* data, int len);
	 void startTcpThread();
	 ssize_t ProcessDirFiles(char* path);
	 RecSensorData mDatas[PACKET_LEN];
	 void worker();
};


#endif /* SOCKETUTILS_H_ */
