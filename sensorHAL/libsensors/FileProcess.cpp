/*
 * FileProcess.cpp
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

#include "FileProcess.h"
#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <errno.h>
#include <sys/stat.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

#include <cutils/log.h>

#if 1
#define  D(...)  LOGD(__VA_ARGS__)
#endif

#define JSON_FORMAT "{'fn':'%s','pt':'%s','dir':%d},"
#define HEADER "{file:{files:["
#define FOOTER "]}}"

 char * FileProcess::make_message(const char *fmt, ...)
       {
           /* Guess we need no more than 100 bytes. */
           int n, size = 100;
           char *p = NULL, *np = NULL;
           va_list ap;

           if ((p = (char*)malloc(size)) == NULL)
               return NULL;
           memset(p,0,size);
           while (1) {
               /* Try to print in the allocated space. */
               va_start(ap, fmt);
               n = vsnprintf(p, size, fmt, ap);
               va_end(ap);
               /* If that worked, return the string. */
               if (n > -1 && n < size){
                   return p;
               }
               /* Else try again with more space. */
               if (n > -1)    /* glibc 2.1 */
                   size = n+1; /* precisely what is needed */
               else           /* glibc 2.0 */
                   size *= 2;  /* twice the old size */
               if ((np = (char*)realloc (p, size)) == NULL) {
                   free(p);
                   return NULL;
               } else {
                   p = np;
               }
           }
           return NULL;
       }


 char* FileProcess::doProcessDirectory(const char* path,int pathRemaining) {
    // place to copy file or directory name
	D("doProcessDirectory\n");
	unsigned int n = 0, size = 1000;
	char* fileSpot =(char*) path + strlen(path);
	char* json = NULL,*njson = NULL;
	if((json = (char*)malloc(size)) == NULL){
		return NULL;
	}
	memset(json,0,size);
    struct dirent* entry;
    DIR* dir = opendir(path);
    if (!dir) {
    	D("opendir %s failed, errno: %d \n", path, errno);
    	free(json);
        return NULL;
    }
   // D(" path = %s fileSpot=%s\n",path,fileSpot);
    //D(" json0 = %s",json);
    strcat(json,HEADER);
    //D(" json1 = %s",json);
    while ((entry = readdir(dir))) {
		const char* name = entry->d_name;
		// ignore "." and ".."
		if (name[0] == '.'
				&& (name[1] == 0 || (name[1] == '.' && name[2] == 0))) {
			continue;
		}

		int type = entry->d_type;
		if (type == DT_UNKNOWN) {
			// If the type is unknown, stat() the file instead.
			// This is sometimes necessary when accessing NFS mounted filesystems, but
			// could be needed in other cases well.
			struct stat statbuf;
			if (stat(path, &statbuf) == 0) {
				if (S_ISREG(statbuf.st_mode)) {
					type = DT_REG;
				} else if (S_ISDIR(statbuf.st_mode)) {
					type = DT_DIR;
				}
			} else {
				//D("stat() failed for %s: %s", path, strerror(errno));
				D("----stat() failed for %s: %s \n", path, strerror(errno));
			}
		}

		 if (type == DT_REG || type == DT_DIR) {
			int nameLength = strlen(name);
			int isDirectory = (type == DT_DIR);
	           if (nameLength > pathRemaining || (isDirectory && nameLength + 1 > pathRemaining)) {
	                // path too long!
	                continue;
	            }
			//LOGI(" name = %s \n",name);
			if (name[0] == '.') {
				continue;
			}
			strcat(fileSpot, name);
			char * p;
			if (isDirectory) {
				strcat(fileSpot, "/");
				p = make_message(JSON_FORMAT, name, path,
						1);
			} else {
				p = make_message(JSON_FORMAT, name, path,
						0);
			}
			n = strlen(json) + strlen(p);
			if ( n > size) {
				size *= 2;
				if ((njson = (char*)realloc(json, size)) == NULL) {
					//D(" free p failed!");
					free(json);
					return NULL;
				} else {
					json = njson;
				}
			}

			//D(" p = %s strlen(json) =%d strlen(p) =%d \n", p, strlen(json), strlen(p));
			strcat(json, p);
			free(p);
			p = NULL;
			memset(fileSpot, 0, strlen(name) + 1);
		}

	}
    if (strlen(json) + strlen(FOOTER) > size) {
		if (n > size) {
			size += strlen(FOOTER);
			if ((njson = (char*) realloc(json, size)) == NULL) {
				D(" free p failed!");
				free(json);
				return NULL;
			} else {
				json = njson;
			}
		}
	}
    strcat(json,FOOTER);
    //D(" json3 = %s \n",json);
    //D(" str(json) = %d \n",strlen(json));
    closedir(dir);

	return json;
}



