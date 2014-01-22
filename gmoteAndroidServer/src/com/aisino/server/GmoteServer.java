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
import java.io.FileFilter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gmote.common.DataReceiverIF;
import org.gmote.common.FileInfo;
import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.MouseEvent;
import org.gmote.common.Protocol.ServerErrorType;
import org.gmote.common.TcpConnection;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.ListReplyPacket;
import org.gmote.common.packet.ListReqPacket;
import org.gmote.common.packet.MouseClickPacket;
import org.gmote.common.packet.RemoteEventPacket;
import org.gmote.common.packet.ServerErrorPacket;
import org.gmote.common.packet.SimplePacket;
import org.gmote.common.packet.ScreenshotPacket;
import org.gmote.common.packet.ScreenshotPacketReq;
import org.gmote.common.packet.MotionEventPacket;
import org.gmote.common.packet.TextPacket;
import org.gmote.common.packet.MouseWheelPacket;
import org.gmote.common.packet.RunFileReqPacket;
import org.gmote.common.packet.LaunchUrlPacket;
import org.gmote.common.packet.PluginPacket;

import org.gmote.common.ScreenInfo;
import com.aisino.server.settings.BaseMediaPaths;
import com.aisino.server.settings.DefaultSettings;
import com.aisino.server.settings.DefaultSettingsEnum;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.preference.PreferenceManager;

public class GmoteServer extends InputMethodService implements DataReceiverIF {
    private final static boolean USE_HAL_FILES = false;
    private final String TAG = "GmoteServer";
    static final String VERSION = "2.0.2";
    static final String MINIMUM_CLIENT_VERSION = "2.0.0";
    private static final Logger LOGGER = Logger.getLogger(GmoteServer.class.getName());
    static public TcpConnection mTcpCon;

