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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import org.amote.client.android.ScreenInfo;
import org.amote.utils.DatagramUtils;
import org.amote.utils.SensorHub;
import org.gmote.common.MotionEventStruct;
import org.gmote.common.MotionEventStruct.PointerCoordsT;
import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.RemoteEvent;
import org.gmote.common.Protocol.UdpPacketTypes;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.RemoteEventPacket;
import org.gmote.common.packet.SimplePacket;
import org.gmote.common.packet.TextPacket;

/**
 * Remote Page Acitvity. Simple Remote function, eg: up, down, left, right, home,
 * menu, search, back, volume up, volume down, etc.
 * @author aisino
 *
 */
public class HomeActivity extends Activity implements BaseActivity, OnClickListener
{

	private final String TAG = "HomeActivity";
	private final static int NUM_FLING_POINTS = 9;

	ActivityUtil mUtil = null;
	ImageButton centerBtn = null;
	ImageButton upBtn = null;
	ImageButton downBtn = null;
	ImageButton leftBtn = null;
	ImageButton rightBtn = null;

	ImageButton volupBtn = null;
	ImageButton voldownBtn = null;
//	ImageButton volmuteBtn = null;

	ImageButton homeBtn = null;
	ImageButton menuBtn = null;
	ImageButton backBtn = null;
	ImageButton searchBtn = null;
//	ImageButton shutdownBtn = null;

	ImageButton keyBtn = null;
	ImageButton CloseBtn = null;

	private ImageButton mHomeSwitchButton;
	private ImageButton mRemoteSwitchButton;
	private ImageButton mTouchSwitchButton;
	private ImageButton mGameSwitchButton;
	
	View viewtempTop = null;
	View viewtempBottom = null;
	View viewtempLeft = null;
	View viewtempRight = null;

	private long startTime = 0;
	ListView mList;
	PopupWindow popupWindow;
	EditText editText;
	int iPopupHeight;
	private int index = 0;
	private KeyInputPopupWindow keyInputPopupWindow;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Remote.getInstance();
		mUtil = new ActivityUtil();
		mUtil.onCreate(savedInstanceState, this);
		
		setContentView(R.layout.remote_act);
		
		// need to decouping to Sensor, Now this statement must be invoke, touch event could enable.
		if(SensorHub.getInstance() == null) {
			SensorHub.getInstance(getApplicationContext());
		}
		SensorHub.getInstance().startOrStopSendData(true);
		
		Display display = getWindowManager().getDefaultDisplay();
		float mScreenWidth = (float) display.getWidth();
		float mScreenHeight = (float) display.getHeight();
		mScreenHeight = mScreenHeight * 400 / 528;
		float vert = 0.0f;
		viewtempTop = (View) findViewById(R.id.vtcl_top);
		viewtempBottom = (View) findViewById(R.id.vtcl_bottom);
		viewtempLeft = (View) findViewById(R.id.hrztl_left);
		viewtempRight = (View) findViewById(R.id.hrztl_right);
		
