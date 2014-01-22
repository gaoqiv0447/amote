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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


public class Settings {

    public static final String SETTINGS_AUTO_START_KEY = "AutoStart";
	public static final String SETTING_MOUSE_STYLE_KEY = "MouseStyle";
	public static final String SETTING_BROWSE_PLUGIN_KEY = "browse_plugin";
	public static final String SETTNG_SENSOR_PLUGIN_KEY = "sensor_plugin";
	public static final String SETTING_SHUT_DOWN = "shut_down";
	public static final boolean JUST_FOR_OURSELF = true;

	private static boolean mAutoStart;
	private static String mMouseStyle;
	private static boolean mBrowsePlugin;
	private static boolean mSensorPlugin;
	private static boolean mIsShutDown;

    private static Settings mInstance = null;

    private static int mRefCount = 0;

    private static SharedPreferences mSharedPref = null;

    protected Settings(SharedPreferences pref) {
        mSharedPref = pref;
        initConfs();
    }

    public static Settings getInstance(SharedPreferences pref) {
        if (mInstance == null) {
            mInstance = new Settings(pref);
        }
        assert (pref == mSharedPref);
        mRefCount++;
        return mInstance;
    }

    public static void writeBack() {
        Editor editor = mSharedPref.edit();
		editor.putBoolean(SETTINGS_AUTO_START_KEY,mAutoStart);
		editor.putString(SETTING_MOUSE_STYLE_KEY,mMouseStyle);
		editor.putBoolean(SETTING_BROWSE_PLUGIN_KEY,mBrowsePlugin);
		editor.putBoolean(SETTNG_SENSOR_PLUGIN_KEY,mSensorPlugin);
		editor.putBoolean(SETTING_SHUT_DOWN,mIsShutDown);
        editor.commit();
    }

    public static void releaseInstance() {
        mRefCount--;
        if (mRefCount == 0) {
            mInstance = null;
        }
    }

    private void initConfs() {
		mAutoStart = mSharedPref.getBoolean(SETTINGS_AUTO_START_KEY,true);
		mMouseStyle = mSharedPref.getString(SETTING_MOUSE_STYLE_KEY,"1");
		mBrowsePlugin = mSharedPref.getBoolean(SETTING_BROWSE_PLUGIN_KEY,false);
		mSensorPlugin = mSharedPref.getBoolean(SETTNG_SENSOR_PLUGIN_KEY,false);
		mIsShutDown = mSharedPref.getBoolean(SETTING_SHUT_DOWN,false);
    }

	public static boolean getKeyAutoStart() {
		return mAutoStart;
	}

	public static void setKeyAutoStart(boolean v) {
		if (mAutoStart == v) return;
		mAutoStart = v;
	}

	public static String getKeyMouseStyle() {
		return mMouseStyle;
	}

	public static void setKeyMouseStyle(String v) {
		if (mMouseStyle == v) return;
		mMouseStyle = v;
	}

	public static boolean getKeyBrowsePlugin() {
		return mBrowsePlugin;
	}

	public static void setKeyBrowsePlugin(boolean v) {
		if (mBrowsePlugin == v) return;
		mBrowsePlugin = v;
	}

	public static boolean getKeySensorPlugin() {
		return mSensorPlugin;
	}

	public static void setKeySensorPlugin(boolean v) {
		if (mSensorPlugin == v) return;
		mSensorPlugin = v;
	}

	public static boolean getKeyShutDown() {
		return mIsShutDown;
	}

	public static void setKeyShutDown(boolean v) {
		if(mIsShutDown == v) return;
		mIsShutDown = v;
	}
}
