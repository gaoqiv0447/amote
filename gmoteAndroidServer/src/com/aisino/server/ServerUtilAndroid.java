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

package com.aisino.server;

import java.io.IOException;
import android.util.Log;

public class ServerUtilAndroid extends ServerUtil {
    private static final String TAG = "ServerUtilAndroid";

    @Override
    public void startFileInDefaultApplication(String fileName) {
        String[] commands = {
                "gnome-open", fileName
        };
        Log.i(TAG, "********startFileInDefaultApplication*********");
        // Run the file
        // TODO ********
        // Runtime.getRuntime().exec(commands);

    }

    @Override
    public void startFileInSeparateprocess(String command) {
        // Runtime.getRuntime().exec(command);
        // TODO ********
        Log.i(TAG, "********startFileInSeparateprocess*********");
    }

    @Override
    public String getUpdateUrl() {
        return "http://www.gmote.org/download/latest_version_linux.txt";
    }
}
