/**
 * Copyright 2009 Marc Stogaitis and Mimi Sun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gmote.common.packet;

import java.io.Serializable;

import org.gmote.common.Protocol.Command;
import org.gmote.common.MotionEventStruct;


public class MotionEventPacket extends AbstractPacket implements Serializable{

  private static final long serialVersionUID = 1L;
  
  private MotionEventStruct motionevent;
  byte[] parcelByte;
  private MotionEventStruct[] eventArray;
  

  public MotionEventPacket(MotionEventStruct e) {
      super(Command.MOTION_EVENT);
      this.motionevent = e;
  }
  public MotionEventPacket(byte[] bytes) {
      super(Command.MOTION_EVENT);
      this.parcelByte = bytes;
  }
  public MotionEventPacket(MotionEventStruct[] array) {
      super(Command.MOTION_EVENT);
      this.eventArray = array;
      int i = this.eventArray[0].pointerCount;
      System.out.println("motion event array length: " + eventArray.length);
      System.out.println("motion event pointerCount: " + i);
  }
  
  
  public MotionEventStruct getEvent() {
      return this.motionevent;
  }
  
  public byte[] getparcelByte() {
      return this.parcelByte;
  }
  
  public MotionEventStruct[] getEventArray() {
      return this.eventArray;
  }
}
