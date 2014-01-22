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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;

import org.gmote.common.ScreenInfo;

import com.aisino.server.settings.BaseMediaPaths;
import com.aisino.server.settings.DefaultSettings;
import com.aisino.server.settings.StartupSettings;
import com.aisino.server.settings.StartupSettingsEnum;
import com.aisino.server.settings.SystemPaths;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.provider.Settings;
import android.webkit.MimeTypeMap;

public class AisinoService extends Service{

    private static final String TAG = "AisinoService";
    private static final int THIS_BASE = 4;
    private static final int MOUSE_INVISIBLE = THIS_BASE+1;
    private static final int MOUSE_VISIBLE = THIS_BASE +2;
    private static final int MOUSE_DISMISS_TIME = 5000;
    public static final int NETWORK_DISABLE =THIS_BASE+3 ;
    public static final int RECEVIE_NETWORK_ADDRESSES =THIS_BASE+4 ;
    public static final int RECEVIE_NETWORK_UDPPORT =THIS_BASE+5 ;
    public static final int RECEVIE_AUTHENTICATION_SUCCESS_MSG =THIS_BASE+6 ;
    public static final int RECEVIE_AUTHENTICATION_FAILED_MSG =THIS_BASE+7 ;
    public static final int CLIENT_EXIT_FORCE =THIS_BASE+8 ;
    public static final int HAL_SERVER_NOT_AVIABLE = THIS_BASE+9;
    public static final String AMOTE_NO_NETWORK_ACTION = "amote_no_network_action";
    public static final String AMOTE_NETWORK_ADDRESS_ACTION = "amote_network_address_action";
    public static final String AMOTE_NETWORK_UDPPORT_ACTION = "amote_network_udpport_action";
    public static final String AMOTE_AUTHENTICATION_FAIL_ACTION = "amote_auth_fail_action";
    public static final String AMOTE_AUTHENTICATION_SUCCESS_ACTION = "amote_network_success_action";
    public static final String AMOTE_HANDLE_STARTUP_ACTION = "amote_handle_startup_action";
    private WindowManager.LayoutParams wmParams=null;
    private int oldOrientation = -1;
    private NotificationManager mNotificationManager;
    private Ime mIme ;

    @Override
    public void onCreate() {
        Log.d(TAG, "---onCreate android.os.Process.myPid()="+android.os.Process.myPid());
        try {

            // set the input method
            mIme = new Ime();
            mIme.run();

            GmoteServer server = new GmoteServer();
            String[] args = null;
            wmParams = ((RemoteApplication)getApplication()).getMywmParams();
            setScreenInfo();
            createFiles();
            sendBroadcastToUi(AMOTE_HANDLE_STARTUP_ACTION);
            MouseUtil.createMouseView(this, wmParams,mHandler);
            server.startServer(this,args,mHandler);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mHandler.sendEmptyMessageDelayed(MOUSE_INVISIBLE,MOUSE_DISMISS_TIME);
            if (com.aisino.server.Settings.getKeySensorPlugin() || com.aisino.server.Settings.JUST_FOR_OURSELF) {
                HalSocket.getInstance();
                Log.d(TAG, "~~~~~~~~~~~~~~~HalSocket create!");
            }


        } catch (IOException e) {
            Log.d(TAG, "---startServer exception!");
            e.printStackTrace();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Boolean boot = intent.getBooleanExtra("start_from_boot", false);
        Log.d(TAG, "------onStartCommand start_from_boot=" +boot);
        if(boot){
            StartupSettings settings = StartupSettings.instance();

            if (!settings.getSetting(StartupSettingsEnum.PASSWORD_SHOWN)) {
                Intent newintent = new Intent(this,GmoteServerUiActivity.class);
                newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle extras = new Bundle();
                extras.putBoolean("start_from_boot", true);
                newintent.putExtras(extras);
                startActivity(newintent);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "---onBind");

        return mBinder;

    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (oldOrientation == -1) {
            oldOrientation = newConfig.orientation;
        }
        if (oldOrientation ==  newConfig.orientation) {
            TrackpadHandler.instance().setOrientation(false);
            return;
        }
        TrackpadHandler.instance().setOrientation(true);
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "---onDestroy");
        try{
        if (com.aisino.server.Settings.getKeySensorPlugin() || com.aisino.server.Settings.JUST_FOR_OURSELF) {
            HalSocket.getInstance().stopListenThread();
        }
        }catch(IOException e){
            Log.d(TAG, "---->stopListenThread failed!");
        }finally{
            super.onDestroy();
        }

    }


    private void setScreenInfo(){
        Settings.System.putInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION,
                0);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        ScreenInfo.width = displayMetrics.widthPixels;
        ScreenInfo.height = displayMetrics.heightPixels;
        ScreenInfo.densityDpi = displayMetrics.densityDpi;
        Log.i(TAG, "--->device width="+ScreenInfo.width+" height="+ScreenInfo.height +" ---densityDpi="+displayMetrics.densityDpi +" displayMetrics.density="+displayMetrics.density);
    }