    private AisinoService serverUi;
    private Handler mHandler;
    private Boolean mB;
    private static InputConnection iC;
    private View mInputView;
    @Override
    public void onCreate() {
        Log.d(TAG, "---onCreate android.os.Process.myPid()="+android.os.Process.myPid());
        super.onCreate();
    }
    @Override
    public void onInitializeInterface() {
        iC = getCurrentInputConnection();
  		if(iC == null)
  			Log.i(TAG,"onInitializeInterface:getCurrentInputConnection return null");
    	super.onInitializeInterface();
    }
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
    	Log.i(TAG,"onStartInput start");
    	iC = getCurrentInputConnection();
    	if(iC == null)
  			Log.i(TAG,"onStartInput:getCurrentInputConnection return null");
    	super.onStartInput(attribute, restarting);
    }
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
    	Log.i(TAG,"onCreateInputView start");
        mInputView =getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setMinimumHeight(ScreenInfo.height/5);
        return mInputView;
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
    	SimplePacket tempPacket = new SimplePacket(Command.KEYBOARD);
    	if (tempPacket != null && mTcpCon != null) {
            sendPacket(mTcpCon, tempPacket);
        }
    	super.onStartInputView(info, restarting);
    }

    /**
     * Called when a packet is received from the user.
     */
    @Override
    public void handleReceiveData(AbstractPacket packet, TcpConnection connection) {
        // TODO Auto-generated method stub
       // LOGGER.info("--->handleReceiveData Received command: " + packet.toString());
        Command command = packet.getCommand();
        AbstractPacket returnPacket = null;
     // add by gaoqi
        if (command == Command.TEXTSTR_REQ) {
          	Log.i("^^^^^^^^^^^^^^^^^^^^^^^^", "---->Base paths =" + ((TextPacket)packet).getTextPacket());
          		if(iC == null){
          			Log.i(TAG,"handleReceiveData:getCurrentInputConnection return null");
          		}else{
          			mB = iC.commitText(((TextPacket)packet).getTextPacket(), ((TextPacket)packet).getTextPacket().length());
          		}
          }
        if (command == Command.REMOTE_EVENT){
        	TrackpadHandler.instance().handleRemoteCommand((RemoteEventPacket)packet);
        	//returnPacket = (RemoteEventPacket)packet; // add by zhangdawei
        }
      // end gaoqi
        if (command == Command.BASE_LIST_REQ) {
            // Return the base list of directories the client has access to.
            List<FileInfo> existingBasePaths = new ArrayList<FileInfo>();
            for (FileInfo path : BaseMediaPaths.getInstance().getBasePaths()) {
              // Make sure that we only return paths that exist.
              if (new File(path.getAbsolutePath()).exists()) {
                Log.i(TAG, "---->Base paths =" +path.getAbsolutePath());
                existingBasePaths.add(path);
              }
            }

            returnPacket = new ListReplyPacket(existingBasePaths.toArray(new FileInfo[existingBasePaths
                .size()]));
          } else if (command == Command.LIST_REQ) {
           // add by zhangdawei
              if(USE_HAL_FILES){
                Log.d(TAG, "command.list_req");
                ListReqPacket listReqPacket = (ListReqPacket) packet;

                String path = listReqPacket.getPath();

                boolean osopen = HalSocket.getInstance().sendFilesRequestToHAL(path);
                if (!osopen) {
                    returnPacket = createFileNotExistErrorPacket();
                }
              }
              else{
                ListReqPacket listReqPacket = (ListReqPacket) packet;
                String path = listReqPacket.getPath();
                File file = new File(path);
                if (file.exists()) {
                     returnPacket = createListFilesPacket(path);
                } else {

                    returnPacket = createFileNotExistErrorPacket();
                }
            }


          } else if (command == Command.RUN){  // add by zhangdawei
           // Run a file in its default application.
              Log.d(TAG,"RUN: ");
              FileInfo fileInfo = ((RunFileReqPacket) packet).getFileInfo();
              String filePath = fileInfo.getAbsolutePath();
              this.serverUi.openFile(filePath);

              // end add
          } else if (command == Command.LAUNCH_URL_REQ){  // add by gaoqi 20120120
        	  // open a web site using default web browser in server
        	  String u = ((LaunchUrlPacket)packet).getUrl();
        	  Log.i(TAG, "Command.LAUNCH_URL_REQ" + u);
        	  this.serverUi.openUrl(u);


          }else if(command == Command.MOUSE_CLICK_REQ){
              MouseEvent mouseEvent = ((MouseClickPacket)packet).getMouseEvent();
//              if(mouseEvent == MouseEvent.LEFT_MOUSE_UP){
//                  returnPacket = new MouseClickPacket(mouseEvent);
//              }
              TrackpadHandler.instance().hanldeMouseClickCommand((MouseClickPacket) packet);
          } else if(command == Command.MOUSE_WHEEL_REQ){
              TrackpadHandler.instance().handeMouseWheelCommand((MouseWheelPacket)packet);
          }else if(command == Command.CLIENT_EXIT_FORCE){
              mHandler.sendEmptyMessage(AisinoService.CLIENT_EXIT_FORCE);
          }else if(command == Command.BEATHEART){ // add by zhangdawei
              returnPacket = new SimplePacket(Command.BEATHEART_REPLY);
              LOGGER.info("======heart beat from client Pong! Pong!");
          }else if(command == Command.SCREENSHOT_REQ){
			ScreenshotPacketReq screenpacketreq = (ScreenshotPacketReq) packet;
			int width = screenpacketreq.getWidth();
			int height = screenpacketreq.getHeight();
			LOGGER.info("======SCREENSHOT_REQ========= received");
			byte[] imagebuffer = Screenshot.getScreenshot(serverUi.getApplicationContext(),width,height);
			returnPacket = new ScreenshotPacket(imagebuffer);
		  }else if(command == Command.MOTION_EVENT) {
			TrackpadHandler.instance().handeMotionEvent((MotionEventPacket)packet);
          }else if(command == Command.BROWSE_PLUGIN_INSTALL) {
			LOGGER.info("======BROWSE_PLUGIN_INSTALL=========");
			PluginManager mPluginHandler = new PluginManager(serverUi.getApplicationContext());
			if(mPluginHandler.addBrowsePlugin()) {
				// edit pre
				Settings.getInstance(PreferenceManager.getDefaultSharedPreferences(serverUi.getApplicationContext()));
				Settings.setKeyBrowsePlugin(true);
				Settings.writeBack();
				Settings.releaseInstance();
				returnPacket = new PluginPacket(Command.BROWSE_PLUGIN_INSTALL_RESULT,true);
			}else {
				LOGGER.info("===ERROR: "+"install browse plugin error!");
				returnPacket = new PluginPacket(Command.BROWSE_PLUGIN_INSTALL_RESULT,false);
			}
		  }else if (command == Command.SENSOR_PLUGIN_INSTALL){
			LOGGER.info("======SENSOR_PLUGIN_INSTALL=========");
			PluginManager mPluginHandler = new PluginManager(serverUi.getApplicationContext());
			if(mPluginHandler.addSensorPlugin()) {
				Settings.getInstance(PreferenceManager.getDefaultSharedPreferences(serverUi.getApplicationContext()));
				Settings.setKeySensorPlugin(true);
				Settings.writeBack();
				Settings.releaseInstance();
				returnPacket = new PluginPacket(Command.SENSOR_PLUGIN_INSTALL_RESULT,true);
			}else {
				LOGGER.info("===ERROR: "+"install sensor plugin error!");
				returnPacket = new PluginPacket(Command.SENSOR_PLUGIN_INSTALL_RESULT,false);
			}
          }
        if (returnPacket != null) {
            sendPacket(connection, returnPacket);
            LOGGER.info("--->Sent reply to client");
          } else if (command != Command.MOUSE_CLICK_REQ && command != Command.MOUSE_MOVE_REQ
              && command != Command.KEYBOARD_EVENT_REQ) {
           // LOGGER.warning("Did not send a return packet for an incomming request: " + packet);
          }
    }

    /**
     * Starts the server.
     *
     * @param ui
     * @param arguments
     *          The command line arguments that were passed to the program in a
     *          key=value format. (ex: loglevel=ALL)
     * @throws IOException
     */
    void startServer(AisinoService ui, String[] arguments, Handler mHandler) throws IOException {
      this.serverUi = ui;
      this.mHandler = mHandler;
      // Start a thread that will supply our ip to clients.
      int mouseUdpPort = MulticastServerThread.MULTICAST_LISTENING_PORT;

      try {
        mouseUdpPort = Integer.parseInt(DefaultSettings.instance().getSetting(
            DefaultSettingsEnum.UDP_PORT));
      } catch (NumberFormatException e) {
        Log.d(TAG, "There was an error reading the udp port from the config file. Using default setting. "
                + e.getMessage());
        DefaultSettings.instance().setSetting(DefaultSettingsEnum.UDP_PORT,
            Integer.toString(mouseUdpPort));
      }
      Message msg = mHandler.obtainMessage(AisinoService.RECEVIE_NETWORK_UDPPORT, mouseUdpPort,0);
      mHandler.sendMessage(msg);
      MulticastServerThread.listenForIpRequests(mouseUdpPort,mHandler);

      TcpConnectionHandler.instance(this).listenOnAllIpAddresses(mHandler);
	  Log.d(TAG,"==============TcpConnectionHandler->listenOnAllIpAddresses Start!!!");
    }

    private void sendPacket(TcpConnection connection, AbstractPacket returnPacket) {
        // Send a return packet to the client.
        try {
          connection.sendPacket(returnPacket);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
      }

 // add by zhangdawei
    /**
     * Creates a packet with the list of the files in the current directory.
     */
    ListReplyPacket createListFilesPacket(String directory) {
      File file = new File(directory);
      FileFilter filter = new FileFilter() {
        public boolean accept(File arg0) {

          if (arg0.isHidden()) {
            return false;
          }

          if (arg0.isDirectory()) {
            return true;
          }

//          String name = arg0.getName().toLowerCase();
//          if (SupportedFiletypeSettings.fileNameToFileType(name) != FileType.UNKNOWN) {
//            return true;
//          }

          boolean showAllFiles = DefaultSettings.instance().getSetting(
              DefaultSettingsEnum.SHOW_ALL_FILES).equalsIgnoreCase("true");
            return true;
        }
      };

        File[] listOfFiles = file.listFiles(filter);
        ListReplyPacket packet = null;
        if (listOfFiles == null) {
            Log.i(TAG, "#######listOfFiles is null");
            FileInfo[] fileInfos = new FileInfo[0];
            packet = new ListReplyPacket(fileInfos);
            packet.setErrorType(ServerErrorType.NO_THIS_FUNCTION);
        } else {
            FileInfo[] fileInfo = convertFileListToFileInfo(listOfFiles);

            Arrays.sort(fileInfo);
            packet = new ListReplyPacket(fileInfo);
        }
        return packet;
    }

    ListReplyPacket getFileInfoFromSh(String directory){
        try {
            String testfile = "/mnt/sdcard/Android";
            File file = new File(testfile);
            Log.d(TAG,"test file is a directory? " + String.valueOf(file.isDirectory()));
            // Executes the command.
            Process process = Runtime.getRuntime().exec("ls " + directory);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

            String resultinfo =  output.toString();
            Log.d(TAG,"result info:\n"+ resultinfo);
        } catch (Exception e) {
            Log.d(TAG,"execute the commad error:"+e.getMessage());
        }
        ListReplyPacket packet = new ListReplyPacket(null);
        return packet;
    }
    private FileInfo[] convertFileListToFileInfo(File[] files) {
        FileInfo[] fileInfo = new FileInfo[files.length];
        int index = 0;
        for (File file : files) {
          fileInfo[index] = ServerUtil.instance().fileInfoFromFile(file);
          index++;
        }
        return fileInfo;
      }

    private AbstractPacket createFileNotExistErrorPacket() {
        AbstractPacket returnPacket;
        returnPacket = new ServerErrorPacket(ServerErrorType.INVALID_FILE.ordinal(),
            "The file does not exist.");
        return returnPacket;
      }
}
