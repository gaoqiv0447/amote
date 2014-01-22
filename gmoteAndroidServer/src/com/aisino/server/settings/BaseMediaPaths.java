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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gmote.common.FileInfo;
import org.gmote.common.FileInfo.FileSource;
import org.gmote.common.FileInfo.FileType;

import android.os.Environment;
import android.util.Log;

import com.aisino.server.ServerUtil;

/**
 * Keeps track of the list of directories the user has access to by default.
 * These are at the bottom of the file tree. (ex: c:\downloads\movies)
 */
public class BaseMediaPaths {
  private final static String TAG = "BaseMediaPaths";
  private final static String PATH = "/mnt";
  private static BaseMediaPaths instance = null;
  private String fileName;
  private List<FileInfo> basePaths = new ArrayList<FileInfo>();

  private BaseMediaPaths(String fileName) {
    this.fileName = fileName;
    loadBasepathsFromFile();
  }

  public static synchronized BaseMediaPaths getInstance() {
    if (instance == null) {
      instance = new BaseMediaPaths(SystemPaths.BASE_PATHS.getFullPath());
    }
    return instance;
  }

  public void addPath(String path) {
    basePaths.add(ServerUtil.instance().fileInfoFromFile(new File(path)));
    saveBasepathsToFile();
  }

  public void removePath(int index) {
    basePaths.remove(index);
    saveBasepathsToFile();
  }

  public List<FileInfo> getBasePaths() {
    return Collections.unmodifiableList(basePaths);
  }

  /**
   * Creates a new, file that holds the default paths. This should only be done
   * the first time the application is launched.
   */
  public static void createDefaultFile() {
    getInstance().basePaths.clear();
    getInstance().addPath(createDefaultMediaPathForFileSystem().getAbsolutePath());
  }

  private void loadBasepathsFromFile() {
    basePaths.clear();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));

      String line;
      while ((line = reader.readLine()) != null) {
        File file = new File(line);
        if (ServerUtil.instance().isDvdDrive(file)) {
          basePaths.add(new FileInfo(file.getName(), file.getAbsolutePath(), FileType.DVD_DRIVE,
              true, FileSource.FILE_SYSTEM));
        } else {
          basePaths.add(ServerUtil.instance().fileInfoFromFile(file));
        }

      }
      reader.close();
      if (basePaths.size() == 0) {
        basePaths.add(ServerUtil.instance().fileInfoFromFile(createDefaultMediaPathForFileSystem()));
      }
    } catch (IOException e) {
      Log.d(TAG, e.getMessage(), e);
    }
  }

  private void saveBasepathsToFile() {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      for (FileInfo file : basePaths) {
        writer.write(file.getAbsolutePath());
        writer.newLine();
      }
      writer.close();
    } catch (IOException e) {
      Log.d(TAG, e.getMessage(), e);
    }
  }

  private static File createDefaultMediaPathForFileSystem() {
      //wdw modify for android 2011-12-26
      //return Environment.getRootDirectory();
	  File mntFile = new File(PATH);
	  if(mntFile.exists()){
		return mntFile;
	  }else {
		return null;
	  }
  }

}
