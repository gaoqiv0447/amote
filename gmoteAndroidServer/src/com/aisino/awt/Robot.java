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

package com.aisino.awt;

import com.aisino.server.MouseUtil;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class Robot {
    private final static boolean ROBOT_DEBUG = false;
    protected static final String TAG = "Robot";
    protected static boolean isPointerDown = false;
    public static void setPointeState(boolean state){
        isPointerDown  = state;
    }
    public  synchronized void mouseMove(final int newX, final int newY,final int type,final int y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        final MotionEvent e = MotionEvent.obtain(downTime, downTime,
                type, newX, newY, 0);

//        if (y != MouseUtil.LEFT_MOUSE_DOWN && y != MouseUtil.LEFT_MOUSE_UP)
//            MouseUtil.sendEventToService(newX, newY + y);
//        else {
            MouseUtil.sendEventToService(newX, newY);
//        }

        if(y==MouseUtil.LEFT_MOUSE_DOWN){
            isPointerDown = true;
        }

        if(ROBOT_DEBUG){
            Log.d(TAG, "---->Robot->mouseMove before newX"+newX + " newY="+newY);
        }

        if(!isPointerDown){
            if(ROBOT_DEBUG) Log.d(TAG, "---->Robot->isPointerDown=false");
            return;
        }

        if(ROBOT_DEBUG) Log.d(TAG, "---->Robot->mouseMove newX="+newX + " newY="+newY);

        new Thread()
        {
            public void run() {
                if(ROBOT_DEBUG) Log.d(TAG, "---->Robot->mouseMove newX="+newX + " newY="+newY);

                try {

                    (IWindowManager.Stub
                        .asInterface(ServiceManager.getService("window")))
                        .injectPointerEvent(e, true);
                } catch (RemoteException e1) {
                    Log.i("Input", "-------DeadOjbectException" + e1);
                } catch(java.lang.SecurityException e1){
                    Log.i("Input", "-------SecurityException" +e1);
                }finally{
                    if(y==MouseUtil.LEFT_MOUSE_UP){
                        isPointerDown = false;
                    }
                }

              }

        }.start();
    }

    public  synchronized void mouseClick(final int newX, final int newY) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        final MotionEvent e = MotionEvent.obtain(downTime, downTime,
                MotionEvent.ACTION_DOWN, newX, newY, 0);
        final MotionEvent e1 = MotionEvent.obtain(downTime, downTime,
                MotionEvent.ACTION_UP, newX, newY, 0);
        MouseUtil.sendEventToService(newX,newY);
        isPointerDown = false;
        new Thread()
        {
            public void run() {
                if(ROBOT_DEBUG)  Log.d(TAG, "---->Robot->mouseMove newX="+newX + " newY="+newY);
                try {
                    (IWindowManager.Stub
                        .asInterface(ServiceManager.getService("window")))
                        .injectPointerEvent(e, true);
                    (IWindowManager.Stub
                            .asInterface(ServiceManager.getService("window")))
                            .injectPointerEvent(e1, true);

                } catch (RemoteException e1) {
                    Log.i("Input", "-------DeadOjbectException" + e1);
                } catch(java.lang.SecurityException e1){
                    Log.i("Input", "-------SecurityException" +e1);
                }

              }

        }.start();
    }

    /**
     * Send a single key event.
     *
     * @param event is a string representing the keycode of the key event you
     * want to execute.
     */
    public synchronized void sendKeyEvent(final int eventCode) {
        new Thread(){
          @Override
        public void run() {
              final long now = SystemClock.uptimeMillis();
              if(ROBOT_DEBUG) Log.i("SendKeyEvent","---->"+ eventCode);
              try {
                  KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, eventCode, 0);
                  KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, eventCode, 0);
                  (IWindowManager.Stub
                      .asInterface(ServiceManager.getService("window")))
                      .injectKeyEvent(down, true);
                  (IWindowManager.Stub
                      .asInterface(ServiceManager.getService("window")))
                      .injectKeyEvent(up, true);
              } catch (RemoteException e) {
                  Log.i("Input", "DeadOjbectException");
              }
        }
        }.start();

    }

	public  synchronized void MotionEventControl(MotionEvent event) {

        final MotionEvent e = event;
		long currentTime = System.currentTimeMillis();
		if(ROBOT_DEBUG) Log.d(TAG,"=======motionevent robot received time: "+String.valueOf(currentTime));
		//MouseUtil.sendEventToService((int)event.getX(),(int)event.getY());
        new Thread()
        {
            public void run() {
                try {
                    (IWindowManager.Stub
                        .asInterface(ServiceManager.getService("window")))
                        .injectPointerEvent(e, true);
                    if(ROBOT_DEBUG) Log.d(TAG, "---robot-motionevent:" + e.toString());
                } catch (RemoteException e1) {
                    Log.i("Input", "-------DeadOjbectException" + e1);
                } catch(java.lang.SecurityException e1){
                    Log.i("Input", "-------SecurityException" +e1);
                }finally{
                }

              }

        }.start();
    }
}
