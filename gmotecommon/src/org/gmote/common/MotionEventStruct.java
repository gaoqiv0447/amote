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

package org.gmote.common;

import java.io.Serializable;

    /**
     * 
     * @author zhangdawei
     * This class is a copy of android.view.motionevent,
     * it contains main values of the android.view.motionevent(include multi-touch values).
     * When the server received MotionEventPacket,a motionevent will be rebuild by using
     * MotionEvent.obtain(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, PointerCoords[] pointerCoords, int metaState, float xPrecision, float yPrecision, int deviceId, int edgeFlags, int source, int flags)
     * unfortunately,this method was deprecated in android2.3,and was instead of a new method in android4.0
     */

  public class MotionEventStruct implements Serializable {
      
      private static final long serialVersionUID = 1L;
      
      // The time (in ms) when the user originally pressed down to start a stream of position events. 
      // This must be obtained from uptimeMillis().
      public long downTime; 
      
      // The the time (in ms) when this specific event was generated. 
      // This must be obtained from uptimeMillis().
      public long eventTime; 
      
      // The kind of action being performed, such as ACTION_DOWN.
      public int action; 
      
      // The number of pointers that will be in this event.
      public int pointerCount; 
      
      // An array of pointerCount values providing an identifier for each pointer.
      public int[] pointerIds; 
      
      // An array of pointerCount values providing a MotionEvent.PointerCoords coordinate object for each pointer.
      public PointerCoordsT[] pointerCoords; 
      
      // The state of any meta / modifier keys that were in effect when the event was generated.
      public int metaState;
      
      // The precision of the X coordinate being reported.
      public float xPrecision; 
      
      // The precision of the Y coordinate being reported.
      public float yPrecision; 
      
      // The id for the device that this event came from. 
      // An id of zero indicates that the event didn't come from a physical device; 
      // other numbers are arbitrary and you shouldn't depend on the values.
      public int deviceId; 
      
      // A bitfield indicating which edges, if any, were touched by this MotionEvent.
      public int edgeFlags; 
      
      // The source of this event.
      public int source; 
      
      // The motion event flags.
      public int flags;
      
      public MotionEventStruct(){};
      public MotionEventStruct(long downTime ,long eventTime, int action, int pointerCount, 
              int[] pointerIds, PointerCoordsT[] pointerCoords,int metaState,
              float xPrecision,float yPrecision,int deviceId,int edgeFlags,int source, int flags) {
          
          this.downTime = downTime; 
          this.eventTime = eventTime; 
          this.action = action; 
          this.pointerCount = pointerCount; 
          this.pointerIds = pointerIds; 
          this.pointerCoords = pointerCoords; 
          this.metaState = metaState;
          this.xPrecision = xPrecision; 
          this.yPrecision = yPrecision; 
          this.deviceId = deviceId; 
          this.edgeFlags = edgeFlags; 
          this.source = source; 
          this.flags = flags;
          
      }
      
      
  public static class PointerCoordsT implements Serializable {
      private static final long serialVersionUID = 1L;
      
      // The orientation of the touch area and tool area in radians clockwise from vertical.
      public float  orientation; 
      
      // A normalized value that describes the pressure applied to the device by a finger or other tool.
      public float  pressure;   
      
      // A normalized value that describes the approximate size of the pointer touch area 
      // in relation to the maximum detectable size of the device.
      public float  size;
      
      // The length of the major axis of an ellipse that describes the size of the approaching tool.
      public float  toolMajor;   
      
      // The length of the minor axis of an ellipse that describes the size of the approaching tool.
      public float  toolMinor;   
      
      // The length of the major axis of an ellipse that describes the touch area at the point of contact.
      public float  touchMajor;
      
      // The length of the minor axis of an ellipse that describes the touch area at the point of contact.
      public float  touchMinor;  
      
      // The X component of the pointer movement.   
      public float  x;  
      
      // The Y component of the pointer movement.
      public float  y;   
          
     public PointerCoordsT(float orientation,float pressure,float size,float toolMajor,
                  float toolMinor,float touchMajor,float touchMinor,float x,float y) {
          this.orientation = orientation;
          this.pressure = pressure;
          this.size = size;
          this.toolMajor = toolMajor;
          this.toolMinor = toolMinor;
          this.touchMajor = touchMajor;
          this.touchMinor = touchMinor;
          this.x = x;
          this.y = y;
         }
         public PointerCoordsT(){};
      }
  }