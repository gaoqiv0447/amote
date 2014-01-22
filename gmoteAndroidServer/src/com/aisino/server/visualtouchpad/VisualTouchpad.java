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

package com.aisino.server.visualtouchpad;

import org.gmote.common.TcpConnection;

public class VisualTouchpad {
    private static VisualTouchpad instance = null;
    private TcpConnection con;
    /**
     * Gets an instance of this class.
     */
    public static VisualTouchpad instance() {
      if (instance == null) {
        instance = new VisualTouchpad();

      }
      return instance;
    }

    public synchronized void setConnection(TcpConnection newConnection) {
        con = newConnection;
    }

}
