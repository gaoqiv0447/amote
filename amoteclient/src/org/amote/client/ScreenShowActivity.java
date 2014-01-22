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

import org.amote.utils.DatagramUtils;
import org.amote.utils.SensorHub;
import org.gmote.common.MotionEventStruct.PointerCoordsT;
import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.UdpPacketTypes;
import org.gmote.common.MotionEventStruct;
import org.gmote.common.packet.SimplePacket;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.MotionEventPacket;
import org.gmote.common.packet.ScreenshotPacket;
import org.gmote.common.packet.ScreenshotPacketReq;
import org.gmote.common.packet.PluginPacket;
import org.amote.client.android.ScreenInfo;
import org.amote.client.panel.Panel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.MotionEvent.PointerCoords;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;



public class ScreenShowActivity extends Activity implements BaseActivity, OnClickListener{

    private final static String TAG = "ScreenShowActivity";
    private boolean DEBUG_S = false;
	private static final String SENSOR_USER_GUIDE = "sensor_user_guide";
	private static final int DIALOG_SENSOR_GUIDE = 0;
	private static final int DIALOG_SENSOR_PLUGIN_RESULT_F = 1;  // install failed
	private static final int DIALOG_SENSOR_PLUGIN_RESULT_S = 2;  // install success
	
	private boolean isUserGuide = true;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;

    private int serverUdpPort;
    private ActivityUtil mUtil = null;
    private boolean sendByUDP = true;
    private ImageView mScreenView;
    private ImageButton mCaptureButton;
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int srcWidth = 0;
    private int srcHeight = 0;
    private long scalX;
    private long scalY;

    private Remote remoteInstance;
    private int index = 0;
    private long start_t = 0;
    private int []lastLocation;
    private int down_x;
    private int down_y;
    private boolean isCapture = false;

    /**
     * Util class to send datagram.
     */
    private DatagramUtils mUtils;
    /**
     * Lock to keep screen wake.
     */
    private WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screenshow);
        mUtil = new ActivityUtil();
        mUtil.onCreate(savedInstanceState, this);

        Display display = getWindowManager().getDefaultDisplay();
        mScreenWidth = display.getWidth();
        mScreenHeight = display.getHeight();
		srcWidth = ScreenInfo.ServerWidth;
		srcHeight = ScreenInfo.ServerHeight;
        lastLocation = new int[] {0,0,0,0};
        remoteInstance = Remote.getInstance();

        ImageButton mShowMenu = (ImageButton) findViewById(R.id.capture_button);
        mShowMenu.setOnClickListener(this);
        ImageButton mHome = (ImageButton)findViewById(R.id.main_button);
        mHome.setOnClickListener(this);
        ImageButton mpad = (ImageButton)findViewById(R.id.touch_pad);
        mpad.setOnClickListener(this);

        ImageButton mgame = (ImageButton) findViewById(R.id.game_control);
        mgame.setOnClickListener(this);
        mgame.setVisibility(View.GONE);
//        mShowMenu.bringToFront();
//        mShowMenu.setOnTouchListener(new OnTouchListener() {
//            int[] temp = new int[] { 0, 0 };
//
//            public boolean onTouch(View v, MotionEvent event) {
//
//                int eventaction = event.getAction();
//
//                int x = (int) event.getRawX();
//                int y = (int) event.getRawY();
//
//                switch (eventaction) {
//                case MotionEvent.ACTION_DOWN:
//                    down_x = (int)event.getRawX();
//                    down_y = (int)event.getRawX();
//                    temp[0] = (int) event.getX() ;
//                    temp[1] = y - v.getTop();
//                    break;
//
//                case MotionEvent.ACTION_MOVE:
//                    v.layout(x - temp[0], y - temp[1], x + v.getWidth() - temp[0], y - temp[1] + v.getHeight());
//
//
//                    lastLocation[0] = x - temp[0];
//                    lastLocation[1] = y - temp[1];
//                    lastLocation[2] = x + v.getWidth() - temp[0];
//                    lastLocation[3] =  y - temp[1] + v.getHeight();
//                    v.postInvalidate(); //redraw
//
//                    break;
//                case MotionEvent.ACTION_UP:
//                    int up_x = (int)event.getRawX();
//                    int up_y = (int)event.getRawX();
//                    if(Math.abs(up_x-down_x)<=3 && Math.abs(up_y-down_y)<=3 && !isCapture) {
//                        mUtil.send(new ScreenshotPacketReq(mScreenWidth,mScreenHeight));
//                        mUtil.showProgressDialog(getString(R.string.screen_shot));
//                        isCapture = true;
//                    }
//                    isCapture = false;
//                    break;
//                }
//                return false;
//            }
//        });
        mScreenView = (ImageView) findViewById(R.id.screenshot_imageview);
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire();

        if(SensorHub.getInstance() == null) {
            SensorHub.getInstance(getApplicationContext());
            SensorHub.getInstance().startOrStopSendData(true);
        }else{
            SensorHub.getInstance().startOrStopSendData(true);
        }
		
		// init sharedpreferences
		prefs = getSharedPreferences(GmoteClient.PREFS,MODE_WORLD_READABLE);
		editor = getSharedPreferences(GmoteClient.PREFS, MODE_WORLD_WRITEABLE).edit();
		
