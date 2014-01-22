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
//
///*
// *
// * 浣跨敤鏂瑰紡锛� * 闇�鍦ˋndroidManifest.xml涓坊鍔� * <uses-permission android:name="android.permission.INTERNET"/>
// *	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
// *  璋冪敤锛� * 	UpdateSoft uS = new UpdateSoft(this, "http://mdd.aisino.com:8080/video/wenzhouyidong.xml");
// *  uS.start();
// *
// */
//import java.io.BufferedInputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//
//import org.xmlpull.v1.XmlPullParser;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.app.AlertDialog.Builder;
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.DialogInterface.OnClickListener;
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Message;
//import android.util.Log;
//import android.util.Xml;
//import android.widget.Toast;
//
public class UpdateSoft {
//
//	private String serverurl;
//	private Context context;
//	protected static final String TAG = "UpdateSoft";
//	private UpdataInfo info;
//	protected static final int UPDATA_CLIENT = 0;
//	protected static final int GET_UNDATAINFO_ERROR = 1;
//	protected static final int DOWN_ERROR = 2;
//
//	private String strGetUnDataInfoErr;
//	private String strDownErr;
//	private String strDialogTitle;
//	private String strDialogOk;
//	private String strDialogCancel;
//
//	// private String ;
//
//	public UpdateSoft(Context context, String serverurl) {
//		this.context = context;
//		this.serverurl = serverurl;
//		strGetUnDataInfoErr = "鑾峰彇鏈嶅姟鍣ㄦ洿鏂颁俊鎭け璐�;
//		strDownErr = "涓嬭浇鏂扮増鏈け璐�;
//		strDialogTitle = "鐗堟湰鍗囩骇";
//		strDialogOk = "纭畾";
//		strDialogCancel = "鍙栨秷";
//	}
//
//	// set string to make it internationalization
//	public void setString(String strGetUnDataInfoErr, String strDownErr, String strDialogTitle, String strDialogOk, String strDialogCancel){
//		this.strGetUnDataInfoErr = strGetUnDataInfoErr;
//		this.strDownErr = strDownErr;
//		this.strDialogTitle =  strDialogTitle;
//		this.strDialogOk = strDialogOk;
//		this.strDialogCancel = strDialogCancel;
//		return;
//	}
//	public void start(){
//		CheckVersionTask chTask = new CheckVersionTask();
//		handler.post(chTask);
//	}
//
//	// 浠庢湇鍔″櫒涓嬭浇apk:
//	public static File getFileFromServer(String path, ProgressDialog pd)
//			throws Exception {
//		Log.i(TAG, "getFileFromServer");
//		// 濡傛灉鐩哥瓑鐨勮瘽琛ㄧず褰撳墠鐨剆dcard鎸傝浇鍦ㄦ墜鏈轰笂骞朵笖鏄彲鐢ㄧ殑
//		if (Environment.getExternalStorageState().equals(
//				Environment.MEDIA_MOUNTED)) {
//			URL url = new URL(path);
//			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//			conn.setConnectTimeout(5000);
//			// 鑾峰彇鍒版枃浠剁殑澶у皬
//			pd.setMax(conn.getContentLength());
//			InputStream is = conn.getInputStream();
//			File file = new File(Environment.getExternalStorageDirectory(),
//					"wenzhou.apk");
//			FileOutputStream fos = new FileOutputStream(file);
//			BufferedInputStream bis = new BufferedInputStream(is);
//			byte[] buffer = new byte[1024];
//			int len;
//			int total = 0;
//			while ((len = bis.read(buffer)) != -1) {
//				fos.write(buffer, 0, len);
//				total += len;
//				// 鑾峰彇褰撳墠涓嬭浇閲�				pd.setProgress(total);
//			}
//			fos.close();
//			bis.close();
//			is.close();
//			return file;
//		} else {
//			return null;
//		}
//	}
//
//	public class UpdataInfo {
//		private String version = "1.0";
//		private String url = "";
//		private String description = "";
//
//		public UpdataInfo() {
//			// TODO Auto-generated constructor stub
//		}
//
//		public void setVersion(String ver) {
//			this.version = ver;
//		}
//
//		public void setUrl(String url) {
//			this.url = url;
//		}
//
//		public void setDescription(String desc) {
//			this.description = desc;
//		}
//
//		public String getVersion() {
//			return this.version;
//		}
//
//		public String getUrl() {
//			return this.url;
//		}
//
//		public String getDescription() {
//			return this.description;
//		}
//	}
//
//	// 鑾峰彇鏈嶅姟鍣ㄧ鐨勭増鏈彿锛�	/*
//	 * 鐢╬ull瑙ｆ瀽鍣ㄨВ鏋愭湇鍔″櫒杩斿洖鐨剎ml鏂囦欢 (xml灏佽浜嗙増鏈彿)
//	 */
//	public UpdataInfo getUpdataInfo(InputStream is) throws Exception {
//		Log.i(TAG, "getUpdataInfo");
//		XmlPullParser parser = Xml.newPullParser();
//		parser.setInput(is, "utf-8");// 璁剧疆瑙ｆ瀽鐨勬暟鎹簮
//		int type = parser.getEventType();
//		UpdataInfo info = new UpdataInfo();// 瀹炰綋
//		while (type != XmlPullParser.END_DOCUMENT) {
//			switch (type) {
//			case XmlPullParser.START_TAG:
//				if ("version".equals(parser.getName())) {
//					info.setVersion(parser.nextText()); // 鑾峰彇鐗堟湰鍙�				} else if ("url".equals(parser.getName())) {
//					info.setUrl(parser.nextText()); // 鑾峰彇瑕佸崌绾х殑APK鏂囦欢
//				} else if ("description".equals(parser.getName())) {
//					info.setDescription(parser.nextText()); // 鑾峰彇璇ユ枃浠剁殑淇℃伅
//				}
//				break;
//			}
//			type = parser.next();
//		}
//		return info;
//	}
//
//	/*
//	 * 1. 鑾峰彇褰撳墠绋嬪簭鐨勭増鏈彿
//	 */
//	private String getVersionName() throws Exception {
//		// 鑾峰彇packagemanager鐨勫疄渚�		PackageManager packageManager = context.getPackageManager();
//		// getPackageName()鏄綘褰撳墠绫荤殑鍖呭悕锛�浠ｈ〃鏄幏鍙栫増鏈俊鎭�		PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(),
//				0);
//		return packInfo.versionName;
//	}
//
//	// 鍖归厤銆佷笅杞姐�鑷姩瀹夎锛�	/*
//	 * 浠庢湇鍔″櫒鑾峰彇xml瑙ｆ瀽骞惰繘琛屾瘮瀵圭増鏈彿
//	 */
//	public class CheckVersionTask implements Runnable {
//
//		public void run() {
//			try {
//				URL url = new URL(serverurl);
//				Log.i(TAG, "URL*********************************************" + url.toString());
//				HttpURLConnection conn = (HttpURLConnection) url
//						.openConnection();
//				conn.setConnectTimeout(5000);
//				InputStream is = conn.getInputStream();
//				info = getUpdataInfo(is);
//				Log.i(TAG, "info*********************************************" + info.getVersion());
//				if (info.getVersion().equals(getVersionName())) {
//					Log.i(TAG, "鐗堟湰鍙风浉鍚屾棤闇�崌绾�);
//				} else {
//					Log.i(TAG, "鐗堟湰鍙蜂笉鍚�,鎻愮ず鐢ㄦ埛鍗囩骇 ");
//					Message msg = new Message();
//					msg.what = UPDATA_CLIENT;
//					handler.sendMessage(msg);
//				}
//			} catch (Exception e) {
//				// 寰呭鐞�				Message msg = new Message();
//				msg.what = GET_UNDATAINFO_ERROR;
//				handler.sendMessage(msg);
//				e.printStackTrace();
//			}
//		}
//	}
//
//	Handler handler = new Handler() {
//		@Override
//		public void handleMessage(Message msg) {
//			// TODO Auto-generated method stub
//			super.handleMessage(msg);
//			switch (msg.what) {
//			case UPDATA_CLIENT:
//				// 瀵硅瘽妗嗛�鐭ョ敤鎴峰崌绾х▼搴�				showUpdataDialog();
//				break;
//			case GET_UNDATAINFO_ERROR:
//				// 鏈嶅姟鍣ㄨ秴鏃�				Toast.makeText(context.getApplicationContext(), strGetUnDataInfoErr,
//						1).show();
//				break;
//			case DOWN_ERROR:
//				// 涓嬭浇apk澶辫触
//				Toast.makeText(context.getApplicationContext(), strDownErr, 1)
//						.show();
//				break;
//			}
//		}
//	};
//
//	/*
//	 *
//	 * 寮瑰嚭瀵硅瘽妗嗛�鐭ョ敤鎴锋洿鏂扮▼搴�	 *
//	 * 寮瑰嚭瀵硅瘽妗嗙殑姝ラ锛�1.鍒涘缓alertDialog鐨刡uilder. 2.瑕佺粰builder璁剧疆灞炴�, 瀵硅瘽妗嗙殑鍐呭,鏍峰紡,鎸夐挳
//	 * 3.閫氳繃builder 鍒涘缓涓�釜瀵硅瘽妗�4.瀵硅瘽妗唖how()鍑烘潵
//	 */
//	protected void showUpdataDialog() {
//		Log.i(TAG, "showUpdataDialog");
//		AlertDialog.Builder builer = new Builder(context);
//		builer.setTitle(strDialogTitle);
//		builer.setMessage(info.getDescription());
//		// 褰撶偣纭畾鎸夐挳鏃朵粠鏈嶅姟鍣ㄤ笂涓嬭浇 鏂扮殑apk 鐒跺悗瀹夎
//		builer.setPositiveButton(strDialogOk, new OnClickListener() {
//			public void onClick(DialogInterface dialog, int which) {
//				Log.i(TAG, "涓嬭浇apk,鏇存柊");
//				downLoadApk();
//			}
//		});
//		// 褰撶偣鍙栨秷鎸夐挳鏃惰繘琛岀櫥褰�		builer.setNegativeButton(strDialogCancel, new OnClickListener() {
//			public void onClick(DialogInterface dialog, int which) {
//				// TODO Auto-generated method stub
//
//			}
//		});
//		AlertDialog dialog = builer.create();
//		dialog.show();
//	}
//
//	/*
//	 * 浠庢湇鍔″櫒涓笅杞紸PK
//	 */
//	protected void downLoadApk() {
//		Log.i(TAG, "downLoadApk");
//		final ProgressDialog pd; // 杩涘害鏉″璇濇
//		pd = new ProgressDialog(context);
//		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//		pd.setMessage("姝ｅ湪涓嬭浇鏇存柊");
//		pd.show();
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					File file = getFileFromServer(info.getUrl(), pd);
//					sleep(3000);
//					installApk(file);
//					pd.dismiss(); // 缁撴潫鎺夎繘搴︽潯瀵硅瘽妗�				} catch (Exception e) {
//					Message msg = new Message();
//					msg.what = DOWN_ERROR;
//					handler.sendMessage(msg);
//					e.printStackTrace();
//				}
//			}
//		}.start();
//	}
//
//	// 瀹夎apk
//	protected void installApk(File file) {
//		Log.i(TAG, "installApk");
//		Intent intent = new Intent();
//		// 鎵ц鍔ㄤ綔
//		intent.setAction(Intent.ACTION_VIEW);
//		// 鎵ц鐨勬暟鎹被鍨�		intent.setDataAndType(Uri.fromFile(file),
//				"application/vnd.android.package-archive");
//		context.startActivity(intent);
//	}
}
