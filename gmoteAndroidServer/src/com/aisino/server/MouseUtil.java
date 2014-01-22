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

import org.gmote.common.Protocol.MouseEvent;
import org.gmote.common.ScreenInfo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.preference.PreferenceManager;

import com.aisino.awt.InputEvent;
import com.aisino.awt.MouseArrow;
import com.aisino.awt.MouseInfo;
import com.aisino.awt.Robot;

public class MouseUtil {
    private static final String TAG = "MouseUtil";
    public static final int MESSAGE_FOR_MOUSE = 1;
    public static final int MESSAGE_FOR_MOUSE_ARROW = 2;
    public static final int MESSAGE_FOR_MOUSE_HAND = 3;
    public static final int  LEFT_MOUSE_UP = 10;
    public static final int  LEFT_MOUSE_DOWN = 11;

	private static final String MOUSE_STYLE_LIGHT = "0";
	private static final String MOUSE_STYLE_DARK = "1";

    private static MouseArrow mMouse=null;
    private static Context mContext;
    public static  Handler mHandler;
    private static boolean mViewRemoved = false;

    public static  void doMouseEvent(MouseEvent mouseEvent, Robot robot,int x,int y) {
        Log.i(TAG, "--->MouseEvent mouseEvent="+mouseEvent);
        if (mouseEvent == MouseEvent.SINGLE_CLICK) {
          clickMouse(InputEvent.BUTTON1_MASK, robot,x,y);
        } else if (mouseEvent == MouseEvent.RIGHT_CLICK) {
          robot.sendKeyEvent(KeyEvent.KEYCODE_BACK);
        } else if (mouseEvent == MouseEvent.DOUBLE_CLICK) {
          clickMouse(InputEvent.BUTTON1_MASK, robot,x,y);
          clickMouse(InputEvent.BUTTON1_MASK, robot,x,y);
        } else if (mouseEvent == MouseEvent.LEFT_MOUSE_DOWN) {
            mHandler.sendEmptyMessage(MESSAGE_FOR_MOUSE_HAND);
            robot.mouseMove(x,y,MotionEvent.ACTION_DOWN,LEFT_MOUSE_DOWN);
        } else if (mouseEvent == MouseEvent.LEFT_MOUSE_UP) {
            robot.mouseMove(x,y,MotionEvent.ACTION_UP,LEFT_MOUSE_UP);
            mHandler.sendEmptyMessage(MESSAGE_FOR_MOUSE_ARROW);
        } else if(mouseEvent == MouseEvent.LONG_PRESS){
            robot.mouseMove(x,y,MotionEvent.ACTION_DOWN,0);
        }
      }

    public static void setPointerMouse(){
		if (Settings.getKeyMouseStyle().equals(MOUSE_STYLE_LIGHT)) {
			mMouse.setImageResource(R.drawable.mouse_arrow_light);
		}else {
        mMouse.setImageResource(R.drawable.mouse_arrow);
    }

    }
    public static void setHandMouse(){
		if (Settings.getKeyMouseStyle().equals(MOUSE_STYLE_LIGHT)) {
			mMouse.setImageResource(R.drawable.mouse_hand_light);
		}else {
        mMouse.setImageResource(R.drawable.mouse_hand);
    }
    }


      private static void clickMouse(int buttonMask, Robot robot,int x, int y) {
           mHandler.sendEmptyMessage(MESSAGE_FOR_MOUSE_ARROW);
           robot.mouseClick(x, y);
      }

      protected static void createMouseView(Context context,WindowManager.LayoutParams wmParams,Handler handler){

          mHandler = handler;
          WindowManager wm=null;
          mContext = context;
          mMouse=new MouseArrow(mContext.getApplicationContext());
          mMouse.setImageResource(R.drawable.mouse_arrow);
          Drawable d  = context.getResources().getDrawable(R.drawable.mouse_arrow);

		  Settings.getInstance(PreferenceManager
                .getDefaultSharedPreferences(mContext.getApplicationContext()));
		  Log.d(TAG,"=====MouseStyle: " + Settings.getKeyMouseStyle());
		  if (Settings.getKeyMouseStyle().equals(MOUSE_STYLE_LIGHT)) {
			Log.d(TAG,"======MouseStyle Changed to Light Style!!!");
			mMouse.setImageResource(R.drawable.mouse_arrow_light);
			d  = context.getResources().getDrawable(R.drawable.mouse_arrow_light);
		  }
		  Settings.releaseInstance();
          // 获取WindowManager
          wm=(WindowManager)mContext.getApplicationContext().getSystemService("window");
          // 设置LayoutParams(全局变量）相关参数
           /**
           * 以下都是WindowManager.LayoutParams的相关属性
           * 具体用途可参考SDK文档
           */
         /*system overlay windows, which need to be displayed on top of everything else*/
          wmParams.type=LayoutParams.TYPE_SYSTEM_OVERLAY;
          wmParams.format=PixelFormat.RGBA_8888;   //设置图片格式，效果为背景透明

          /*
           * 下面的flags属性的效果形同“锁定”。
           * 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应
           * 布局区域为真个屏幕区域。
           * */
           wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL
                                 | LayoutParams.FLAG_NOT_FOCUSABLE
                                 | LayoutParams.FLAG_NOT_TOUCHABLE
                                 | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                 | LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                 | LayoutParams.FLAG_KEEP_SCREEN_ON;
                                 //| LayoutParams.FLAG_LAYOUT_INSET_DECOR;



          wmParams.gravity=Gravity.LEFT|Gravity.TOP;   //调整悬浮窗口至左上角
          //以屏幕左上角为原点，设置x、y初始值
          int tmpx = MouseInfo.getLocation().x;
          int tmpy = MouseInfo.getLocation().y;
          if(tmpx!=0 || tmpy!=0 ){
              wmParams.x=tmpx;
              wmParams.y=tmpx;
          MouseInfo.setLocation(tmpx, tmpy);
          }else{
              wmParams.x=ScreenInfo.width/2;
              wmParams.y=ScreenInfo.height/2;
              MouseInfo.setLocation(ScreenInfo.width/2, ScreenInfo.width/2);
          }

          //设置悬浮窗口长宽数据
          wmParams.width=d.getIntrinsicWidth();
          wmParams.height=d.getIntrinsicHeight();
          //显示myFloatView图像
          wm.addView(mMouse, wmParams);
          mViewRemoved = false;

      }

      public static void  updateMouseView(int x, int y) {
          if(!mViewRemoved)
              mMouse.handlerUpdatePosition(x, y);
      }
      protected static void  removeMouseView() {
          mViewRemoved = true;
          WindowManager wm =(WindowManager)mContext.getApplicationContext().getSystemService("window");
          wm.removeView(mMouse);
      }
      public static void setMouseInvisible(){
          mMouse.setVisibility(View.GONE);
      }
      public static void setMouseVisible(){
          mMouse.setVisibility(View.VISIBLE);
      }
      public static int getMouseVisible(){
         return mMouse.getVisibility();
      }
      public static void sendEventToService(int x, int y){
          Message msg = mHandler.obtainMessage(MESSAGE_FOR_MOUSE, x, y);
          mHandler.sendMessage(msg);
      }
}
