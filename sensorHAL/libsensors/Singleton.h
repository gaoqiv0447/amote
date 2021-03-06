/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef SINGLETON_H
#define SINGLETON_H

#include <stdint.h>
#include <sys/types.h>

// ---------------------------------------------------------------------------

//template <typename TYPE>
//class Singleton
//{
//public:
//    static TYPE& getInstance() {
//        TYPE* instance = sInstance;
//        if (instance == 0) {
//            instance = new TYPE();
//            sInstance = instance;
//        }
//        return *instance;
//    }
//
//protected:
//    ~Singleton() { };
//    Singleton() { };
//
//private:
//    Singleton(const Singleton&);
//    Singleton& operator = (const Singleton&);
//    static TYPE* sInstance;
//};
template<class T>
class Singleton {
public:
	static T& getInstance() {
		static T _instance;
		return _instance;
	}
protected:
	Singleton(void) {
	}
	virtual ~Singleton(void) {
	}
	Singleton(const Singleton<T>&); //不实现
	Singleton<T>& operator=(const Singleton<T> &); //不实现
};

// ---------------------------------------------------------------------------

#endif //SINGLETON_H
