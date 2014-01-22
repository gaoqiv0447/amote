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

package com.aisino.server.media;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.gmote.common.FileInfo;
import org.gmote.common.Protocol.Command;
import org.gmote.common.media.MediaMetaInfo;

public interface MediaPlayerInterface {
    public void initialise(String[] arguments);

    /**
     * Launches a file in this media controller.
     *
     * @param fileName The file to launch.
     * @param fileType
     * @throws UnsupportedEncodingException
     * @throws UnsupportedCommandException
     */
    public void runFile(FileInfo fileInfo) throws UnsupportedEncodingException,
        UnsupportedCommandException;

    /**
     * Handles commands to the player, such as pause, play etc.
     *
     * @throws UnsupportedCommandException
     */
    public void controlPlayer(Command command) throws UnsupportedCommandException;

    /**
     * @returns the new media information if media has changed, null otherwise
     */
    public MediaMetaInfo getNewMediaInfo();


    /**
     * Returns a list of files that is at the base of a media library. This is
     * useful when media players such as Itunes support their own media library.
     * If the player doesn't support this feature, returns null.
     */
    public List<FileInfo> getBaseLibraryFiles();

    public List<FileInfo> getLibrarySubFiles(FileInfo fileInfo);

    public boolean isRunning();

}
