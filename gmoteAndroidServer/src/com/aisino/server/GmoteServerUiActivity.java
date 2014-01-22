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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.gmote.common.FileInfo;

import com.aisino.awt.MouseInfo;
import org.gmote.common.ScreenInfo;
import com.aisino.server.settings.BaseMediaPaths;
import com.aisino.server.settings.DefaultSettings;
import com.aisino.server.settings.PreferredPorts;
import com.aisino.server.settings.StartupSettings;
import com.aisino.server.settings.StartupSettingsEnum;
import com.aisino.server.settings.SystemPaths;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class GmoteServerUiActivity extends Activity {
    /** Called when the activity is first created. */
    private final static String TAG = "GmoteServerUiActivity";
    private final static String AUTO_START = "auto_start";
    private final static int SHOW_SETTING_PWD = 0;

    private final static int ENCRYPTION_EXCEPTION = 1;
    private final static int CHANGE_PASSWORD = 2;
    private final static int SETTING_AUTO_START = 3;
    private final static int CHECK_PASSWORD = 4;

	private final static int REQ_MAIN_SETTINGS = 0;

	private final static String DEFAULT_PASSWORD = "1234";

    View mPWdEntryView;
    View change_pwd_view;

    boolean mResult = false;

    int downCount = 0;

    float mOldx = 0;

    float mOldy = 0;

    float mNewx = 0;

    float mNewy = 0;

    boolean flag = true;

    private NotificationManager mNotificationManager;

    private IAisinoService mService;

    private boolean mToken;
    private boolean isAutoStart = false;

    private TextView mTextView;
    private TextView mUdpText;
    private TextView mClientView;
    private ListView mList;
//    private ImageView qrcodeImg;
    SimpleAdapter mListAdapter;
    ArrayList<HashMap<String,String>> mIplist = new ArrayList<HashMap<String,String>>();
    static Socket server;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "---onCreate android.os.Process.myPid()="+android.os.Process.myPid());
        initializeUi();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AisinoService.AMOTE_NO_NETWORK_ACTION);
        filter.addAction(AisinoService.AMOTE_NETWORK_ADDRESS_ACTION);
        filter.addAction(AisinoService.AMOTE_NETWORK_UDPPORT_ACTION);
        filter.addAction(AisinoService.AMOTE_AUTHENTICATION_SUCCESS_ACTION);
        filter.addAction(AisinoService.AMOTE_AUTHENTICATION_FAIL_ACTION);
        filter.addAction(AisinoService.AMOTE_HANDLE_STARTUP_ACTION);
        Intent intent = new Intent(this, AisinoService.class);

        if(savedInstanceState!=null && savedInstanceState.getBoolean("start_from_boot")){
            Log.d(TAG, "---onCreate savedInstanceState!=null");
            handleStartupSettings();
        }
        intent.putExtra("start_from_boot", false);
        this.registerReceiver(mBroadcastRecever, filter);
        this.startService(intent);
        mToken = this.bindService(intent, conn,
                Context.BIND_AUTO_CREATE);
		getLocalAddsAndPorts();
		//
    }
    @Override
    protected void onResume() {

        super.onResume();
    }
    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "----->onDestroy()");
        mNotificationManager.cancel(R.drawable.icon);
        this.unbindService(conn);
        this.unregisterReceiver(mBroadcastRecever);
       // android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.main_setting:
            //GmoteServerUiActivity.this.showDialog(SETTING_AUTO_START);
			GmoteServerUiActivity.this.startActivityForResult(new Intent(this, SettingsActivity.class),REQ_MAIN_SETTINGS);
            break;
        case R.id.main_change_pwd:
            LayoutInflater factory = LayoutInflater.from(GmoteServerUiActivity.this);
            change_pwd_view = factory.inflate(R.layout.change_pwd_dialog, null);
            new AlertDialog.Builder(GmoteServerUiActivity.this)
                    .setTitle(getResources().getString(R.string.change_pwd_title))
                    .setView(change_pwd_view)
                    .setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText pwdEdit = (EditText) change_pwd_view.findViewById(R.id.new_pwd_text);
                            EditText pwdEdit1 = (EditText) change_pwd_view.findViewById(R.id.confirm_pwd_text);
                            String password = pwdEdit.getText().toString();
                            String passwordConfirmation = pwdEdit1.getText().toString();
                            if (password.equals(passwordConfirmation)) {
                                try {
                                    StringEncrypter.writePasswordToFile(password);
                                } catch (EncryptionException e1) {
                                    Log.d(TAG, "------>" + e1.getMessage());
                                }
                            } else {
                                GmoteServerUiActivity.this.showDialog(CHECK_PASSWORD);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
            break;
        }

        return true;
    }

	protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQ_MAIN_SETTINGS) {
			MouseUtil.setHandMouse();
			MouseUtil.setPointerMouse();
		}
    }

    private void initializeUi() {
        setContentView(R.layout.main);
        mTextView =(TextView) findViewById(R.id.content);
        mUdpText = (TextView)findViewById(R.id.udp_port);
        mClientView = (TextView)findViewById(R.id.connected_ip);
//        qrcodeImg = (ImageView) findViewById(R.id.qrcode_img);
        mList = (ListView)findViewById(R.id.list);
        mListAdapter = new SimpleAdapter(this,mIplist , R.layout.addr_info,new String[]{"address"}, new int[]{R.id.addr});
        mList.setAdapter(mListAdapter);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // miniTheWindow();
    }

    private void handleStartupSettings() {
        new Thread("StartupSettings") {
            @Override
            public void run() {
                Looper.prepare();
                StartupSettings settings = StartupSettings.instance();
                if (!settings.getSetting(StartupSettingsEnum.PASSWORD_SHOWN)) {
                    //showPasswordSettings(settings);
					try {
						StringEncrypter.writePasswordToFile(DEFAULT_PASSWORD);
						settings.setSetting(StartupSettingsEnum.PASSWORD_SHOWN, true);
					} catch (EncryptionException e1) {
						Log.d(TAG, "------>" + e1.getMessage());
						settings.setSetting(StartupSettingsEnum.PASSWORD_SHOWN, false);
					}
                }

                if (!settings.getSetting(StartupSettingsEnum.PATH_SHOWN)) {
                    showPathChooser();
                    settings.setSetting(StartupSettingsEnum.PATH_SHOWN, true);
                }

                handleExtraSettings(settings);
                Looper.loop();
            }

        }.start();
    }
    private void getLocalAddsAndPorts(){
        try {
            SystemPaths.ROOT_PATH = getFilesDir().getAbsolutePath();
            for (InetAddress address : ServerUtil.findAllLocalIpAddresses(true)) {
                String ip = address.getHostAddress();
                Integer port = PreferredPorts.instance().getPreferredPort(ip);
                if(port==null){
                    ip = ip + ":unknown";
                    continue;
                }else {
                    ip = ip +":" +port;
                }
                HashMap<String, String> map ;

                for(HashMap<String, String> m:mIplist){
                    map = new HashMap<String, String>();
                    if(ip.equals(m.get("address")))
                            break;
                    map.put("address", ip);
                    mIplist.add(map);
                }


               if(mIplist.size()==0){
                  map = new HashMap<String, String>();
                  map.put("address", ip);
                  mIplist.add(map);
              }
               mListAdapter.notifyDataSetChanged();
            }
        }
         catch (SocketException e) {
            e.printStackTrace();
        }

    }

    Handler mHandler = new Handler();

    boolean showPasswordSettings(final StartupSettings settings) {

        LayoutInflater factory = LayoutInflater.from(this);
        mPWdEntryView = factory.inflate(R.layout.pwd_form, null);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(mPWdEntryView);

        AlertDialog serverDialog = new AlertDialog.Builder(this).setView(scrollView)
                .setTitle(getString(R.string.set_password))
                .setPositiveButton(getString(R.string.ok_btn), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText pwdEdit = (EditText) mPWdEntryView.findViewById(R.id.pwd_edit);
                        EditText pwdEdit1 = (EditText) mPWdEntryView.findViewById(R.id.pwd_edit1);
                        String password = pwdEdit.getText().toString();
                        String passwordConfirmation = pwdEdit1.getText().toString();
                        if (password.equals(passwordConfirmation)) {
                            try {
                                StringEncrypter.writePasswordToFile(password);
                                settings.setSetting(StartupSettingsEnum.PASSWORD_SHOWN, true);
                            } catch (EncryptionException e1) {
                                Log.d(TAG, "------>" + e1.getMessage());
                                settings.setSetting(StartupSettingsEnum.PASSWORD_SHOWN, false);
                            }
                        } else {
                            showDialog(SHOW_SETTING_PWD);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel_btn), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mResult = false;
                        settings.setSetting(StartupSettingsEnum.PASSWORD_SHOWN, false);
                    }
                }).show();
        return mResult;
    }

    private void showPathChooser() {
        // TODO **************need to add shared dictionaries select window
        Log.d(TAG, "****showPathChooser*****");

        // BaseMediaPaths.getInstance().addPath("/mnt/sdcard/");

        loadBasePaths();

    }

    public void handleExtraSettings(StartupSettings settings) {
        if (!settings.getSetting(StartupSettingsEnum.POPUP_SHOWN)) {
            settings.setSetting(StartupSettingsEnum.POPUP_SHOWN, true);
        }

    }



    private void loadBasePaths() {
        // TODO need to display the shared dictionaries

        Log.d(TAG, "****loadBasePaths*****");
        List<FileInfo> paths = BaseMediaPaths.getInstance().getBasePaths();
        for (FileInfo path : paths) {

        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SHOW_SETTING_PWD:
                return new AlertDialog.Builder(this).setTitle(R.string.alert)
                        .setMessage(R.string.password_diff)
                        .setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // TODO****************
                            }
                        }).create();
            case ENCRYPTION_EXCEPTION:
                return new AlertDialog.Builder(this).setTitle(R.string.alert)
                        .setMessage("EncryptionException")
                        .setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // TODO****************
                            }
                        }).create();
            case CHECK_PASSWORD:
                return new AlertDialog.Builder(this).setTitle(R.string.alert)
                        .setMessage(getString(R.string.password_error_info))
                        .setPositiveButton(getString(R.string.ok_btn), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).create();
        }

        return super.onCreateDialog(id);
    }

    OnMultiChoiceClickListener multiClick = new OnMultiChoiceClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            Log.d(TAG,"auto start:" + String.valueOf(isChecked));
            isAutoStart = isChecked;
        }

    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
            new AlertDialog.Builder(GmoteServerUiActivity.this)
                    .setTitle(getResources().getString(R.string.dialog_title))
                    .setMessage(getResources().getString(R.string.dialog_message))
