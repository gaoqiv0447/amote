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

import java.lang.Process;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import android.content.res.AssetManager;
import android.content.Context;
import android.util.Log;

/**
  * how to use this class:
  * first of all,new a PluginManager pm;
  * add browse plugin: pm.addBrowsePlugin();
  * del browse plugin: pm.delBrowsePlugin();
  * add sensor plugin: pm.addSensorsPlugin();
  * del sensor plugin: pm.delSensorsPlugin();
**/
public class PluginManager {
	
	private static final String TAG = "PluginManager";
	private static final boolean DEBUG_S = true;
	
	private static String sensorName = null;
	private static boolean keep = false;

	private static final String VOLD_ASSETS_NAME = "vold";
	private static final String VOLD_PATH_TMP = "/data/data/com.aisino.server/files/vold";
	private static final String VOLD_PATH_FINAL = "/system/bin/vold";
	private static final String SENSOR_ASSETS_NAME = "sensors.default.so";
	private static final String SENSOR_PATH_TMP = "/data/data/com.aisino.server/files/sensors.default.so";
	private static final String SENSOR_PATH_FINAL = "/system/lib/hw/sensors.default.so";
	
	private static final String REMOUNT_SYSTEM_COMMAND = "mount -o remount rw /system";
	private static final String BACKUP_VOLD_COMMAND = "mv /system/bin/vold /system/bin/vold_backup";
	private static final String RECOVER_VOLD_COMMAND = "mv /system/bin/vold_backup /system/bin/vold";
	private static final String SENSOR_LIB_COMMAND = "mv /system/lib/hw/%s /system/lib/hw/%s";
	private static final String COPY_COMMAND = "cat %s > %s";
	
	// type
	private static final int VOLD_T = 0;
	private static final int SENSOR_T = 1;
	
	private Context mContext = null;
	
	public PluginManager(Context context) {
		mContext = context;
		keep = isRoot(); // is rooted
		if(keep) {
			// remount
			keep = reMountSys();
			Log.d(TAG,"===remount /system: "+String.valueOf(keep));
		}else {
			Log.d(TAG,"=======Error: your device has not been rooted!");
		}
			
	}
	
	private boolean execCommand(String command) {
		Process p = null;
		DataOutputStream os = null;
		InputStream in = null; 
		StringBuffer result = new StringBuffer(); 
		try {
			p = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(p.getOutputStream());
			if(command != null) {
				Log.d(TAG,"===execute command: "+"\""+command+"\"");
				os.writeBytes(command + "\n");
			}
			os.writeBytes("exit\n");
			os.flush();
			int value = p.waitFor();
			if(DEBUG_S) {
				// read inputstream
				in = p.getInputStream(); 
				byte[] re = new byte[1024]; 
				while (in.read(re) != -1) { 
					result = result.append(new String(re)); 
				}
				Log.d(TAG,"result: "+result.toString());
			}

			// return 
			if(value == 0) {
				return true;
			}else {
				return false;
			}
		} catch(Exception e) {
				System.err.println(e.toString());
				return false;
		} finally {
			try {
				if(os != null){
					os.close();
				}
				if(p != null) {
					p.destroy();
				}
			} catch(Exception e) {}
		}
	}
	
	private boolean isRoot() {
		return execCommand(null);
	}
	
	private void hackRoot() {
	}
	
	private boolean reMountSys() {
		return execCommand(REMOUNT_SYSTEM_COMMAND);
	}

	public boolean addBrowsePlugin() {
		if(DEBUG_S) {
			Log.d(TAG,"====== add browse plugin ======");
		}
		boolean ok = false;
		if(keep=reMountSys()) {
			// step one,backup
			ok = execCommand(BACKUP_VOLD_COMMAND);
			Log.d(TAG,"===execute BACKUP_VOLD_COMMAND: "+String.valueOf(ok));
			// step two,copy ours to system/bin
			if(ok) {
				// do copy working
				ok = copy(VOLD_T);
				if(!ok) {
					delBrowsePlugin();
				}
			}
		}
		return ok;
	}
	
