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

import org.gmote.common.ScreenInfo;

import com.aisino.server.RemoteApplication;

import android.content.Context;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

public class MouseArrow extends ImageView {

    private static final String TAG = "MouseArrow";
    private WindowManager wm=(WindowManager)getContext().getApplicationContext().getSystemService("window");
    //此wmParams为获取的全局变量，用以保存悬浮窗口的属性
    private WindowManager.LayoutParams wmParams = ((RemoteApplication)getContext().getApplicationContext()).getMywmParams();
    public MouseArrow(Context context) {
        super(context);
    }

    public synchronized void handlerUpdatePosition(int x,int y){
       // Log.d(TAG, "######-->handlerUpdatePosition("+x+","+y+")");
        //更新浮动窗口位置参数
        if(x == ScreenInfo.width){
            x = x - 15;
        }
        if(y == ScreenInfo.height){
            y= y - 10;
        }
        wmParams.x= x;
        wmParams.y= y;
        wm.updateViewLayout(this, wmParams);
     }
}
