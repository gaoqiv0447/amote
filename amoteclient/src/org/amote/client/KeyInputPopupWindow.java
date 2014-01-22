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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.gmote.common.Protocol.RemoteEvent;
import org.gmote.common.packet.RemoteEventPacket;
import org.gmote.common.packet.TextPacket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Key input popupWindow: 1. Send text to server; 2. recognize voice.
 * 
 * @author gaoqi
 */
public class KeyInputPopupWindow {
	static final private String TAG = "KeyInputPopupWindow";
	View root;
	PopupWindow popupWindow;
	EditText editText;
	ListView mList;
	ImageButton button_delete;
	ImageButton button_send;
	
	private int FileLength;  
	private int DownedFileLength=0;  
	private InputStream inputStream;  
	private URLConnection connection;  
	private OutputStream outputStream; 
	ProgressDialog xh_pDialog;  
	
	final Handler mHandler =  new Handler()
	{
		public void handleMessage(Message msg) {
			if (msg.what == 0x123) {
				popupWindow.update(width, editText.getHeight()*2 + button_delete.getHeight() + button_send.getHeight());
			}
			
			if (!Thread.currentThread().isInterrupted()) {  
                switch (msg.what) {  
                case 0:  
                	xh_pDialog.setMax(FileLength);  
//                    Log.i("file length ----------->", xh_pDialog.getMax()+"");    
                    break;  
                case 1:  
                	xh_pDialog.setProgress(DownedFileLength);  
                    int x=DownedFileLength*100/FileLength;  
                    break;  
                case 2:  
                	xh_pDialog.cancel();
                    Toast.makeText(mActivity.getApplicationContext(), "Download finished!", Toast.LENGTH_LONG).show(); 
                    String savePathString=Environment.getExternalStorageDirectory()+"/download/"+"VoiceSearch.apk";  
                    File tmpFile = new File(savePathString);
                    openFile(tmpFile);
                    break;  
                      
                default:  
                    break;  
                }  
            }   
		};
	};
	
	Activity mActivity = null;
	ActivityUtil mUtil = null;
	int width, height;

	public KeyInputPopupWindow(Activity v, ActivityUtil mUtil) {
		this.mActivity = v;
		this.mUtil = mUtil;
		
	}
	
	public void startPopupWindow(){
		root = mActivity.getLayoutInflater().inflate(R.layout.inputpopup, null);
		// get devices width
		DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels; 
        height = metric.heightPixels; 
        Log.i(TAG, "densityDpi" + metric.densityDpi);      
        
        xh_pDialog = new ProgressDialog(mActivity);
        xh_pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        xh_pDialog.setTitle(R.string.download_apk_title);
//        xh_pDialog.setMessage(R.string.download_apk_msg);
        xh_pDialog.setMessage("Downloading Google Voice Search apk...");
//        xh_pDialog.setIcon(R.drawable.img2); 
        xh_pDialog.setIndeterminate(false);
        xh_pDialog.setProgress(100);
        // set ProgressDialog can be cancel.  
        xh_pDialog.setCancelable(true);

        popupWindow = new PopupWindow(root, width, width);
//        popupWindow = new PopupWindow(mActivity);
//        popupWindow.setWidth(ViewGroup.LayoutParams.FILL_PARENT);  
//        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT); 
		
		popupWindow.setBackgroundDrawable(new BitmapDrawable());
		popupWindow.setFocusable(true);
		popupWindow.showAtLocation(root, Gravity.TOP | Gravity.CENTER, 0, 0);
		
        editText = (EditText) root.findViewById(R.id.entry);
		// this delete is not for client, but for server. If we send the wrong
		// word to server, using this button can delete it, and resend the new
		// word.
		button_delete = (ImageButton) root.findViewById(R.id.deletetext);
		button_delete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_DELETE));
			}
		});

		// send text and dismiss popup window
		button_send = (ImageButton) root.findViewById(R.id.sendtext);
		button_send.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mUtil.send(new TextPacket(editText.getText().toString()));
				editText.setText("");
