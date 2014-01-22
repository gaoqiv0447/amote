/*
 * common.h
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

#ifndef COMMON_H_
#define COMMON_H_

static union FloatValue{
char val[4];
float f;
int i;
} mf_t;

static union LongValue{
char val[8];
long long l;
} ml_t;

typedef struct RecSensorData{
	 int type;
	 long long timestamp;
	 float values[3];

}RecSensorData;

typedef struct sensor_st{
	int handle;
	float maxRange;
	float resolution;
}sensor_st;

#endif /* COMMON_H_ */
