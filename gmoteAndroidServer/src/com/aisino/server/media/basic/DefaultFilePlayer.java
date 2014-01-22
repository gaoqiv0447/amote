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

package com.aisino.server.media.basic;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.gmote.common.FileInfo;
import org.gmote.common.Protocol.Command;
import org.gmote.common.media.MediaMetaInfo;

import android.util.Log;

import com.aisino.server.ServerUtil;
import com.aisino.server.media.MediaPlayerInterface;
import com.aisino.server.media.UnsupportedCommandException;

public class DefaultFilePlayer implements MediaPlayerInterface{
    private static final String TAG = "DefaultFilePlayer";
    private boolean isRunning = false;
    @Override
    public void initialise(String[] arguments) {
        // TODO Auto-generated method stub

    }

    @Override
    public void runFile(FileInfo fileInfo) throws UnsupportedEncodingException,
            UnsupportedCommandException {
        ServerUtil.instance().startFileInDefaultApplication(fileInfo.getAbsolutePath());
        isRunning = true;

    }

    @Override
    public void controlPlayer(Command command) throws UnsupportedCommandException {
        Log.i(TAG, "*************DefaultFilePlayer***********");
            if (command == Command.FAST_FORWARD || command == Command.FAST_FORWARD_LONG) {

            } else if (command == Command.REWIND || command == Command.REWIND_LONG) {

            } else if (command == Command.CLOSE) {

              isRunning = false;
            } else if (command == Command.PLAY) {
              // Maximizes a window

            }
    }

    @Override
    public MediaMetaInfo getNewMediaInfo() {
        return new MediaMetaInfo("", "File Viewer", "", PlayerUtil
                .loadImage("file.png"), false);
    }

    @Override
    public List<FileInfo> getBaseLibraryFiles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<FileInfo> getLibrarySubFiles(FileInfo fileInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRunning() {
        // TODO Auto-generated method stub
        return isRunning;
    }

}
