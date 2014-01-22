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

package org.amote.client;

import org.amote.utils.SensorHub;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Program entry, the main activity of amoteclient.
 * @author aisino
 *
 */
public class MainActivity extends Activity implements View.OnClickListener {

	private final String TAG = "MainActivity";
	private long startTime = 0;
	ActivityUtil mUtil = null;
	ImageButton remoteBtn = null;
	ImageButton touchpadBtn = null;
	ImageButton gameBtn = null;
	ImageButton findSeverBtn = null;
	ImageButton tvFileBtn = null;
	ImageButton webBtn = null;
	ImageButton helpBtn = null;
	View viewtempTop = null;
	View viewtempBottom = null;
	View viewtempLeft = null;
	View viewtempRight = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUtil = new ActivityUtil();
		mUtil.onCreate(savedInstanceState, this);
		setContentView(R.layout.main_act);
		remoteBtn = (ImageButton) findViewById(R.id.main_remote);
		remoteBtn.setOnClickListener(this);
		touchpadBtn = (ImageButton) findViewById(R.id.main_touchpad);
		touchpadBtn.setOnClickListener(this);
		gameBtn = (ImageButton) findViewById(R.id.main_game);
		gameBtn.setOnClickListener(this);
		findSeverBtn = (ImageButton) findViewById(R.id.main_findserver);
		findSeverBtn.setOnClickListener(this);
		tvFileBtn = (ImageButton) findViewById(R.id.main_tv_file);
		tvFileBtn.setOnClickListener(this);
		webBtn = (ImageButton) findViewById(R.id.main_web);
		webBtn.setOnClickListener(this);
		helpBtn = (ImageButton) findViewById(R.id.main_help);
		helpBtn.setOnClickListener(this);
		TextView tv = (TextView) findViewById(R.id.version);
		tv.setText(getAppVersionName(this));
		
		Display display = getWindowManager().getDefaultDisplay();
		float mScreenWidth = (float) display.getWidth();
		float mScreenHeight = (float) display.getHeight();
		float vert = 0.0f;
		viewtempTop = (View) findViewById(R.id.vtcl_top);
		viewtempBottom = (View) findViewById(R.id.vtcl_bottom);
		viewtempLeft = (View) findViewById(R.id.hrztl_left);
		viewtempRight = (View) findViewById(R.id.hrztl_right);
		
		if (mScreenHeight / mScreenWidth > (float)4/(float)3 ) {
			vert = (mScreenHeight * 100 / (mScreenWidth * 4 / 3) - 100) / 2;	
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, 0);
			params.weight = vert;
			viewtempTop.setLayoutParams(params);
			viewtempBottom.setLayoutParams(params);
		} else {
			vert = (mScreenWidth * 100 / (mScreenHeight * 3 / 4) - 100) / 2;
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					0, LayoutParams.FILL_PARENT);
			params.weight = vert;
			viewtempLeft.setLayoutParams(params);
			viewtempRight.setLayoutParams(params);
		}

		if( SensorHub.getInstance() == null){
		    SensorHub.getInstance(getApplicationContext());
		}
	}

	@Override
	public void onClick(final View v) {
		mUtil.startActivity(v);
	}

	public static String getAppVersionName(Context context) {    
	    String versionName = "";    
	    try {    
	        // ---get the package info---    
	        PackageManager pm = context.getPackageManager();    
	        PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);    
	        versionName = pi.versionName;    
	        if (versionName == null || versionName.length() <= 0) {    
	            return "";    
	        }    
	    } catch (Exception e) {    
	        Log.e("VersionInfo", "Exception", e);    
	    }
	    versionName = "Ver: " + versionName;
	    return versionName;    
	} 
	
//	@Override
//	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
//
//	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
//
//	        final long currentTime = System.currentTimeMillis();
//	        if (currentTime - startTime > 2000) {
//	        	Toast.makeText(this, getString(R.string.exit_msg), Toast.LENGTH_SHORT).show();
//	        	startTime = currentTime;
//	        	Log.i(TAG, "main activity back once!");
//	        } else {
//	        	Log.i(TAG, "main activity finished!");
//		    	this.finish();
////		    	System.exit(0);
//		    }
//	    }
//        return true;
//	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		return mUtil.onCreateDialog(id);
	}

	@Override
	protected void onDestroy() {
        if (SensorHub.getInstance() != null) {
            SensorHub.getInstance().stopSendingDataThread();
        }
	    super.onDestroy();
	}

}
