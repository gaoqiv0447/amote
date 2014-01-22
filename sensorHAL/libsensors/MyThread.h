/*
 * MyThread.h
 *
 *  Created on: 2012-3-28
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

#ifndef MYTHREAD_H_
#define MYTHREAD_H_
#include <pthread.h>
class MyThread
{
public:
    virtual ~MyThread() {}
    pthread_t startThread() {
        pthread_t tid;
        pthread_create(&tid, NULL, hook, this);
        return tid;
    }

private:
    static void* hook(void* args) {
        reinterpret_cast<MyThread*>(args)->worker();
        return NULL;
    }
protected:
    virtual void worker()=0;
};

#endif /* MYTHREAD_H_ */
