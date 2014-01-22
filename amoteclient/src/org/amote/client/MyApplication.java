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

package org.amote.client;

import android.app.Application;
import android.view.WindowManager;

public class MyApplication extends Application {

	private WindowManager.LayoutParams wmParams=new WindowManager.LayoutParams();

	public WindowManager.LayoutParams getMywmParams(){
		return wmParams;
	}
}
