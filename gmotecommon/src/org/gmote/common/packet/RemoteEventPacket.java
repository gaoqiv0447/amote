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

package org.gmote.common.packet;

import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.RemoteEvent;

public class RemoteEventPacket extends AbstractPacket {

  private static final long serialVersionUID = 1L;
  public static final int DELETE_KEYCODE = -10;
  public static final int SEARCH_KEYCODE = -11;
  
  private RemoteEvent remoteEvent;
  
  public RemoteEventPacket(RemoteEvent remoteEvent) {
    super(Command.REMOTE_EVENT);
    this.remoteEvent = remoteEvent;
  }

  public RemoteEvent getRemoteEvent() {
    return remoteEvent;
  }

}