//                    .setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            try {
//                                mService.close();
//                            } catch (RemoteException e) {
//                                // TODO Auto-generated catch block
//                                e.printStackTrace();
//                            }  finally {
//                                GmoteServerUiActivity.this.finish();
//                            }
//
//                        }
//                    })
                    .setPositiveButton(R.string.hide_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            miniTheWindow();

                        }
                    })
                    .setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void miniTheWindow() {
        Intent g_intent;
        g_intent = new Intent(Intent.ACTION_MAIN);
        g_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        g_intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(g_intent);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            // we need to be able to bind again, so unbind
            Log.i(this.getClass().getName(), "ServiceConnection");
            mService = IAisinoService.Stub.asInterface(obj);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    BroadcastReceiver mBroadcastRecever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "---------->mBroadcastRecever onReceive action=" +intent.getAction());
            String action = intent.getAction();
            if(action.equals(AisinoService.AMOTE_NO_NETWORK_ACTION) ){
                showAlertDialog(R.string.dialog_title,R.string.no_network_message,true);
            }else if(action.equals(AisinoService.AMOTE_NETWORK_ADDRESS_ACTION)){
                String str = intent.getStringExtra("argstr");
                int inter = intent.getIntExtra("argint",0);
                if(inter==0){
                    str = str+":unknown";
                }else{
                    str = str+":"+inter;
                }
                HashMap<String, String> map;
                for(HashMap<String, String> m:mIplist){
                   map = new HashMap<String, String>();
                    if(str.equals(m.get("address")))
                            break;
                    map.put("address", str);
                    mIplist.add(map);
                }

                if(mIplist.size()==0){
                    map = new HashMap<String, String>();
                    map = new HashMap<String, String>();
                    map.put("address", str);
                    mIplist.add(map);
                }

                mListAdapter.notifyDataSetChanged();
            }else if(action.equals(AisinoService.AMOTE_NETWORK_UDPPORT_ACTION)){
                mUdpText.setText(""+intent.getIntExtra("argint", 0));
            }else if(action.equals(AisinoService.AMOTE_AUTHENTICATION_FAIL_ACTION)){
                String msg = getResources().getString(R.string.connect_fail_message)+" "+intent.getStringExtra("argstr");
                mClientView.setText(msg);
            }else if(action.equals(AisinoService.AMOTE_AUTHENTICATION_SUCCESS_ACTION)){
                String msg = getResources().getString(R.string.connect_success_message)+" "+intent.getStringExtra("argstr");
                mClientView.setText(msg);
            }else if(action.equals(AisinoService.AMOTE_HANDLE_STARTUP_ACTION)){
                handleStartupSettings();
            }
        }
    };

    private void showAlertDialog(int title,int msg,final boolean exit) {
        new AlertDialog.Builder(this).setTitle(getResources().getString(title))
                .setMessage(getResources().getString(msg))
                .setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(exit){
                            GmoteServerUiActivity.this.finish();
                        }
                    }
                }).show();
    }
    private void showAlertDialog(String title,String msg,final boolean exit) {
        new AlertDialog.Builder(this).setTitle(title)
                .setMessage(msg)
                .setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(exit){
                            GmoteServerUiActivity.this.finish();
                        }
                    }
                }).show();
    }

}
