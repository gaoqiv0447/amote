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

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.gmote.common.FileInfo;
import org.gmote.common.FileInfo.FileSource;
import org.gmote.common.FileInfo.FileType;

import com.aisino.server.settings.SupportedFiletypeSettings;

import android.util.Log;

public abstract class ServerUtil {
    private static final String TAG = "ServerUtil";
    private static ServerUtil mServerUtil = null;
    private static final String VIDEO_TS = "VIDEO_TS";

    public static ServerUtil instance() {
        if (mServerUtil == null) {
          try {
              // not supported yet!
              Log.d(TAG,"Server util  implemented for this android system");
              mServerUtil = (ServerUtil) Class.forName(
              "com.aisino.server.ServerUtilAndroid").newInstance();

          } catch (ClassCastException e) {
            Log.d(TAG, e.getMessage(), e);
          } catch (InstantiationException e) {
              Log.d(TAG, e.getMessage(), e);
            System.exit(1);
          } catch (IllegalAccessException e) {
              Log.d(TAG, e.getMessage(), e);
            System.exit(1);
          } catch (ClassNotFoundException e) {
              Log.d(TAG, e.getMessage(), e);
          }
        }
        return mServerUtil;
      }
    /**
     * Returns a list of all the local ip addresses on this computer. For
     * example, a laptop may have multiple local ip addresses if it is connected
     * to WiFi while an Ethernet cable is also plugged in.
     *
     * @param ignoreLoopback if true, will not return loopback addresses
     *            (127.0.0.1)
     */
    public static List<InetAddress> findAllLocalIpAddresses(boolean ignoreLoopback)
            throws SocketException {
        List<InetAddress> inetAddresses = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> nics = null;

        nics = NetworkInterface.getNetworkInterfaces();

        while (nics != null && nics.hasMoreElements()) {
            NetworkInterface nic = nics.nextElement();
            Enumeration<InetAddress> ipAddresses = nic.getInetAddresses();
            while (ipAddresses.hasMoreElements()) {
                InetAddress address = ipAddresses.nextElement();
                address.getAddress();
                if (address.isSiteLocalAddress()) {
                    inetAddresses.add(address);
                }
            }
        }
        return inetAddresses;
    }

    public FileInfo fileInfoFromFile(File file) {
        String fileName = file.getName();
        boolean isDirectory = file.isDirectory();
        String absolutePath = file.getAbsolutePath();
        FileType fileType = null;
        if (!isDirectory) {
          fileType = SupportedFiletypeSettings.fileNameToFileType(fileName);
        } else {
          if (folderIsRippedDvd(file)) {
            // Handle ripped dvd's as if they were media files.
            isDirectory = false;
            fileType = FileType.VIDEO;
          }
        }
        return new FileInfo(fileName, absolutePath, fileType, isDirectory, FileSource.FILE_SYSTEM);
      }

    public FileInfo fileInfoFromjson(String fileName, int isDir, String absolutePath) {
        boolean isDirectory =false;
        if(1==isDir ){
            isDirectory = true;
        }

        FileType fileType = null;
        if (!isDirectory) {
          fileType = SupportedFiletypeSettings.fileNameToFileType(fileName);
        } else {
          if (folderIsRippedDvd(fileName,isDirectory)) {
            // Handle ripped dvd's as if they were media files.
            isDirectory = false;
            fileType = FileType.VIDEO;
          }
        }
        return new FileInfo(fileName, absolutePath, fileType, isDirectory, FileSource.FILE_SYSTEM);
      }

    public boolean folderIsRippedDvd(File folder) {
        return (folder.isDirectory() && folder.getName().equalsIgnoreCase(VIDEO_TS));
      }

    public boolean folderIsRippedDvd(String name, boolean isDir) {
        return (isDir && name.equalsIgnoreCase(VIDEO_TS));
      }

    /**
     * Creates a directory unless it already exists
     */
    public static void createIfNotExists(String rootPath) {
        File f=new File(rootPath);
        if(f.exists()==false){
            f.mkdirs();
        }

    }

    /**
     * Returns true if the drive is a cd or dvd drive. Most dvd drives are called
     * 'cd' drives which is why we return both types.
     *
     */
    public boolean isDvdDrive(File drive) {
      return false;
    }

    /**
     * Run a file from the local file system in its default application.
     *
     */
    public abstract void startFileInDefaultApplication(String fileName);

    /**
     * Runs a program by starting it in its own process (not a subprocess of this
     * class like the standard exec function would do). This must be implemented
     * in each operating system
     *
     */
    public abstract void startFileInSeparateprocess(String command);

    public abstract String getUpdateUrl();

}