	public boolean delBrowsePlugin() {
		if(DEBUG_S) {
			Log.d(TAG,"====== del browse plugin ======");
		}
		boolean ok = false;
		if(keep=reMountSys()) {
			ok = execCommand(RECOVER_VOLD_COMMAND);
		}
		return ok;

	}
	private String getSensorLibName() {
		// this method is needed to be optimized
		String libpath = "/system/lib/hw/";
		File f = new File(libpath); 
		String []filelist = f.list(); 
		for(int i=0;i <filelist.length;i++) {
			if(filelist[i].contains("sensors") && filelist[i].endsWith(".so")) {
				if(!filelist[i].contains("goldfish")) {
					return filelist[i];
				}
			}
		}
		return null;
	}
	public boolean addSensorPlugin() {
		if(DEBUG_S) {
			Log.d(TAG,"====== add sensor plugin ======");
		}
		boolean ok = false;
		if(keep=reMountSys()) {
			sensorName = getSensorLibName();
			if(DEBUG_S) {
				Log.d(TAG,"===device sensor lib name: "+sensorName);
			}
			if(sensorName != null) {
				// step one: backup
				ok = execCommand(String.format(SENSOR_LIB_COMMAND,sensorName,sensorName+"_backup"));
				// step two: copy new
				if(ok) {
					// do copy working
					ok = copy(SENSOR_T);
					if(!ok) {
						delSensorPlugin();
					}
				}
			}
		}
		return ok;

	}
	
	public boolean delSensorPlugin() {
		if(DEBUG_S) {
			Log.d(TAG,"====== del sensor plugin ======");
		}
		boolean ok = false;
		if(keep=reMountSys()) {
			if(sensorName != null) {
				ok = execCommand(String.format(SENSOR_LIB_COMMAND,sensorName+"_backup",sensorName));
			}
		}
		return ok;

	}
	
	/**
	 * the copy() method do two things:
	 * first: copy assets files to the app dada directory(data/data/com.aisino.server/files):
     * VOLD_ASSETS_NAME ---> VOLD_PATH_TMP
	 * second: copy tmep files to system/bin or /system/lib/hw(this should be done as su):
	 * VOLD_PATH_TMP ---> VOLD_PATH_FINAL
	*/ 
	private boolean copy(int type) {
		String input = null;
		String tmpput = null;
		String output = null;
		if(type == VOLD_T) {
			input = VOLD_ASSETS_NAME;
			tmpput = VOLD_PATH_TMP;
			output = VOLD_PATH_FINAL;
		}else if(type == SENSOR_T) {
			input = SENSOR_ASSETS_NAME;
			tmpput = SENSOR_PATH_TMP;
			output = SENSOR_PATH_FINAL;
		}else {
			return false;
		}
		if(DEBUG_S) {
			Log.d(TAG,"====== copying ======");
			Log.d(TAG,"===input: " + input);
			Log.d(TAG,"===output: " + output);
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			in = mContext.getAssets().open(input);
			File outFile = new File(tmpput); 
			out = new FileOutputStream(tmpput);
			//transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[512];
			int length;
			while((length = in.read(buffer))>0){
				out.write(buffer, 0, length);
			}
			out.flush();
			boolean re = execCommand(String.format(COPY_COMMAND,tmpput,output));
			if(re) {
				execCommand("chmod 777 "+output);
			}
			return re;
			
		} catch(Exception e) {
			Log.d(TAG,"error:when copy "+input+"\n"+e.toString());
			return false;
		} finally {
			try {
				if(out != null) {
					out.close();
				}
				if(in != null) {
					in.close();
				}
				if(DEBUG_S) {
					Log.d(TAG,"====== copying end ======");
				}
			}catch(Exception e){}
		}

    }
	/*
	private boolean copy2(int type) {
		String input = null;
		String output = null;
		if(type == VOLD_T) {
			input = VOLD_TO_PATH;//PRE_PATH + VOLD_ASSETS_NAME;
			output = "/system/bin/vold__";
		}else if(type == SENSOR_T) {
			input = PRE_PATH + SENSOR_ASSETS_NAME;
			output = SENSOR_TO_PATH;
		}else {
			return false;
		}
		if(DEBUG_S) {
			Log.d(TAG,"====== copying ======");
			Log.d(TAG,"===input: " + input);
			Log.d(TAG,"===output: " + output);
		}
		return execCommand(String.format(COPY_COMMAND,input,output));
	}
	*/
}
