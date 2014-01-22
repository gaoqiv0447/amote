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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.amote.client.android.AisinoGestureDetector;
import org.amote.client.android.ScreenInfo;
import org.amote.utils.DatagramUtils;
import org.amote.utils.SensorHub;
import org.gmote.common.FileInfo;
import org.gmote.common.MotionEventStruct;
import org.gmote.common.MotionEventStruct.PointerCoordsT;
import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.MouseEvent;
import org.gmote.common.Protocol.RemoteEvent;
import org.gmote.common.Protocol.UdpPacketTypes;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.KeyboardEventPacket;
import org.gmote.common.packet.MouseClickPacket;
import org.gmote.common.packet.MouseWheelPacket;
import org.gmote.common.packet.RemoteEventPacket;
import org.gmote.common.packet.TextPacket;
import org.gmote.common.packet.RunFileReqPacket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.MotionEvent.PointerCoords;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Touchpad extends Activity implements BaseActivity,
		View.OnClickListener {

	private static final String DEBUG_TAG = "Gmote_Touchpad";
	private static final int FLING_MIN_DISTANCE = 100;
	private static final int FLING_MIN_VELOCITY = 200;
	private final static int NUM_FLING_POINTS = 9;
	private final static int NUM_FLING_TIMER = 15;

	AisinoGestureDetector gestureDetector = null;
	GestureDetector mGestureDetectorH;
	GestureDetector mGestureDetectorV;
	private DatagramUtils mUtils;

	private Display display;
	private int mScreenWidth;
	private int mScreenHeight;
	private int srcWidth;
	private int srcHeight;

	/** To send fling event interval */
	private  Timer mTimer;
	private  TimerTask mTimerTask;
	static private int indextemp = 0;

	ActivityUtil mUtil = null;
	ProgressDialog mDialog = null;

	View mContentView = null;
	View mPasswordEntryView = null;
	View SweepAreaH = null;
	View SweepAreaV = null;

	private float mX = 0;
	private float mY = 0;

	private long timeOfLastPosX = 0;
	private long timeOfLastNegX = 0;
	private long timeOfLastPosY = 0;
	private long timeOfLastNegY = 0;

	private float posXAcceleration = 0;
	private float negXAcceleration = 0;
	private float posYAcceleration = 0;
	private float negYAcceleration = 0;

	private static final float ACCELERATION_DECAY = (float) 0.1;
	private static final float MOUSE_TOUCH_DEFAULT = (float) -1.4;
	private static final float MOUSE_GRAVITATION_DEFAULT = (float) 0.5;

	protected static final String TAG = "Touchpad";

	private float mouseTouch = MOUSE_TOUCH_DEFAULT;
	private float mouseGravitation = MOUSE_GRAVITATION_DEFAULT;

	public static final String MOUSE_SENSITIVITY_SETTINGS_KEY = "mouse_sensitivity";
	public static final String MOUSE_ACCELERATION_SETTINGS_KEY = "mouse_acceleration";

	private Remote remoteInstance;

	// Object that we will wait on when the mouse is not moving.
	private Object waitForMouseMove = new Object();

	private int serverUdpPort;

	private ImageButton mLeftButton;
	private ImageButton mRightButton;
	private ImageButton mSendTextButton;
	private ImageButton mCloseButton;

	private ImageButton mHomeSwitchButton;
	private ImageButton mRemoteSwitchButton;
	private ImageButton mTouchSwitchButton;
	private ImageButton mGameSwitchButton;

	// add by zhangdawei
	private FileInfo fileInfo;
	// end add
	// define the sensors
	private SensorManager sensorManager;
	private Sensor grsensor;

	// Data
	private float[] g_acceleration = new float[3];
	private float[] start_acceleration = new float[3];
	private float[] stop_acceleration = new float[3];
	private static final float ALPHA = 0.15f;
	private int mMouseTouchPref;
	private int mMouseGravitationPref;
	private static final float DIVIDING_VALUE = (float) 0.1;
	private static final float SCALE_X = 16;
	private static final float SCALE_Y = 16;
	public static boolean isTrackBallOK = false;
	private boolean isPressed = false;

	private ImageButton new_mouse_sensor;
	private KeyInputPopupWindow keyInputPopupWindow;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mUtil = new ActivityUtil();
		mUtil.onCreate(icicle, this);
		mUtils = DatagramUtils.instance();
		remoteInstance = Remote.getInstance();
		
		// need to decouping to Sensor, Now this statement must be invoke, touch event could enable.
		if(SensorHub.getInstance() == null) {
			SensorHub.getInstance(getApplicationContext());
		}
		SensorHub.getInstance().startOrStopSendData(true);
		gestureDetector = new AisinoGestureDetector(this, gestureListener);
		gestureDetector.setIsLongpressEnabled(true);
		setContentView(R.layout.touchpad);
		keyInputPopupWindow = new KeyInputPopupWindow(Touchpad.this, mUtil);
		
		mContentView = findViewById(R.id.touchpad);
		mContentView.setFocusable(true);
		mContentView.setFocusableInTouchMode(true);
//		mTouchView = (TouchExampleView) findViewById(R.id.new_mouse_wheel);
//		mTouchView.setActivityUtil(mUtil);
		mLeftButton = (ImageButton) findViewById(R.id.new_mouse_left);
		mRightButton = (ImageButton) findViewById(R.id.new_mouse_right);
		mSendTextButton = (ImageButton) findViewById(R.id.new_mouse_key);
		mCloseButton = (ImageButton) findViewById(R.id.new_mouse_close);

		mLeftButton.setOnClickListener(this);
		mRightButton.setOnClickListener(this);
		mSendTextButton.setOnClickListener(this);
		mCloseButton.setOnClickListener(this);

		mHomeSwitchButton = (ImageButton) findViewById(R.id.home_switch_touchpad);
		mHomeSwitchButton.setOnClickListener(this);
		mRemoteSwitchButton = (ImageButton) findViewById(R.id.main_control_switch_touchpad);
		mRemoteSwitchButton.setOnClickListener(this);
		mTouchSwitchButton = (ImageButton) findViewById(R.id.touch_pad_switch_touchpad);
		mTouchSwitchButton.setOnClickListener(this);
		mTouchSwitchButton.setBackgroundResource(R.drawable.touch_click_switch);
		mGameSwitchButton = (ImageButton) findViewById(R.id.game_control_switch_touchpad);
		mGameSwitchButton.setOnClickListener(this);

		SweepAreaH = (View) findViewById(R.id.seek_bar_h);
		SweepAreaH.setLongClickable(true);
		SweepAreaH.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetectorH.onTouchEvent(event);
			}
		});

		SweepAreaV = (View) findViewById(R.id.seek_bar_v);
		SweepAreaV.setLongClickable(true);
		SweepAreaV.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetectorV.onTouchEvent(event);
			}
		});

		((SeekBar) findViewById(R.id.mouse_touch_seek))
				.setOnSeekBarChangeListener(new SeekBarListener());
		((SeekBar) findViewById(R.id.mouse_gravitation_seek))
				.setOnSeekBarChangeListener(new SeekBarListener());

		new_mouse_sensor = (ImageButton) findViewById(R.id.new_mouse_sensor);
		OnTouchListener listener = new ImageButton.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				if (event.getAction() == MotionEvent.ACTION_UP) {
					isPressed = false;
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					isPressed = true;
				}
				return false;
			}
		};
		new_mouse_sensor.setOnTouchListener(listener);
		// create sensors
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		grsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		sensorManager.registerListener(myListener, grsensor,
				SensorManager.SENSOR_DELAY_GAME);

		loadMouseSettings();
		MouseSendingThread mst = new MouseSendingThread();
		new Thread(mst).start();

		// add by zhangdawei 2012-01-13
		// sent packet to server,when the user click one file
		Intent intent = getIntent();
		fileInfo = (FileInfo) intent
				.getSerializableExtra(getString(R.string.file_type));

		if (fileInfo != null) {
			Log.d(DEBUG_TAG, "open: " + fileInfo.getFileName());
			mUtil.send(new RunFileReqPacket(fileInfo));
			Log.d(DEBUG_TAG, "send packet on onCreate");
		}
		// end add

		display = getWindowManager().getDefaultDisplay();
	    mScreenWidth = display.getWidth();
	    mScreenHeight = display.getHeight();

		mGestureDetectorH = new GestureDetector(new android.view.GestureDetector.OnGestureListener() {

			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			public void onShowPress(MotionEvent e) {
			}

			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				return false;
			}

			public void onLongPress(MotionEvent e) {
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				srcWidth = ScreenInfo.ServerWidth;
			    srcHeight = ScreenInfo.ServerHeight;

				if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
						&& Math.abs(velocityX) > FLING_MIN_VELOCITY) {
					horizontalFling(e1, e2);

				} else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
						&& Math.abs(velocityX) > FLING_MIN_VELOCITY) {
					horizontalFling(e1, e2);
				}
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}
		});

		mGestureDetectorV = new GestureDetector(new android.view.GestureDetector.OnGestureListener() {

			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			public void onShowPress(MotionEvent e) {
			}

			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				return false;
			}

			public void onLongPress(MotionEvent e) {
			}

			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				srcWidth = ScreenInfo.ServerWidth;
			    srcHeight = ScreenInfo.ServerHeight;

				if (e1.getY() - e2.getY() > FLING_MIN_DISTANCE
						&& Math.abs(velocityY) > FLING_MIN_VELOCITY) {
					Log.i(TAG, "Fling Up");
					verticalFling(e1, e2);

				} else if (e2.getY() - e1.getY() > FLING_MIN_DISTANCE
						&& Math.abs(velocityY) > FLING_MIN_VELOCITY) {
					Log.i(TAG, "Fling Down");
					verticalFling(e1, e2);
				}

				return false;
			}
			public boolean onDown(MotionEvent e) {
				return false;
			}
		});
	}
	
	

	private void eventFunc(MotionEvent event, int x, int y, int action){
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
                        x,
                        y);

                mPointerCoordsT[i] = pt;
            }catch(Exception e) {

            }
        }

        MotionEventStruct new_e = new MotionEventStruct(
                0,
                0,
                action,
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

        byte[] data = makeMotionEventPacket(new_e);
        mUtils.makeDatagramPacket(data);
	}

	private class SeekBarListener implements OnSeekBarChangeListener {

		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromTouch) {
			//Log.i(DEBUG_TAG,"" + progress);
			if (progress == 0)
			{
				progress = 10;
			}
			if (seekBar.getId() == R.id.mouse_gravitation_seek) {

				mMouseGravitationPref = progress;
				// wdw add 2012-1-29
				mouseGravitation = MOUSE_GRAVITATION_DEFAULT
						* ((float) mMouseGravitationPref / 50);
				// wdw add end
				saveMousePreferences(MOUSE_ACCELERATION_SETTINGS_KEY, progress);
			} else {
				mMouseTouchPref = progress;
				saveMousePreferences(MOUSE_SENSITIVITY_SETTINGS_KEY, progress);
				// wdw add 2012-1-29
				mouseTouch = MOUSE_TOUCH_DEFAULT
						* ((float) mMouseTouchPref / 50);
				// wdw add end
			}
		}

		public void onStartTrackingTouch(SeekBar arg0) { }

		public void onStopTrackingTouch(SeekBar arg0) { }
	}

	private void saveMousePreferences(String preferenceName, int sensitivity) {
		SharedPreferences.Editor editor = getSharedPreferences(
				GmoteClient.PREFS, MODE_WORLD_WRITEABLE).edit();
		editor.putInt(preferenceName, sensitivity);
		editor.commit();
	}

	public static float round(final float value, final int scale, int roundingMode) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(scale, roundingMode);
		final float d = bd.floatValue();
		bd = null;
		return d;
	}

	final SensorEventListener myListener = new SensorEventListener() {
		public void onSensorChanged(final SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
				return;
			}
			if (isPressed) {
				isTrackBallOK = false;
				// g_acceleration = event.values.clone();
				g_acceleration = lowPass(event.values, g_acceleration);
				// g_acceleration[0] =
				// round(g_acceleration[0],2,BigDecimal.ROUND_HALF_UP);
				// g_acceleration[1] =
				// round(g_acceleration[1],2,BigDecimal.ROUND_HALF_UP);
				if (Math.abs(stop_acceleration[0] - g_acceleration[0]) >= DIVIDING_VALUE) {
					stop_acceleration[0] = g_acceleration[0];
					mX = stop_acceleration[0] - start_acceleration[0];
//					if (mMouseGravitationPref <= 10) {
//						mMouseGravitationPref = 10;
//					}
					float sensitivity = (float) (mMouseGravitationPref * 1.5 / 10.0);
					Log.i("num", "" + sensitivity);
					mX = - mX * sensitivity * SCALE_X;
					start_acceleration[0] = stop_acceleration[0];
				}
				if (Math.abs(stop_acceleration[1] - g_acceleration[1]) >= DIVIDING_VALUE) {
					stop_acceleration[1] = g_acceleration[1];
					mY = stop_acceleration[1] - start_acceleration[1];
//					if (mMouseGravitationPref <= 10) {
//						mMouseGravitationPref = 10;
//					}
					float num = (float) (mMouseGravitationPref / 10.0);
					Log.i("num", "" + num);
					mY = - mY * num * SCALE_Y;
					start_acceleration[1] = stop_acceleration[1];
				}
				synchronized (waitForMouseMove) {
					waitForMouseMove.notify();
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Log.i(DEBUG_TAG,"WHAT HAPPENED");
		}
	};

	/*
	 * time smoothing constant for low-pass filter 0 ; a smaller value basically
	 * means more smoothing See:
	 * http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
	 */

	protected float[] lowPass(float[] input, float[] output) {
		if (output == null)
			return input;

		for (int i = 0; i < input.length; i++) {
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
		}
		return output;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return mUtil.onCreateDialog(id);
	}

	AisinoGestureDetector.OnGestureListener gestureListener = new AisinoGestureDetector.SimpleOnGestureListener() {
		public boolean onDown(MotionEvent e) {
			isTrackBallOK = false;
			return true;
		};

		public void onLongPress(MotionEvent e) {
			// TODO: need this method
			Log.d(TAG, "--->gestureListener onLongPress e=" + e.getAction());
			mouseClick(MouseEvent.LEFT_MOUSE_DOWN);
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			incrementDistance(distanceX * mouseTouch, distanceY
					* mouseTouch);
			synchronized (waitForMouseMove) {
				waitForMouseMove.notify();
			}
			return true;
		}

		public boolean onSingleTapUp(MotionEvent e) {
			Log.d(TAG, "--->gestureListener onSingleTapUp e=" + e.getAction());
			mouseClick(MouseEvent.SINGLE_CLICK);
			return true;
		}

		public boolean onLongPressUp(MotionEvent e) {
			Log.d(TAG, "--->gestureListener onLongPressUp ");
			mouseClick(MouseEvent.LEFT_MOUSE_UP);
			return true;
		}
	};

	void mouseClick(MouseEvent evt) {
		mUtil.send(new MouseClickPacket(evt));
//		if(evt == MouseEvent.LEFT_MOUSE_UP)
//		    mUtil.mHandler.sendEmptyMessageDelayed(Remote.LONG_PRESS_UP_CONFIRM, 1000);
	}

	@Override
	public void onStart() {
		super.onStart();
		mUtil.onStart(this);
		serverUdpPort = Remote.getInstance().getServerUdpPort();
		Log.d(TAG, "--->TouchPad onStart()");
	}

	@Override
	public void onStop() {
		super.onStop();

		mUtil.onStop();
		Log.d(TAG, "--->TouchPad onStop()");
	}

	@Override
	public void onResume() {
		super.onResume();
		mUtil.onResume();
		// createView();

		SharedPreferences prefs = getSharedPreferences(GmoteClient.PREFS,
				MODE_WORLD_READABLE);
		mMouseTouchPref = prefs.getInt(
				Touchpad.MOUSE_SENSITIVITY_SETTINGS_KEY, 50);
		setSeekBar(R.id.mouse_touch_seek, mMouseTouchPref);

		mMouseGravitationPref = prefs.getInt(
				Touchpad.MOUSE_ACCELERATION_SETTINGS_KEY, 50);
		setSeekBar(R.id.mouse_gravitation_seek, mMouseGravitationPref);
	}

	private void setSeekBar(int seekBarId, int seekBarValue) {
		((SeekBar) findViewById(seekBarId)).setProgress(seekBarValue);
	}

	@Override
	public void onPause() {
		super.onPause();
		mUtil.onPause();
		Log.d(TAG, "--->TouchPad onPause()");
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "--->TouchPad onDestory()");
		super.onDestroy();
	}

	public void handleReceivedPacket(AbstractPacket reply) {
		if(reply.getCommand() == Command.KEYBOARD) {
			keyInputPopupWindow.startPopupWindow();
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		return gestureDetector.onTouchEvent(event);

	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mouseClick(MouseEvent.LEFT_MOUSE_DOWN);
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			mouseClick(MouseEvent.LEFT_MOUSE_UP);
			return true;
		}

		final float scaleY = event.getYPrecision();
		final float y = 0 - event.getY() * scaleY;

		if (y < 0) {
			mouseWheelMove(1);
		} else if (y > 0) {
			mouseWheelMove(-1);
		}

		return true;
	}

	void mouseWheelMove(int wheelAmount) {
		remoteInstance.queuePacket(new MouseWheelPacket(wheelAmount));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Log.i("onKeyDown", event.toString());
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		}
		else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			mouseClick(MouseEvent.SINGLE_CLICK);

		} else {
			KeyCharacterMap kmap = KeyCharacterMap.load(event.getDeviceId());

			int c = kmap.get(keyCode, event.getMetaState());

			if (c != 0) {
				mUtil.send(new KeyboardEventPacket(c));
			} else {
				if (keyCode == KeyEvent.KEYCODE_DEL) {
					mUtil.send(new KeyboardEventPacket(
							KeyboardEventPacket.DELETE_KEYCODE));
				} else if (keyCode == KeyEvent.KEYCODE_SEARCH
						|| keyCode == KeyEvent.KEYCODE_SYM) {
					mUtil.send(new KeyboardEventPacket(
							KeyboardEventPacket.SEARCH_KEYCODE));
				}
			}
		}
		return true;
	}

	private synchronized void incrementDistance(float distanceX, float distanceY) {
		long currentTime = System.currentTimeMillis();
		if (distanceX > 0) {
			posXAcceleration = computeAcceleration(timeOfLastPosX, currentTime,
					posXAcceleration, distanceX);
			timeOfLastPosX = currentTime;
			mX += distanceX + posXAcceleration;
		} else {
			negXAcceleration = computeAcceleration(timeOfLastNegX, currentTime,
					negXAcceleration, Math.abs(distanceX));
			timeOfLastNegX = currentTime;
			mX += distanceX + (negXAcceleration * -1); // * distanceX * -1);
		}

		if (distanceY > 0) {
			posYAcceleration = computeAcceleration(timeOfLastPosY, currentTime,
					posYAcceleration, distanceY);
			timeOfLastPosY = currentTime;
			mY += distanceY + posYAcceleration;
		} else {
			negYAcceleration = computeAcceleration(timeOfLastNegY, currentTime,
					negYAcceleration, Math.abs(distanceY));
			timeOfLastNegY = currentTime;
			mY += distanceY + (negYAcceleration * -1); // * distanceX * -1);
		}
	}

	private synchronized DatagramPacket makeDatagramPacketIfNeeded() {
		DatagramPacket packet = null;

		if (mX != 0 || mY != 0) {
			byte[] buf = new byte[5];
			short tempX = (short) mX;
			short tempY = (short) mY;
			buf[0] = UdpPacketTypes.MOUSE_MOVE.getId();
			buf[1] = (byte) tempX;
			buf[2] = (byte) (tempX >>> 8);
			buf[3] = (byte) tempY;
			buf[4] = (byte) (tempY >>> 8);
			// Log.i(DEBUG_TAG, "mX=" + mX + " mY=" + mY + " shortX=" + tempX +
			// " shortY=" + tempY);
			packet = new DatagramPacket(buf, buf.length,
					remoteInstance.getServerInetAddress(), serverUdpPort);
			// Log.i(DEBUG_TAG, remoteInstance.getServerIp());
			mX = 0;
			mY = 0;
		}

		return packet;
	}

	public class MouseSendingThread implements Runnable {

		public void run() {
			DatagramSocket socket = null;
			try {
				socket = new DatagramSocket();
			} catch (SocketException e) {
				Log.e(DEBUG_TAG, e.getMessage(), e);
			}
			while (true) {
				DatagramPacket packet = makeDatagramPacketIfNeeded();
				if (packet != null) {
					// Log.i(DEBUG_TAG, "Sending packet");
					try {
						if (!remoteInstance.isConnected()) {
							remoteInstance.connect(false);
						}
						socket.send(packet);
						// Log.i(DEBUG_TAG, "Packet sent");
					} catch (IOException e) {
						Log.e(DEBUG_TAG, e.getMessage(), e);
					}
				} else {
					try {
						synchronized (waitForMouseMove) {
							waitForMouseMove.wait();
						}
					} catch (InterruptedException e) {
						Log.e(DEBUG_TAG, e.getMessage(), e);
						return;
					}
				}
			}
		}
	}

	private float computeAcceleration(long timeOfLastMove, long currentTime,
			float lastAcceleration, float distanceMoved) {

		// Decay the current acceleration value.
		lastAcceleration -= (currentTime - timeOfLastMove) * ACCELERATION_DECAY;
		// Add acceleration based on the current movement.
		lastAcceleration += distanceMoved;

		if (lastAcceleration < 0) {
			lastAcceleration = 0;
		}

		return lastAcceleration * mouseGravitation;
	}

	public void loadMouseSettings() {
		SharedPreferences prefs = getSharedPreferences(GmoteClient.PREFS,
				MODE_WORLD_READABLE);
		int mouseSensitivityPref = prefs.getInt(MOUSE_SENSITIVITY_SETTINGS_KEY,
				50);
		int mouseAccelerationPref = prefs.getInt(
				MOUSE_ACCELERATION_SETTINGS_KEY, 50);
		mouseTouch = MOUSE_TOUCH_DEFAULT
				* ((float) mouseSensitivityPref / 50);
		mouseGravitation = MOUSE_GRAVITATION_DEFAULT
				* ((float) mouseAccelerationPref / 50);
		Log.i(DEBUG_TAG, "Setting Prefs: sens=" + mouseSensitivityPref
				+ " accel=" + mouseAccelerationPref);
		Log.i(DEBUG_TAG, "Setting Mouse to: sens=" + mouseTouch
				+ " accel=" + mouseGravitation);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		keyInputPopupWindow.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onClick(View v) {
		if (v.getId() == R.id.new_mouse_left) {
			if (!isTrackBallOK) {
				mouseClick(MouseEvent.SINGLE_CLICK);display = getWindowManager().getDefaultDisplay();
			    mScreenWidth = display.getWidth();
			    mScreenHeight = display.getHeight();
			} else {
				mUtil.send(new MouseWheelPacket(0));
			}

		} else if (v.getId() == R.id.new_mouse_right) {
			Intent intent = new Intent();
			intent.setAction("com.android.CAPTURE_SCREEN");
			sendBroadcast(intent);
			mouseClick(MouseEvent.RIGHT_CLICK);
		} else if (v.getId() == R.id.new_mouse_key) {
			keyInputPopupWindow.startPopupWindow();
		} else if (v.getId() == R.id.new_mouse_close) {
			this.finish();
		} else if (v.getId() == R.id.home_switch_touchpad) {
			this.finish();
		} else if(v.getId() == R.id.main_control_switch_touchpad) {
			Intent intent = new Intent(this,HomeActivity.class);
			this.startActivity(intent);
			this.finish();
		} else if(v.getId() == R.id.game_control_switch_touchpad) {
			Intent intent = new Intent(this,ScreenShowActivity.class);
			this.startActivity(intent);
			this.finish();
		}
	}
	
	/** Make udp event packet to be send. */
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

            } catch (Exception ex) {
                     ex.printStackTrace();
            } finally {
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
                } catch (final IOException e1) {
                    e1.printStackTrace();
                }

            }
        }
        return sendBuff;
    }
	
	/** Horizontal seekbar fling. */
	private void horizontalFling(final MotionEvent e1, final MotionEvent e2){
		final int start_dis_x = (int) e1.getX()  * srcWidth / mScreenWidth;
		final int end_dis_x = (int) e2.getX() * srcWidth / mScreenWidth;

		mTimer = new Timer(true);
		mTimerTask = new TimerTask() {
	        public void run() {

	        	int action;
	        	switch (indextemp) {
				case 0:
					action = 0;
					break;
				case NUM_FLING_POINTS-1:
					action = 1;
					break;
				default:
					action = 2;
					break;
				}
	        	
	        	eventFunc(e1, 
	        			start_dis_x + (end_dis_x - start_dis_x) / NUM_FLING_POINTS * indextemp, srcHeight / 2, 
						action);
				indextemp++;
				
				if (indextemp == NUM_FLING_POINTS) {
					indextemp = 0;
					while(!mTimerTask.cancel());
					mTimer.cancel();
				}
	        }
	    };
        mTimer.schedule(mTimerTask, 0, NUM_FLING_TIMER);
	}
	
	/** Vertical seekbar fling. */
	private void verticalFling(final MotionEvent e1, final MotionEvent e2){
		final int start_dis_y = (int) e1.getY()  * srcHeight / mScreenHeight;
		final int end_dis_y = (int) e2.getY() * srcHeight / mScreenHeight;

		mTimer = new Timer(true);
		mTimerTask = new TimerTask() {
	        public void run() {
	        	int action;
	        	switch (indextemp) {
					case 0:
						action = 0;
						break;
					case NUM_FLING_POINTS - 1:
						action = 1;
						break;
					default:
						action = 2;
						break;
				}
	        	eventFunc(e1, srcWidth / 2, 
	        			start_dis_y + (end_dis_y - start_dis_y) / NUM_FLING_POINTS * indextemp, 
						action);
				indextemp++;
				if (indextemp == NUM_FLING_POINTS) {
					indextemp = 0;
					while (!mTimerTask.cancel()) ;
					mTimer.cancel();
				}
	        }
	    };
        mTimer.schedule(mTimerTask, 0, NUM_FLING_TIMER);
	}
}
