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

import android.graphics.Point;

public class MouseInfo {
    private static Point point = new Point();
    private static MouseInfo instance = null;
    /**
     * Gets an instance of this class.
     */
    public static MouseInfo instance() {
      if (instance == null) {
          instance = new MouseInfo();
      }
      return instance;
    }
    public static Point getLocation() {
        return point;
    }
    public static void setLocation(int x, int y){
        point.x = x;
        point.y = y;
    }

}