//		isUserGuide = prefs.getBoolean(SENSOR_USER_GUIDE,true);
//		if(isUserGuide) {
//			showDialog(DIALOG_SENSOR_GUIDE);
//		}
    }

    @Override
    public void onStart() {
      super.onStart();
      mUtils = DatagramUtils.instance();
      mUtil.onStart(this);
      serverUdpPort = Remote.getInstance().getServerUdpPort();
    }
    @Override
    public void onStop() {

      super.onStop();
      mUtil.onStop();
    }

    @Override
    protected void onDestroy() {
        SensorHub.getInstance().startOrStopSendData(false);
        mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public void onResume() {
      super.onResume();

      mUtil.onResume();
    }

    @Override
    public void onPause() {
      super.onPause();

      mUtil.onPause();
    }

    private Handler mHandler = new Handler(){
      public void dispatchMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    mUtil.send(new ScreenshotPacketReq(mScreenWidth, mScreenHeight));
                    mUtil.showProgressDialog(getString(R.string.screen_shot));
                    break;
                case 2:
                    mWakeLock.release();
                    break;
				case 3:
					mUtil.send(new SimplePacket(Command.SENSOR_PLUGIN_INSTALL));
					mUtil.showProgressDialog(getString(R.string.plugin_install_progress));
					break;
            }

      };
    };

    public void handleReceivedPacket(AbstractPacket tempReply) {
        if(tempReply.getCommand() == Command.SCREENSHOT_REPLY) {
            if(DEBUG_S) {
                Log.d(TAG,"====yes,i have received your data,command: " + tempReply.toString());
            }

            ScreenshotPacket screenshotpacket = (ScreenshotPacket) tempReply;
            byte[] imageByte = screenshotpacket.getByte();
            if (imageByte == null) return;
            Bitmap bm = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
            // calculate the factor
            // srcWidth = bm.getWidth();
            // srcHeight = bm.getHeight();
            // if (mScreenWidth != 0 && mScreenHeight != 0) {
                // scalX = (long)(srcWidth / mScreenWidth);
                // scalY = (long)(srcHeight / mScreenHeight);
            // }
            if(DEBUG_S) {
                Log.d(TAG,"====width: " + String.valueOf(bm.getWidth()));
                Log.d(TAG,"====height: " + String.valueOf(bm.getHeight()));
            }

            if (bm != null )
                mScreenView.setImageBitmap(bm);

            mHandler.sendEmptyMessage(0);

        }else if(tempReply.getCommand() == Command.SENSOR_PLUGIN_INSTALL_RESULT) {
			PluginPacket pluginpacket = (PluginPacket) tempReply;
			boolean re = pluginpacket.getPluginResult();
			if(re) {
				showDialog(DIALOG_SENSOR_PLUGIN_RESULT_S);
			}else {
				showDialog(DIALOG_SENSOR_PLUGIN_RESULT_F);
			}
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_MOVE) {
            index++;
            if(index%2 == 0) {
                if(index > 1024) index = 0;
                return true;
            }
        }

        int count = event.getPointerCount();
        int[] mPointerIds = new int[count];
        for(int i=0;i<count;i++) {
            mPointerIds[i] = event.getPointerId(i);
        }

        //PointerCoords[] mPointerCoords = new PointerCoords[count];
        PointerCoordsT[] mPointerCoordsT = new PointerCoordsT[count];
        for(int i=0;i<count;i++) {
            PointerCoords out = new PointerCoords();
            event.getPointerCoords(i, out);

            try {
                PointerCoordsT pt = new PointerCoordsT(
                        out.orientation,
                        out.pressure,
                        out.size,
                        out.toolMajor,
                        out.toolMinor,
                        out.touchMajor,
                        out.touchMinor,
                        out.x * srcWidth / mScreenWidth,
                        out.y * srcHeight / mScreenHeight);
                Log.i(TAG, "out.orientation:" + out.orientation + ", " +
                		"out.pressure:" + out.pressure + ", " +
                		"out.size:" + out.size + ", " +
                		"out.toolMajor:" + out.toolMajor + ", " +
                		"out.toolMinor:" + out.toolMinor + ", " +
                		"out.touchMajor:" + out.touchMajor + ", " +
                		"out.touchMinor:" + out.touchMinor + ", " +
                		"out.x:" + out.x * srcWidth / mScreenWidth + ", " +
                		"out.y:" + out.y * srcHeight / mScreenHeight);

//                mPointerCoords[i] = out;
                mPointerCoordsT[i] = pt;

            }catch(Exception e) {

            }
        }

        MotionEventStruct new_e = new MotionEventStruct(
                event.getDownTime()-SystemClock.uptimeMillis(),
                event.getEventTime()-SystemClock.uptimeMillis(),
                 event.getAction(),
                 event.getPointerCount(),
                 mPointerIds,
                 mPointerCoordsT,
                 event.getMetaState(),
                 event.getXPrecision() * srcWidth / mScreenWidth,
                 event.getYPrecision() * srcHeight / mScreenHeight,
                 event.getDeviceId(),
                 event.getEdgeFlags(),
                 event.getSource(),
                 event.getFlags());
        
        long downTime = event.getDownTime()-SystemClock.uptimeMillis();
        long eventTime = event.getEventTime()-SystemClock.uptimeMillis();

        Log.i(TAG, "downTime:" + downTime + ", " +
        		"eventTime:" + eventTime + ", " +
        		"getAction:" + event.getAction() + ", " +
        		"getPointerCount:" + event.getPointerCount() + ", " +
        		"getMetaState:" + event.getMetaState() + ", " +
        		"getXPrecision:" + event.getXPrecision() * srcWidth / mScreenWidth + ", " +
        		"getYPrecision:" + event.getYPrecision() * srcHeight / mScreenHeight + ", " +
        		"mPointerIds:" + mPointerIds[0] + ", " +
        		"getDeviceId:" + event.getDeviceId()  + ", " +
        		"getEdgeFlags:" + event.getEdgeFlags()  + ", " +
        		"getSource:" + event.getSource()  + ", " +

        		"getFlags:" + event.getFlags()
        		);
//        Log.i(TAG, "downTime:" + downTime + ", " +
//        		"eventTime:" + eventTime + ", " +
//        		"getAction:" + event.getAction()
//        		);
        // mEvent = new_e;
        // do send event to server
        if(sendByUDP) {
            byte[] data = makeMotionEventPacket(new_e);
            mUtils.makeDatagramPacket(data);

        }else {
            mUtil.send(new MotionEventPacket(new_e));
        }
        return true;
    }

    private synchronized byte[] makeMotionEventPacket(MotionEventStruct e) {
        byte[] sendBuff = null;
        ObjectOutputStream oout = null;
        ByteArrayOutputStream bout = null;
        if (e != null) {
            try {
                     bout = new ByteArrayOutputStream();
                     bout.write(UdpPacketTypes.MOTION_EVENT.getId());
                     oout = new ObjectOutputStream(bout);
                     oout.writeObject(e);
                     oout.flush();
                     sendBuff = bout.toByteArray();
                     if(DEBUG_S) {
                         if(sendBuff[0] == UdpPacketTypes.MOTION_EVENT.getId()) {
                             Log.d(TAG,"===== buffer[0] is MOTION_EVENT");
                             Log.d(TAG,"===Motionevent:"+ String.valueOf(sendBuff[0]));
                         }
                     }

            }catch(Exception ex) {
                     ex.printStackTrace();
            }finally{
                try {
                    if (null != oout) {
                        oout.close();
                        oout = null;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    if (null != bout) {
                        bout.close();
                        bout = null;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }
        return sendBuff;
    }

    @Override
    public void onClick(View v) {
       if(v.getId() == R.id.capture_button){
           mHandler.sendEmptyMessage(1);
       }else if(v.getId() == R.id.main_button){
           mUtil.startActivityByClass(HomeActivity.class);
       }else if(v.getId() == R.id.touch_pad){
           mUtil.startActivityByClass(Touchpad.class);
       }
    }
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case DIALOG_SENSOR_GUIDE:
				return new AlertDialog.Builder(ScreenShowActivity.this)
					.setTitle(R.string.user_guide_tilte)
					.setMessage(R.string.user_guide_msg)
					.setPositiveButton(R.string.user_guide_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SensorPluginInstall();
						}
					})
					.setNegativeButton(R.string.user_guide_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SensorPluginInstallCancel();
						}
					})
					.create();
			case DIALOG_SENSOR_PLUGIN_RESULT_S:
				return new AlertDialog.Builder(ScreenShowActivity.this)
					.setTitle(R.string.user_guide_tilte)
					.setMessage(R.string.plugin_install_sucess_msg)
					.setPositiveButton(R.string.plugin_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					})
					.create();
			case DIALOG_SENSOR_PLUGIN_RESULT_F:
				return new AlertDialog.Builder(ScreenShowActivity.this)
					.setTitle(R.string.user_guide_tilte)
					.setMessage(R.string.plugin_install_error_msg)
					.setPositiveButton(R.string.plugin_reinstall, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SensorPluginInstall();
						}
					})
					.setNegativeButton(R.string.user_guide_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SensorPluginInstallCancel();
						}
					})
					.create();
		}
		return null;
	}
	
	private void SensorPluginInstall() {
		//send socket
		
		mHandler.sendEmptyMessage(3);
		isUserGuide = false;
		// save
		editor.putBoolean(SENSOR_USER_GUIDE, isUserGuide);
		editor.commit();
	}
	
	private void SensorPluginInstallCancel() {
		isUserGuide = true;
		editor.putBoolean(SENSOR_USER_GUIDE, isUserGuide);
		editor.commit();
		finish();
	}
}