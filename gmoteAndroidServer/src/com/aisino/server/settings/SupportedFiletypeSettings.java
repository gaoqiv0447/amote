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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gmote.common.FileInfo.FileType;

import com.aisino.server.media.basic.DefaultFilePlayer;

import android.util.Log;

public class SupportedFiletypeSettings {
    private static final String TAG = "SupportedFiletypeSettings";
    private static SupportedFiletypeSettings instance = null;
    private static final String FIELD_SEPARATOR = ":";
    private static final String FILE_GROUP_PREFIX = "filegroup";
    private Map<String, String> defaultPlayerExceptions = new HashMap<String, String>();
    private Map<FileType, String> defaultPlayerForFileType = new HashMap<FileType, String>();
    private Map<String, FileType> fileExtensionToFileType = new HashMap<String, FileType>();
    public static synchronized SupportedFiletypeSettings getInstance() {
        if (instance == null) {
          instance = new SupportedFiletypeSettings(SystemPaths.SUPPORTED_FILE_TYPES.getFullPath());
        }
        return instance;
      }
    // Private constructor to prevent instantiation.
    private SupportedFiletypeSettings(String fileName) {
      Log.d(TAG,"Initializing supported file types settings.");
      loadSupportedTypes(fileName);
      Log.d(TAG,"Done initializing supported file types settings.");
    }

    /**
     * Determines the type of file from it's name by looking at it's extension.
     */
    public static FileType fileNameToFileType(String fileName) {
      String extension = extractFileExtension(fileName);
      if (extension == null) {
        return FileType.UNKNOWN;
      }
      return extensionToFileType(extension);
    }



    public static String extractFileExtension(String fileName) {
        if (fileName == null) {
          return null;
        }
        String[] fileSplit = fileName.split("\\.");
        return fileSplit[fileSplit.length - 1].toLowerCase();
      }
    private static FileType extensionToFileType(String fileExtension) {
        if (fileExtension == null
            || !getInstance().fileExtensionToFileType.containsKey(fileExtension.toLowerCase())) {
          return FileType.UNKNOWN;
        }
        return getInstance().fileExtensionToFileType.get(fileExtension.toLowerCase());
      }

    private synchronized void loadSupportedTypes(String fileName) {
        try {
          BufferedReader reader = new BufferedReader(new FileReader(fileName));
          String line = null;
          FileType currentFileType = null;
          while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line != "" && !line.startsWith("#")) {
              String[] fields = line.split(FIELD_SEPARATOR);
              if (fields[0].equalsIgnoreCase(FILE_GROUP_PREFIX)) {
                // This is a new group of files.
                currentFileType = FileType.valueOf(fields[1].toUpperCase());
                defaultPlayerForFileType.put(currentFileType, fields[2]);
              } else {
                fileExtensionToFileType.put(fields[0].toLowerCase(), currentFileType);
                if (fields.length > 1) {
                  // The user has specified that this file should be played in a player different from the default for the group.
                  defaultPlayerExceptions.put(fields[0].toLowerCase(), fields[1]);
                }
              }
            }
          }
          // Do a little error checking in case the config file was not written properly.
          if (!defaultPlayerForFileType.containsKey(FileType.UNKNOWN)) {
            Log.d(TAG,"Did not find UNKNOWN as a file type in config file.");
            defaultPlayerForFileType.put(FileType.UNKNOWN, DefaultFilePlayer.class.getName());
          }
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        }
      }



}
