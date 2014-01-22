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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MouseContainer extends LinearLayout {

    public MouseContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void initialize() {

        invalidate();
        requestLayout();
    }

}