    public void open(){

    }

    public void close(){
        mIme.enableAisinoInput(false);
        mHandler.removeCallbacksAndMessages(null);
        try{
            MouseUtil.removeMouseView();
        }catch(java.lang.IllegalArgumentException e ){
            Log.d(TAG, e.getMessage());
        }finally{
            mNotificationManager.cancel(R.drawable.icon);
            try{
                if (com.aisino.server.Settings.getKeySensorPlugin() || com.aisino.server.Settings.JUST_FOR_OURSELF) {
                    HalSocket.getInstance().stopListenThread();
                }
            }catch(final IOException ex){
                Log.d(TAG, " error when close halsocket");
            }
            stopSelf();      mNotificationManager.cancel(R.drawable.icon);
           // android.os.Process.killProcess(android.os.Process.myPid());
        }

    }
    int x = 0;
    int y= 0;
    Handler mHandler = new Handler(){

      public void handleMessage(android.os.Message msg) {
          switch(msg.what){
              case MouseUtil.MESSAGE_FOR_MOUSE:
                  x = msg.arg1;
                  y = msg.arg2;
                  mHandler.removeMessages(MOUSE_INVISIBLE);

                  if(MouseUtil.getMouseVisible()!=View.VISIBLE){
                      MouseUtil.setMouseVisible();
                  }
                  mHandler.post(runnable);//MouseUtil.updateMouseView(x, y);
                  mHandler.sendEmptyMessageDelayed(MOUSE_INVISIBLE,MOUSE_DISMISS_TIME);
                  break;
              case MOUSE_INVISIBLE:
                  MouseUtil.setMouseInvisible();
                  break;
              case MOUSE_VISIBLE:
                  MouseUtil.setMouseVisible();
                  break;
              case MouseUtil.MESSAGE_FOR_MOUSE_ARROW:
                  MouseUtil.setPointerMouse();
                  break;
              case MouseUtil.MESSAGE_FOR_MOUSE_HAND:
                  MouseUtil.setHandMouse();
                  break;
              case NETWORK_DISABLE:
                  Log.d(TAG, "---------->mBroadcastRecever sendBroadcast");
                  sendBroadcastToUi(AMOTE_NO_NETWORK_ACTION);
                  break;
              case RECEVIE_NETWORK_ADDRESSES:
                  Log.d(TAG, "---------->mBroadcastRecever RECEVIE_NETWORK_ADDRESSES");
                  String addr = ((InetAddress)msg.obj).getHostAddress();
                  int port = msg.arg1;
                  sendBroadcastToUi(AMOTE_NETWORK_ADDRESS_ACTION,addr,port);
                  break;
              case RECEVIE_NETWORK_UDPPORT:
                  sendBroadcastToUi(AMOTE_NETWORK_UDPPORT_ACTION,msg.arg1);
                  break;
              case RECEVIE_AUTHENTICATION_SUCCESS_MSG:

                  String mesg = getResources().getString(R.string.connect_success_message)+" " +msg.obj.toString();
                  mIme.enableAisinoInput(true);
                  ShowNotification(mesg, mesg,
                          null,
                          R.drawable.icon,
                          R.drawable.auth_success);
                  sendBroadcastToUi(AMOTE_AUTHENTICATION_SUCCESS_ACTION,msg.obj.toString(), 0);
                  break;
              case RECEVIE_AUTHENTICATION_FAILED_MSG:
                  String mesg1 = getResources().getString(R.string.connect_fail_message)+" " +msg.obj.toString();
                  ShowNotification(mesg1, mesg1,
                          null,
                          R.drawable.icon,
                          R.drawable.auth_fail);
                  sendBroadcastToUi(AMOTE_AUTHENTICATION_FAIL_ACTION, msg.obj.toString(), 0);
                  break;
              case CLIENT_EXIT_FORCE:
                  mIme.enableAisinoInput(false);
                  String mesg2 = getResources().getString(R.string.client_force_exit);
                  ShowNotification(mesg2, mesg2,
                          null,
                          R.drawable.icon,
                          R.drawable.auth_fail);
                  break;
          }
      };
    };

