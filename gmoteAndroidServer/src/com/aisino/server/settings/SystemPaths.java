/**
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

package com.aisino.server.settings;

import android.os.Environment;
import android.util.Log;

public enum SystemPaths {
    DEFAULT_SETTINGS("default_settings.txt"),
    SUPPORTED_FILE_TYPES("supported_filetypes.txt"),
    BASE_PATHS("base_paths.txt"),
    PASSWORD("data_settings.txt"),
    STARTUP_SETTINGS("startup_settings.txt"),
    GMOTE_LOG("gmote.log"),
    GMOTE_ERROR_LOG("gmote-error.log"),
    PREFERED_PORTS("prefered_ports.txt");

    String name;
    public static String ROOT_PATH = null;
    public String getName() {
      return name;
    }

    SystemPaths(String pathName) {
      name = pathName;
    }

    public static String getRootPath() {
      if (ROOT_PATH == null) {
        ROOT_PATH = Environment.getExternalStorageDirectory().getPath();
        ROOT_PATH = ROOT_PATH + "/gmotedata/";
      }

      return ROOT_PATH;
    }

    public String getFullPath() {
      return getRootPath() + "/" + name;
    }
}