//				popupWindow.dismiss();
			}
		});
		
		ImageButton button_left = (ImageButton) root.findViewById(R.id.inputleft);
		button_left.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_LEFT));
			}
		});
		
		ImageButton button_right = (ImageButton) root.findViewById(R.id.inputright);
		button_right.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mUtil.send(new RemoteEventPacket(RemoteEvent.REMOTE_RIGHT));
			}
		});

		ImageButton button_voice = (ImageButton) root
				.findViewById(R.id.voicesearch);
		button_voice.setOnClickListener(new OnClickListener() {
			public void onClick(View v2) {
				try {
					Log.i(TAG, "recognize_speech");
					Intent intent = new Intent(
							RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//					Log.i(TAG, intent.toString());
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
					intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
							"Recognize voice");
					mActivity.startActivityForResult(intent, 4321);

				} catch (ActivityNotFoundException e) {
					Toast.makeText(mActivity, R.string.not_install_voicesearch,
							Toast.LENGTH_LONG).show();
					// 让ProgressDialog显示  
					final Builder builder = new AlertDialog.Builder(mActivity);
//					builder.setIcon(R.drawable.btn_star);
					builder.setTitle(R.string.download_title);
					builder.setMessage(R.string.is_down_voicesearch);
					builder.setPositiveButton(R.string.download_ok, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int which) {
							xh_pDialog.show(); 
							new Thread(){
								public void run(){
									DownFile(new String("http://mdd.aisino.com:8080/Amote/update/VoiceSearch.apk"));
								}
							}.start();
							
						}
					});
					builder.setNegativeButton(R.string.download_cancel, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
						}
					});
					builder.create().show();
				}
			}
		});

		// only dismiss popup window
		ImageButton button_fade = (ImageButton) root.findViewById(R.id.fade);
		button_fade.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				popupWindow.dismiss();
			}
		});
		
		mList = (ListView) root.findViewById(R.id.ListViewVoiceSearch);
		openKeyboard();

		
	}

	/** Open input mpopupWindow.update(width, editText.getHeight()*2 + button_delete.getHeight() + button_send.getHeight());ethod keyboard. */
	private void openKeyboard() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
                public void run() {
                        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    	Message msg = new Message();
                    	msg.what = 0x123;
                    	mHandler.sendMessage(msg);
                }
        }, 1000);
	}
	
	void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == 4321 && resultCode == Activity.RESULT_OK) {
			final ArrayList<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			mList.setAdapter(new ArrayAdapter<String>(mActivity,
					android.R.layout.simple_list_item_1, results));
			mList.setVisibility(View.VISIBLE);
			mList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					Log.i(TAG, "onItemClick" + arg2);
					editText.setText(editText.getText().toString()
							+ results.get(arg2));
					mList.setVisibility(View.INVISIBLE);
					popupWindow.update(width, editText.getHeight()*2 + button_delete.getHeight() + button_send.getHeight());
				}
			});
			popupWindow.update(width, height);
		}
	}
	
	/**
	 * Download Google voiceSearch APK.
	 * @param httpUrl
	 * @return
	 */
	protected File DownFile(String httpUrl) {
        // TODO Auto-generated method stub
        File tmpFile = new File("/sdcard/download");
        if (!tmpFile.exists()) {
                tmpFile.mkdir();
        }
        final File file = new File("/sdcard/download/VoiceSearch.apk");
        try {
                URL url = new URL(httpUrl);
                try {
                        HttpURLConnection conn = (HttpURLConnection) url
                                        .openConnection();
                        InputStream is = conn.getInputStream();
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buf = new byte[1024*4];
                        conn.connect();
                        double count = 0;
                        Message message=new Message();  
                        if (conn.getResponseCode() >= 400) {
                                Toast.makeText(mActivity, R.string.timeout, Toast.LENGTH_SHORT)
                                                .show();
                        } else {
                        		FileLength=conn.getContentLength();  
	                        	message.what=0;  
	                            mHandler.sendMessage(message);  
                                while (count <= 100) {
                                        if (is != null) {
                                                int numRead = is.read(buf);
                                                if (numRead <= 0) {
                                                        break;
                                                } else {
                                                	DownedFileLength += numRead;
                                                        fos.write(buf, 0, numRead);
                                                }
                                        } else {
                                                break;
                                        }
                                        Message message1=new Message();  
                                        message1.what=1;  
                                        mHandler.sendMessage(message1);
                                }   
                                Message message2=new Message();  
                                message2.what=2;  
                                mHandler.sendMessage(message2);  
                        }
                        conn.disconnect();
                        fos.close();
                        is.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        } catch (MalformedURLException e) {
                e.printStackTrace();
        }

        return file;
	}

	// Open APK file
	private void openFile(File file) {
//	        Log.e("OpenFile", file.getName());
	        Intent intent = new Intent();
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.setAction(android.content.Intent.ACTION_VIEW);
	        intent.setDataAndType(Uri.fromFile(file),
	                        "application/vnd.android.package-archive");
	        mActivity.startActivity(intent);
	}
}