		if (mScreenHeight / mScreenWidth > (float)3/(float)2 ) {
			vert = (mScreenHeight * 100 / (mScreenWidth * 3 / 2) - 100) / 2;	
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, 0);
			params.weight = vert;
			viewtempTop.setLayoutParams(params);
			viewtempBottom.setLayoutParams(params);
		} else {
			vert = (mScreenWidth * 100 / (mScreenHeight * 2 / 3) - 100) / 2;
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					0, LayoutParams.FILL_PARENT);
			params.weight = vert;
			viewtempLeft.setLayoutParams(params);
			viewtempRight.setLayoutParams(params);
		}
		
		keyInputPopupWindow = new KeyInputPopupWindow(HomeActivity.this, mUtil);
		centerBtn = (ImageButton) findViewById(R.id.new_media_enter);
		centerBtn.setOnClickListener(this);

		upBtn = (ImageButton) findViewById(R.id.new_media_up);
		upBtn.setOnClickListener(this);
		downBtn = (ImageButton) findViewById(R.id.new_media_down);
		downBtn.setOnClickListener(this);
		leftBtn = (ImageButton) findViewById(R.id.new_media_left);
		leftBtn.setOnClickListener(this);
		rightBtn = (ImageButton) findViewById(R.id.new_media_right);
		rightBtn.setOnClickListener(this);

		volupBtn = (ImageButton) findViewById(R.id.new_media_volup);
		volupBtn.setOnClickListener(this);
		voldownBtn = (ImageButton) findViewById(R.id.new_media_voldown);
		voldownBtn.setOnClickListener(this);

		homeBtn = (ImageButton) findViewById(R.id.new_media_home);
		homeBtn.setOnClickListener(this);
		menuBtn = (ImageButton) findViewById(R.id.new_media_menu);
		menuBtn.setOnClickListener(this);
		backBtn = (ImageButton) findViewById(R.id.new_media_back);
		backBtn.setOnClickListener(this);
		searchBtn = (ImageButton) findViewById(R.id.new_media_search);
		searchBtn.setOnClickListener(this);

		// for send text to server
		keyBtn = (ImageButton) findViewById(R.id.new_media_switch);
		keyBtn.setOnClickListener(this);

		CloseBtn = (ImageButton) findViewById(R.id.new_media_close);
		CloseBtn.setOnClickListener(this);

	    mHomeSwitchButton = (ImageButton) findViewById(R.id.home_switch);
		mHomeSwitchButton.setOnClickListener(this);
		mRemoteSwitchButton = (ImageButton) findViewById(R.id.main_control_switch);
		mRemoteSwitchButton.setOnClickListener(this);
		mRemoteSwitchButton.setBackgroundResource(R.drawable.remote_click_switch);
		mTouchSwitchButton = (ImageButton) findViewById(R.id.touch_pad_switch);
		mTouchSwitchButton.setOnClickListener(this);
		mGameSwitchButton = (ImageButton) findViewById(R.id.game_control_switch);
		mGameSwitchButton.setOnClickListener(this);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		keyInputPopupWindow.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void handleReceivedPacket(AbstractPacket reply) {
		if(reply.getCommand() == Command.KEYBOARD) {
			keyInputPopupWindow.startPopupWindow();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return mUtil.onCreateDialog(id);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
		case R.id.new_media_up:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_UP));
			break;
		case R.id.new_media_down:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_DOWN));
			break;
		case R.id.new_media_left:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_LEFT));
			break;
		case R.id.new_media_right:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_RIGHT));
			break;
		case R.id.new_media_enter:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_ENTER));
			break;
		case R.id.new_media_home:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_HOME));
			break;
		case R.id.new_media_menu:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_MENU));
			break;
		case R.id.new_media_back:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_BACK));
			break;
		case R.id.new_media_search:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_SEARCH));
			break;
		case R.id.new_media_voldown:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_VOLDOWN));
			break;
		case R.id.new_media_volup:
			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_VOLUP));
			break;
		case R.id.home_switch:
			this.finish();
			break;
		case R.id.main_control_switch:
			break;
		case R.id.touch_pad_switch:
			intent = new Intent(this,Touchpad.class);
			this.startActivity(intent);
			this.finish();
			break;
		case R.id.game_control_switch:
			intent = new Intent(this,ScreenShowActivity.class);
			this.startActivity(intent);
			this.finish();
			break;
			
//		case R.id.new_media_power:
//			// test for sensor Activity
////			Intent intent = new Intent();
////			intent.setClass(this, SensorActivity.class);
////			startActivity(intent);
//			this.finish();
//			// end test, need remove 
//			break;
//		case R.id.new_media_mute:
//			mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_VOLMUTE));
//			break;
		case R.id.new_media_switch:
			keyInputPopupWindow.startPopupWindow();
			break;
		case R.id.new_media_close:
			this.finish();
			break;
		default:
			Log.i(TAG, "default onClick");
			break;
		}
	}

}
