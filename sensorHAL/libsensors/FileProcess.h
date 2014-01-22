/*
 * FileProcess.h
 *
 *  Created on: 2012-3-27
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

#ifndef FILEPROCESS_H_
#define FILEPROCESS_H_

class FileProcess {
public:
	static char* doProcessDirectory(const char* path , int pathRemaining);
private:
	static char* make_message(const char *fmt, ...);
};

#endif /* FILEPROCESS_H_ */
