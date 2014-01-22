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

public class ScreenInfo {
    private static ScreenInfo instance;
    public static int width;
    public static int height;
    public static int error_x;
    public static int error_y;
    public static int densityDpi;
    private ScreenInfo(){

    }
    public static ScreenInfo instance(){
        if(instance==null){
            instance = new ScreenInfo();
        }
        return instance;
    }
}