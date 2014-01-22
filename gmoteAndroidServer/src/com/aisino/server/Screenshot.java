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

import java.io.File;
import java.lang.Process;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.Error;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

public class Screenshot
{
	static Bitmap mBitmap = null;
	static byte[] byteArray = null;
	static boolean IS_DEBUG = true;
	static boolean CM = true;
    private static final String SCREENSHOT_BUCKET_NAME = "/data/data/com.aisino.server/files";
	
	static {
		try{
			System.loadLibrary("screenshotjni");
		}catch(Error ex){
			System.err.println("======ERROR: "+ ex.toString());
		}
    	
    };
	static native void native_take_screenshot(String file,int width,int height);
	
    public static byte[] getScreenshot(Context context,int width,int height)
    {
		takeScreenshot(context,width,height);
		return byteArray;
    }

    private static void takeScreenshot(Context context,int width,int height)
    {
		String mRawScreenshot = String.format("%s/screenshot.bmp", SCREENSHOT_BUCKET_NAME);
		if(!CM) {
			mRawScreenshot = String.format("%s/screenshot.png", SCREENSHOT_BUCKET_NAME);
		}

		Log.d("CMScreenshot","======path:"+mRawScreenshot);
        try
        {	
			// prepare
			Process p = null;
			DataOutputStream os = null;
			try {
				p = Runtime.getRuntime().exec("su");
				os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("chmod 755 /dev/graphics/fb0" + "\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
			} catch(Exception e) {
				System.err.println("======no root!======");
			} finally {
				try {
					if(os != null){
						os.close();
					}
					if(p != null) {
						p.destroy();
					}
				} catch(Exception e) {
				}
			}

			// take screenshot
			try {
				native_take_screenshot(mRawScreenshot,width,height);
			}catch(Exception ex) {
				System.err.println("====jni screenshot failed: "+ ex.toString());
				return;
			}
			
			File tmpshot = new File(mRawScreenshot);
			if(!tmpshot.exists()){
				System.err.println("======screenshot: screenshot.png does not exists!");
				return;
			}
			
			
			// convert bitmap to byte[]
			try
			{
			  //BitmapFactory.Options options = new BitmapFactory.Options();
			  //options.inScaled = true;
			mBitmap = BitmapFactory.decodeFile(mRawScreenshot);
			}
			catch(Exception e)
			{
				System.err.println("======decodeFile error: "+e.toString());
			}
			//mBitmap = BitmapFactory.decodeFile(tmpshot.getAbsolutePath(),);
			if (mBitmap == null) {
               throw new Exception("======Unable to save screenshot: mBitmap = "+mBitmap);
            }
			
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byteArray = stream.toByteArray();
			
            tmpshot.delete();
        }
        catch (Exception ex)
        {
			System.err.println("======Screenshot Error: "+ ex.toString());
        }
    }
}

