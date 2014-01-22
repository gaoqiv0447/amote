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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
//import android.net.wifi.WifiManager;
import java.net.SocketException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {

	private int mCount = 15;
//	private WifiManager wifiManager;
	private Context mContext;
	private Boolean isAutoStart;
	private Timer mTimer;
	private TimerTask mTimerTask;

    @Override
    public void onReceive(Context context, Intent intent) {
		mContext = context;
		SharedPreferences sharedpref = PreferenceManager
            .getDefaultSharedPreferences(context.getApplicationContext());
		SharedPreferences.Editor editor = sharedpref.edit();
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d("BootBroadcastReceiver", "**********************************Recevie ACTION_BOOT_COMPLETED");

            isAutoStart = sharedpref.getBoolean(Settings.SETTINGS_AUTO_START_KEY,true);
//			wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

			mTimer = new Timer(false);
			mTimerTask = new TimerTask(){
				public void run(){
					try{
						if(ServerUtil.findAllLocalIpAddresses(true).size() != 0){
							Log.d("BootBroadcastReceiver","======internet is enable,AisinoService will start!");
							Intent newIntent = new Intent(mContext, AisinoService.class);
							newIntent.putExtra("start_from_boot", true);
							mContext.startService(newIntent);
							mCount = 0;
						}
						mCount--;
						Log.d("BootBroadcastReceiver","======internet is enable,AisinoService will start!" +mCount);
					    if(mCount < 0){
				                mTimer.cancel();
				         }
					}

					catch (SocketException e) {
						e.printStackTrace();
					}
				}
			};
            if(isAutoStart) {
				mTimer.schedule(mTimerTask, 10000,5000);
			}

        }else if(intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
			Settings.getInstance(PreferenceManager.getDefaultSharedPreferences(mContext));
			if(Settings.getKeyBrowsePlugin() || Settings.getKeySensorPlugin()) {
				Settings.setKeyShutDown(true);
				Settings.writeBack();
				Log.d("BootBroadcastRe","===device shutdown flag has be writed to sharedpref.");
				// SETTING_SHUT_DOWN shoud be false,when ...
			}
			Settings.releaseInstance();
			
        }

    }
}