    private void sendBroadcastToUi(String action){
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void sendBroadcastToUi(String action,String str ,int arg){
        Intent intent = new Intent(action);
        intent.putExtra("argstr", str);
        intent.putExtra("argint", arg);
        sendBroadcast(intent);
    }
    private void sendBroadcastToUi(String action, int arg){
        Intent intent = new Intent(action);
        intent.putExtra("argint", arg);
        sendBroadcast(intent);
    }
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            MouseUtil.updateMouseView(x, y);
        }
    };


    private void ShowNotification(String tickerText, String contentTitle, String contentText,
            int id, int resId) {
        Notification notification = new Notification(resId, tickerText, System.currentTimeMillis());
        Intent g_intent = new Intent(this,GmoteServerUiActivity.class);
        g_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, g_intent, 0);
        notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
        mNotificationManager.notify(id, notification);
    }

    private void createFiles() {
        SystemPaths.ROOT_PATH = getFilesDir().getAbsolutePath();
        createIfNotExists(SystemPaths.getRootPath());

        // Initialize the startup settings first since some of the other
        // settings
        // might need its values.
        try {
            if (new File(SystemPaths.STARTUP_SETTINGS.getFullPath()).createNewFile()) {
                StartupSettings.createDefaultFile();

            }
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        }

        for (SystemPaths path : SystemPaths.values()) {
            try {
                if (path == SystemPaths.BASE_PATHS) {

                    File newFile = new File(SystemPaths.BASE_PATHS.getFullPath());
                    if (!newFile.exists()) {
                        // This is here for backwards compatibility. If the user
                        // previously
                        // had setup a file with base paths,
                        // we move it to the new location.
                        try {
                            URL urlOfOldFile = GmoteServerUiActivity.class
                                    .getResource("/config_files/base_paths.txt");
                            if (urlOfOldFile != null) {
                                File oldFile = new File(urlOfOldFile.toURI());
                                boolean success = oldFile.renameTo(newFile);
                                if (!success) {
                                    Log.d(TAG,
                                            "Unable to move previous version of base_paths.txt. The base paths will have to be setup again.");
                                }

                            }
                        } catch (URISyntaxException e) {
                            Log.d(TAG, e.getMessage(), e);
                        }
                    }
                }

                if (new File(path.getFullPath()).createNewFile()) {
                    if (path == SystemPaths.DEFAULT_SETTINGS) {
                        DefaultSettings.createDefaultFile();
                    } else if (path == SystemPaths.PASSWORD) {
                        StringEncrypter.writePasswordToFile("");
                    } else if (path == SystemPaths.SUPPORTED_FILE_TYPES) {
                        Log.d(TAG, "*******createFiles***********path=" + path);
                      //SupportedFiletypeSettings.createDefaultFile();
                    } else if (path == SystemPaths.BASE_PATHS) {
                        BaseMediaPaths.createDefaultFile();
                    }
                }

            } catch (IOException e) {
                Log.d(TAG, e.getMessage(), e);
            } catch (EncryptionException e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a directory unless it already exists
     */
    private void createIfNotExists(String path) {
        Log.d(TAG, "--->create root directory:" + path);
        File f = new File(path);
        if (f.exists() == false) {
            Log.d(TAG, "--->mkdirs");
            f.mkdirs();
        }
    }

    static class ServiceStub extends IAisinoService.Stub {
        WeakReference<AisinoService> mService;

        ServiceStub(AisinoService service) {
            mService = new WeakReference<AisinoService>(service);
        }

        @Override
        public void open() throws RemoteException {
            mService.get().open();

        }
        @Override
        public void close() throws RemoteException {
            mService.get().close();

        }

    }
    private final IBinder mBinder = new ServiceStub(this);

    // add by zhangdawei
    public void openFile(String filePath){
        File file = new File(filePath);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        String type = getMIMEType(file);

        intent.setDataAndType(Uri.fromFile(file), type);
        startActivity(intent);
    }

 // add by gaoqi 20120120
    public void openUrl(String url){

        Intent viewIntent = new Intent("android.intent.action.VIEW",Uri.parse(url));
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(viewIntent);
    }

    private String getMIMEType(File file)
    {
        String type = "*/*";
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        if(dotIndex < 0){
            return type;
        }
        String end=fileName.substring(++dotIndex,fileName.length()).toLowerCase();
        if(end == "") {
			return type;
		}

		MimeTypeMap mimeType = MimeTypeMap.getSingleton();
		type = mimeType.getMimeTypeFromExtension(end);
		if(type == null) {
			type = "*/*";
        }
        return type;
    }

}
