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

package org.amote.client.android;

import android.util.Log;

public class ScreenInfo {
    public static int width;
    public static int height;
    /*
     *xlarge screens are at least 960dp x 720dp
    * large screens are at least 640dp x 480dp
    * normal screens are at least 470dp x 320dp
    * small screens are at least 426dp x 320dp

     * */
    public final static int E_LARGE_SCREEN = 1;
    public final static int LARGE_SCREEN = 2;
    public final static int NORMAL_SCREEN = 3;
    public final static int SMALL_SCREEN = 4;
    private final static int LDPI = 120;
    private final static int MDPI = 160;
    private final static int HDPI = 240;
    private final static int XHDPI = 320;
    private final static int NUM_240 = 240;
    private final static int NUM_320 = 320;
    private final static int NUM_400 = 400;
    private final static int NUM_432 = 432;
    private final static int NUM_480 = 480;
    private final static int NUM_600 = 600;
    private final static int NUM_640 = 640;
    private final static int NUM_768 = 768;
    private final static int NUM_800 = 800;
    private final static int NUM_854 = 854;
    private final static int NUM_1024 = 1024;
    public static int server_dip = 120;
    public static int server_screen_type = 4;
    public static int GESTURE_MIN_DIS = 4;
	public static int ServerWidth;
	public static int ServerHeight;

    public static void handleScreenType(int w,int h,int d){
        server_dip = d;
		ServerWidth = w;
		ServerHeight = h;
        Log.d("ScreenInfo", "------>w="+w+" h="+h+" d="+d);
        switch(d){
            case LDPI:
                switch(h){
                    case NUM_240:
                        if(w==NUM_320){
                            server_screen_type = SMALL_SCREEN;
                        }else{
                            server_screen_type = NORMAL_SCREEN;
                        }
                        break;
                    case NUM_480:
                        server_screen_type = LARGE_SCREEN;
                        GESTURE_MIN_DIS = 8;
                        break;
                    case NUM_600:
                        server_screen_type = E_LARGE_SCREEN;
                        GESTURE_MIN_DIS = 12;
                        break;
                    default:
                            server_screen_type = NORMAL_SCREEN;
                            GESTURE_MIN_DIS = 4;
                            break;
                }
                break;
            case MDPI:
                switch(h){
                    case NUM_320:
                        server_screen_type = NORMAL_SCREEN;
                        break;
                    case NUM_480:
                    case NUM_600:
                        server_screen_type = LARGE_SCREEN;
                        GESTURE_MIN_DIS = 8;
                        break;
                    case NUM_768:
                    case NUM_800:
                        server_screen_type = E_LARGE_SCREEN;
                        GESTURE_MIN_DIS = 12;
                        break;
                }
                break;
            case HDPI:
                switch(h){
                    case NUM_480:
                        if(w==NUM_640)
                            server_screen_type = SMALL_SCREEN;
                        else{
                            server_screen_type = NORMAL_SCREEN;
                            GESTURE_MIN_DIS = 12;
                        }
                        break;
                    case NUM_600:
                        server_screen_type = NORMAL_SCREEN;
                        GESTURE_MIN_DIS = 12;
                        break;
                    default:
                        server_screen_type = E_LARGE_SCREEN;
                        GESTURE_MIN_DIS = 16;
                        break;
                }
                break;
            case XHDPI:
                switch(h){
                    case NUM_640:
                        server_screen_type = NORMAL_SCREEN;
                        GESTURE_MIN_DIS = 12;
                        break;
                    default:
                        server_screen_type = E_LARGE_SCREEN;
                        GESTURE_MIN_DIS = 20;
                        break;
                }
                break;
        }
    }
}