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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.gmote.common.FileInfo;
import org.gmote.common.FileInfo.FileType;
import org.gmote.common.MimeTypeResolver;

import com.aisino.server.settings.BaseMediaPaths;
import com.aisino.server.settings.SupportedFiletypeSettings;

import android.util.Log;

public class GmoteHttpServer {
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final String TAG ="GmoteHttpServer";

    private Socket connectionSocket;

    public GmoteHttpServer(Socket connectionSocket) {
      this.connectionSocket = connectionSocket;
    }

    public void handleHttpRequestAsync(List<String> latestSessionIds) {
      HttpConnectionHandler conHandler = new HttpConnectionHandler(latestSessionIds);
      new Thread(conHandler).start();
    }
    /**
     *
     * @param latestSessionIds
     *          List of the last 5 session ids that we have seen. We keep more
     *          than once since there are cases where the client could request a
     *          song from the media player, re-connect, and then seek to furthur
     *          in the song, which would cause the media player to do an http
     *          request with the old session id.
     * @throws InterruptedException
     * @throws ImageFormatException
     */
    private void handleHttpRequest(List<String> latestSessionIds) throws InterruptedException {

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        List<String> header = extractHeader(reader);
        String requestedUrl = extractFile(header.get(0));

        String[] urlSplit = requestedUrl.split("\\?");
        if (urlSplit.length < 2 || urlSplit[1].indexOf("=") < 0) {
          Log.d(TAG,"Encountered a malformed url. It's missing a session param. Ignoring request: " + requestedUrl);
          return;
        }

        String sessionId = getParamValue("sessionId", urlSplit[1]);
        if (sessionId == null || !latestSessionIds.contains(sessionId)) {
          Log.d(TAG,"Encountered a malformed url. It has an incorrect session param. Ignoring request: " + requestedUrl + " -- expected: " + latestSessionIds);
          return;
        }

        File file = new File(urlSplit[0]);

        if (!file.exists()) {
          throw new FileNotFoundException("The file was not found: " + file.getName());
        }

        if (!downloadOfFileIsAllowed(file)) {
          throw new FileNotFoundException("The user is not authorized to download this type of file. Please make sure that the file is in the base-paths and that the file type of the file is in the supported_filetypes.txt file");
        }

        long startingByte = extractRange(header);

        PrintWriter ps = new PrintWriter(connectionSocket.getOutputStream());
        printHeaders(file, startingByte,  ps);

        if (SupportedFiletypeSettings.fileNameToFileType(file.getName()) == FileType.IMAGE) {
          sendImage(file, new BufferedOutputStream(connectionSocket.getOutputStream()));
        } else {
          sendFile(file, startingByte, new BufferedOutputStream(connectionSocket.getOutputStream()));
        }


      } catch (UnsupportedEncodingException e) {
        Log.d(TAG, e.getMessage(), e);
      } catch (IOException e) {
        Log.d(TAG, e.getMessage(), e);
      }
    }
    private String getParamValue(String paramName, String fullParam) {
        String[] paramSplit = fullParam.split("=");
        if (paramSplit.length != 2) {
          return null;
        }
        if (paramSplit[0].equalsIgnoreCase(paramName)) {
          return paramSplit[1];
        }

        return null;
      }
    private String getHeaderValue(String fieldName, List<String> headers) {
        for (String header : headers) {
          Log.i(TAG,"----->Header: " + header);
          if (header.startsWith(fieldName + ":") ) {
            return header.substring(header.indexOf(":") + 1).trim();
          }
        }
        return null;
      }

    private List<String> extractHeader(BufferedReader reader) throws IOException {
        String line = null;
        List<String> header = new ArrayList<String>();
        while ((line = reader.readLine()) != null && !(line.length()==0)) {
          header.add(line);
        }
        return header;
      }

    private String extractFile(String fileNameHeaderLine) throws IOException, UnsupportedEncodingException {
        Log.i(TAG,"---->Extracting file path from: " + fileNameHeaderLine);
        String[] fields = fileNameHeaderLine.split(" ");
        if (fields.length < 2) {
          throw new MalformedURLException("Invalid url. Did not find file name: " + fileNameHeaderLine);
        }

        String fileName = URLDecoder.decode(fields[0], "UTF-8");
        if (!fileName.startsWith("/files/")) {
          fileName = URLDecoder.decode(fields[1], "UTF-8");
          if (!fileName.startsWith("/files/")) {
            Log.d(TAG,"Invalid url. Ignoring connection request: " + fileName);
            throw new MalformedURLException("Invalid url. Url doesn't start with /files: " + fileName);
          }
        }
        fileName = fileName.substring("/files/".length());

        return fileName;
      }

    private long extractRange(List<String> headers) {

        String headerValue = getHeaderValue("Range", headers);
        if (headerValue == null) {
          return 0;
        }
        if (headerValue.startsWith("bytes=")) {
          headerValue = headerValue.substring("bytes=".length());
          String fields[] = headerValue.split("-");
          try {
            return Long.parseLong(fields[0]);
          } catch (NumberFormatException e) {
            Log.d(TAG, e.getMessage(), e);
            return 0;
          }
        }
        return 0;
      }
    /**
     * Returns true if, and only if, the file meets the following conditions: 1.
     * The file must be in a directory that is a child of 'base paths' 2. The file
     * must be of a file type that is in the supporte_filetypes.
     *
     * This is based on the least privilege principle. It helps ensure that
     * potential intruders will only have access to media files, and that these
     * files are only
     */
    private boolean downloadOfFileIsAllowed(File file) {
      if (SupportedFiletypeSettings.fileNameToFileType(file.getName()) == FileType.UNKNOWN) {
        return false;
      }

      for (FileInfo path : BaseMediaPaths.getInstance().getBasePaths()) {
        // Make sure that we only return paths that exist.
        if (file.getAbsolutePath().toLowerCase().startsWith(path.getAbsolutePath().toLowerCase())) {
          return true;
        }
      }
      return false;
    }


    void sendFile(File targ, long startingByte, BufferedOutputStream dataOut) throws IOException {
        Log.i(TAG,"----->Sending file: " + targ.getAbsolutePath() + " offset: " + startingByte);
        byte[] buf = new byte[2048];

        InputStream is = null;

        if (targ.isDirectory()) {
          // listDirectory(targ, ps);
          return;
        } else {
          is = new FileInputStream(targ.getAbsolutePath());
          if (startingByte != 0) {
            long bytesSkipped = is.skip(startingByte);
            Log.i(TAG,"--->bytesSkipped = " + bytesSkipped);
          }
        }

        try {
          int n;
          while ((n = is.read(buf)) >= 0) {
            dataOut.write(buf, 0, n);
          }
        } finally {
          Log.i(TAG,"---->Done sending file");
          is.close();
        }
        dataOut.close();
        Log.i(TAG,"---->Print stream closed");
      }

    private void sendImage(File originalImagePath, BufferedOutputStream dataOut) throws InterruptedException, IOException {
        Log.i(TAG,"---->Converting image to smaller scale");
     // load image from INFILE
        // determine thumbnail size from WIDTH and HEIGHT}
        Log.d(TAG, "****sendImage*****");
        // draw original image to thumbnail image object and
        // scale it to the new size on-the-fly
        dataOut.close();
        Log.i(TAG,"--->Done sending image");
      }

    boolean printHeaders(File targ, long startingByte, PrintWriter pw) throws IOException {
        boolean ret = false;

        if (!targ.exists()) {
          pw.println("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");

          ret = false;
        } else {
          pw.println("HTTP/1.0 " + HTTP_OK + " OK");

          ret = true;
        }

        pw.println("Server: GmoteHttpServer");
        pw.println("Date: " + (new Date()));
        if (ret) {
          long fileLength = targ.length();
          if (startingByte != 0) {
            pw.println("Content-range: bytes" + startingByte + "-" + (fileLength - 1) + "/" + fileLength);
          }
          pw.println("Content-length: " + (fileLength - startingByte));
          pw.println("Last Modified: " + (new Date(targ.lastModified())));
          String name = targ.getName();
          String ct = MimeTypeResolver.findMimeType(name);
          if (ct.equals(MimeTypeResolver.UNKNOWN_MIME_TYPE)) {
            FileType type = SupportedFiletypeSettings.fileNameToFileType(name);
            if (type == FileType.MUSIC) {
              ct = "audio/unknown";
            } else if (type == FileType.VIDEO) {
              ct = "video/unknown";
            } else {
              ct = MimeTypeResolver.findMimeTypeSlow(targ);
            }
          }
          Log.d(TAG,"Mime type is: " + ct);
          pw.println("Content-type: " + ct);

        }
        pw.println();
        pw.flush();
        return ret;
      }

    public class HttpConnectionHandler implements Runnable {

        private static final String TAG = "HttpConnectionHandler";
        private List<String> latestSessionIds;

        public HttpConnectionHandler(List<String> latestSessionIds) {
          this.latestSessionIds = latestSessionIds;
        }

        public void run() {
          try {
            handleHttpRequest(latestSessionIds);
            Log.i(TAG,"Done handlerequest(). Closing connection.");
          } catch (Exception ex) {
            // Catching all exceptions since this is the top layer of our app.
            Log.d(TAG, ex.getMessage(), ex);

              try {
                PrintWriter ps = new PrintWriter(connectionSocket.getOutputStream());
                ps.println("HTTP/1.0 " + HTTP_NOT_FOUND + " not found " + ex.getMessage());
              } catch (IOException e) {
                Log.d(TAG, e.getMessage(), e);
              }

          } finally {
              Log.i(TAG,"---->Closing http connection");
            try {
              connectionSocket.close();
            } catch (IOException e) {
                Log.d(TAG, e.getMessage(), e);
            }
          }
        }
      }
}
