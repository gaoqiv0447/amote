/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 */
#include <string.h>
#include <jni.h>
#include <sys/types.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netdb.h>
#include <stdio.h>
#include <utils/Log.h>
//#include "com_aisino_server_HalSocket.h"

#define SOCKET "/data/amote_udp"


jint JNICALL Java_com_aisino_server_HalSocket_sendUdpPacket(JNIEnv * env, jobject thiz,
		 jint socketid, jbyteArray array1)
{
	jbyte* arr;
	int n;

	arr=(*env)->GetByteArrayElements(env, array1, NULL);
	jint length=(*env)->GetArrayLength(env, array1);

	if ((n=sendto(socketid, arr, length, 0, NULL, 0)) != length)
			printf("sending error\n");

	(*env)->ReleaseByteArrayElements(env, array1, arr, 0);

	return 0;
}

jint JNICALL Java_com_aisino_server_HalSocket_createUdpSocket(JNIEnv * env, jobject thiz)
{
	jint ret;
	printf("@@@@ HalSocket_createUdpSocket\n");
	if (!(ret = create_local_client())){
		printf("create local udp failed!\n");
	}
	return ret;
}

int create_local_client(){
	int sockfd, servlen,n;
	struct sockaddr_un  serv_addr;
	char buffer[1024];

	bzero((char *)&serv_addr,sizeof(serv_addr));
	serv_addr.sun_family = AF_UNIX;
	strcpy(serv_addr.sun_path, SOCKET);
	servlen = strlen(serv_addr.sun_path) + sizeof(serv_addr.sun_family);
	if ((sockfd = socket(AF_UNIX, SOCK_DGRAM,0)) < 0){
		LOGE("-----> local client socket create failed!");
		return sockfd;
	}

	if (connect(sockfd, (struct sockaddr *)&serv_addr, servlen) < 0){
		LOGE("-----> local client socket connect failed!");
		return -1;
	}
	LOGE("-----> local client socket start success!");
     return sockfd;
}
