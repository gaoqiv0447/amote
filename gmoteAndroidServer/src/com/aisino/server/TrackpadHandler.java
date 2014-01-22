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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.gmote.common.Protocol.MouseEvent;
import org.gmote.common.Protocol.RemoteEvent;
import org.gmote.common.packet.MouseClickPacket;
import org.gmote.common.packet.MouseWheelPacket;
import org.gmote.common.packet.RemoteEventPacket;
import org.gmote.common.packet.MotionEventPacket;
import org.gmote.common.MotionEventStruct;
import org.gmote.common.MotionEventStruct.PointerCoordsT;

import android.graphics.Point;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.os.Parcel;
import android.os.SystemClock;

import com.aisino.awt.MouseInfo;
import com.aisino.awt.Robot;
import org.gmote.common.ScreenInfo;

public class TrackpadHandler {
    private static final Logger LOGGER = Logger.getLogger(TrackpadHandler.class.getName());

    private static final String TAG = "TrackpadHandler";

    private static TrackpadHandler instance = null;

    private boolean  isChanged = false;

    Robot robot = null;

    int mouseX = 1;

    int mouseY = 1;

    /**
     * Private constructor to prevent instantiation.
     */
    private TrackpadHandler() {
        robot = new Robot();
    }

    /**
     * Gets an instance of this class.
     */
    public static TrackpadHandler instance() {
        if (instance == null) {
            instance = new TrackpadHandler();
        }
        return instance;
    }
    public void setOrientation(boolean flag){
       isChanged = flag;
    }

    /**
     * Moves the mouse on the screen of a user specified amount.
     */
    public synchronized void handleMoveMouseCommand(short diffX, short diffY ) {
        int newX = 0;
        int newY = 0;
        int tmpy=0;

        Point location = MouseInfo.getLocation();
        //Log.d(TAG, "#####-->handleMoveMouseCommand ("+location.x+","+location.y+")");

        if (location != null) {
            newX = location.x + diffX;
            newY = location.y + diffY;

            if(newX<0){
                newX = 0;
            }
            if(newY<0){
                newY = 0;
            }
            if(newX>ScreenInfo.width){
                newX=ScreenInfo.width;
            }
            if(newY >ScreenInfo.height){
                newY=ScreenInfo.height;
            }
        }
        robot.mouseMove(newX, newY, MotionEvent.ACTION_MOVE,tmpy);
        MouseInfo.setLocation(newX, newY);
    }

    /**
     * Performs a mouse click based on what was sent by the user.
     */
    public synchronized void hanldeMouseClickCommand(MouseClickPacket packet) {
        Point location = MouseInfo.getLocation();
        MouseEvent mouseEvent = packet.getMouseEvent();
        MouseUtil.doMouseEvent(mouseEvent, robot, location.x, location.y);
    }

    public void handeMouseWheelCommand(MouseWheelPacket packet) {
        int whell = packet.getWheelAmount();
        if (whell > 0) {
            robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
        } else if (whell < 0) {
            robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP);

        }else if(whell ==0){
            robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER);
        }
    }
    public void handleRemoteCommand(RemoteEventPacket packet){
    	RemoteEvent rE = packet.getRemoteEvent();
    	switch(rE){
    		case REMOTE_HOME:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_HOME);
    			break;
    		case REMOTE_MENU:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_MENU);
    			break;
    		case REMOTE_BACK:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_BACK);
    			break;
    		case REMOTE_SEARCH:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_SEARCH);
    			break;
    		case REMOTE_UP:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
    			break;
    		case REMOTE_DOWN:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
    			break;
    		case REMOTE_LEFT:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);
    			break;
    		case REMOTE_RIGHT:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
    			break;
    		case REMOTE_ENTER:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER);
    			break;
    		case REMOTE_VOLUP:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_VOLUME_UP);
    			break;
    		case REMOTE_VOLDOWN:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN);
    			break;
    		case REMOTE_VOLMUTE:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_MUTE);
    			break;
    		case REMOTE_SHUTDOWN:
    			//robot.sendKeyEvent(KeyEvent.KEYCODE_HOME);
    			break;
    		case REMOTE_DELETE:
    			robot.sendKeyEvent(KeyEvent.KEYCODE_DEL);
    			break;
    		default:
    			Log.i(TAG, "Do not have this Remote Key");
    			break;

    	}
    }

	/**
	* handle motionEvent from client
	**/
	public synchronized void handeMotionEvent(MotionEventPacket packet) {
		MotionEventStruct eStruct = packet.getEvent();

		int count = eStruct.pointerCount;
		PointerCoords[] mPointerCoords = new PointerCoords[count];
        PointerCoordsT[] mPointerCoordsT = eStruct.pointerCoords;
		if(mPointerCoordsT == null) return;
		//Log.d(TAG, "=====try to construct the motionevent=======");
        for(int i=0;i<count;i++) {
            PointerCoords out = new PointerCoords();
			out.orientation = mPointerCoordsT[i].orientation;
			out.pressure = mPointerCoordsT[i].pressure;
            out.size = mPointerCoordsT[i].size;
            out.toolMajor = mPointerCoordsT[i].toolMajor;
            out.toolMinor = mPointerCoordsT[i].toolMinor;
            out.touchMajor = mPointerCoordsT[i].touchMajor;
            out.touchMinor = mPointerCoordsT[i].touchMinor;
            out.x = mPointerCoordsT[i].x;
            out.y = mPointerCoordsT[i].y;
            Log.d(TAG,"=======x: "+out.x);
            Log.d(TAG,"=======x: "+out.y);
            mPointerCoords[i] = out;

        }
		//Log.d(TAG,"distance: "+String.valueOf(eStruct.downTime));
		MotionEvent e = MotionEvent.obtain(SystemClock.uptimeMillis(),//+eStruct.downTime,//SystemClock.uptimeMillis(),
										SystemClock.uptimeMillis(),//+eStruct.eventTime,//SystemClock.uptimeMillis(),
										eStruct.action,
										eStruct.pointerCount,
										eStruct.pointerIds,
										mPointerCoords,
										eStruct.metaState,
										eStruct.xPrecision,
										eStruct.yPrecision,
										0,//eStruct.deviceId,
										eStruct.edgeFlags ,
										eStruct.source,
										eStruct.flags);
		Log.d(TAG,"=======action: "+eStruct.action);
		robot.MotionEventControl(e);
		// Log.d(TAG,"=======e.getDownTime(): "+String.valueOf(e.getDownTime()));
        // Log.d(TAG,"=======e.getEventTime(): "+String.valueOf(e.getEventTime()));
    }
}
