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

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.aisino.server.Settings;

import android.content.Intent;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;


public class SettingsActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener,Preference.OnPreferenceChangeListener {

    private static String TAG = "SettingsActivity";
	private static final int BROWSE_PLUGIN_INSTALL_DIALOG = 0;
	private static final int BROWSE_PLUGIN_UNINSTALL_DIALOG = 1;
	private static final int SENSOR_PLUGIN_INSTALL_DIALOG = 2;
	private static final int SENSOR_PLUGIN_UNINSTALL_DIALOG = 3;
	private static final int PLUGIN_ERROR = 4;

	private CheckBoxPreference mAutoStartPref;
	private ListPreference mMouseStylePref;
	private PreferenceCategory mInstallCategory;
	private PreferenceCategory mUninstallCategory;
//	private PreferenceScreen mBrowsePref;
//	private PreferenceScreen mSensorPref;
	
	private boolean browse_plugin_installed;
	private boolean sensor_plugin_installed;
	
	private PluginManager mPluginHandler = null;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        PreferenceScreen prefSet = getPreferenceScreen();
		
		mAutoStartPref = (CheckBoxPreference) prefSet
					.findPreference(getString(R.string.setting_auto_start_key));
        mMouseStylePref = (ListPreference) prefSet
					.findPreference(getString(R.string.setting_mouse_style_key));
//		mBrowsePref = (PreferenceScreen) prefSet
//					.findPreference(getString(R.string.setting_plugin_browse_key));
//		mSensorPref = (PreferenceScreen) prefSet
//					.findPreference(getString(R.string.setting_plugin_sensor_key));
        prefSet.setOnPreferenceChangeListener(this);
        
		
		mInstallCategory = (PreferenceCategory) prefSet
					.findPreference(getString(R.string.setting_install_category_key));
		mUninstallCategory = (PreferenceCategory) prefSet
					.findPreference(getString(R.string.setting_uninstall_category_key));
		
//		mBrowsePref.setOnPreferenceClickListener(this);
//		mSensorPref.setOnPreferenceClickListener(this);
        
        Settings.getInstance(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()));

        
        updateWidgets();
		
//		if(browse_plugin_installed) {
//			mUninstallCategory.removePreference(mBrowsePref);
//			mInstallCategory.addPreference(mBrowsePref);
//		}else {
//			mInstallCategory.removePreference(mBrowsePref);
//			mUninstallCategory.addPreference(mBrowsePref);
//		}
//		
//		if(sensor_plugin_installed) {
//			mUninstallCategory.removePreference(mSensorPref);
//			mInstallCategory.addPreference(mSensorPref);
//		}else {
//			mInstallCategory.removePreference(mSensorPref);
//			mUninstallCategory.addPreference(mSensorPref);
//		}
		
		//mPluginHandler = new PluginManager(SettingsActivity.this.getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWidgets();
    }

    @Override
    protected void onDestroy() {
        Settings.releaseInstance();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
		
		Settings.setKeyAutoStart(mAutoStartPref.isChecked());
		Settings.setKeyMouseStyle(mMouseStylePref.getValue());
		Settings.setKeyBrowsePlugin(browse_plugin_installed);
		Settings.setKeySensorPlugin(sensor_plugin_installed);
        Settings.writeBack();
		Log.d(TAG,"======Settings write back");
		Log.d(TAG,"======AutoStart: " + String.valueOf(mAutoStartPref.isChecked()));
		Log.d(TAG,"======browse_plugin_installed: " + String.valueOf(browse_plugin_installed));
		Log.d(TAG,"======sensor_plugin_installed: " + String.valueOf(sensor_plugin_installed));
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case BROWSE_PLUGIN_INSTALL_DIALOG:
				return new AlertDialog.Builder(SettingsActivity.this)
					.setTitle(R.string.setting_plugin_install_dialog_title)
					.setMessage(R.string.setting_browse_plugin_install_dialog_msg)
					.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if(mPluginHandler.addBrowsePlugin()) {
//								mUninstallCategory.removePreference(mBrowsePref);
//								mInstallCategory.addPreference(mBrowsePref);
								browse_plugin_installed = true;
							}else {
								Log.e(TAG,"===ERROR: "+"install browse plugin error!");
							}

						}
					})
					.setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					})
					.create();
			case BROWSE_PLUGIN_UNINSTALL_DIALOG:
				return new AlertDialog.Builder(SettingsActivity.this)
					.setTitle(R.string.setting_plugin_uninstall_dialog_title)
					.setMessage(R.string.setting_plugin_uninstall_dialog_msg)
					.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if(mPluginHandler.delBrowsePlugin()) {
//								mInstallCategory.removePreference(mBrowsePref);
//								mUninstallCategory.addPreference(mBrowsePref);
								browse_plugin_installed = false;
							}else {
								Log.e(TAG,"===ERROR: "+"uninstall browse plugin error!");
							}

						}
					})
					.setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					})
					.create();
			case SENSOR_PLUGIN_INSTALL_DIALOG:
				return new AlertDialog.Builder(SettingsActivity.this)
					.setTitle(R.string.setting_plugin_install_dialog_title)
					.setMessage(R.string.setting_sensor_plugin_install_dialog_msg)
					.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if(mPluginHandler.addSensorPlugin()) {
//								mUninstallCategory.removePreference(mSensorPref);
//								mInstallCategory.addPreference(mSensorPref);
								sensor_plugin_installed = true;
							}else {
								Log.e(TAG,"===ERROR: "+"install sensor plugin error!");
							}
						}
					})
					.setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					})
					.create();
			case SENSOR_PLUGIN_UNINSTALL_DIALOG:
				return new AlertDialog.Builder(SettingsActivity.this)
					.setTitle(R.string.setting_plugin_uninstall_dialog_title)
					.setMessage(R.string.setting_plugin_uninstall_dialog_msg)
					.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if(mPluginHandler.delSensorPlugin()) {
//								mInstallCategory.removePreference(mSensorPref);
//								mUninstallCategory.addPreference(mSensorPref);
								sensor_plugin_installed = false;
							}else {
								Log.e(TAG,"===ERROR: "+"uninstall sensor plugin error!");
							}
						}
					})
					.setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					})
					.create();
		}
		return null;
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(mPluginHandler == null) {
			mPluginHandler = new PluginManager(SettingsActivity.this.getApplicationContext());
		}

//		if(preference == mBrowsePref) {
//			Log.d(TAG,"==this is browse plugin ");
//			if(browse_plugin_installed) {
//				showDialog(BROWSE_PLUGIN_UNINSTALL_DIALOG);
//			}else {
//				showDialog(BROWSE_PLUGIN_INSTALL_DIALOG);
//			}
//			return true;
//		}else if(preference == mSensorPref) {
//			Log.d(TAG,"===this is sensor plugin");
//			if(sensor_plugin_installed) {
//				showDialog(SENSOR_PLUGIN_UNINSTALL_DIALOG);
//			}else {
//				showDialog(SENSOR_PLUGIN_INSTALL_DIALOG);
//			}
//			return true;
//		} else {
//			return false;
//		}
		
		return false;

	}
	
	@Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    private void updateWidgets() {
		mAutoStartPref.setChecked(Settings.getKeyAutoStart());
		mMouseStylePref.setValue(Settings.getKeyMouseStyle());
		browse_plugin_installed = Settings.getKeyBrowsePlugin();
		sensor_plugin_installed = Settings.getKeySensorPlugin();
    }
}
