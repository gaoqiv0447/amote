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

import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.LaunchUrlPacket;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class WebBrowser extends Activity implements BaseActivity {

	ActivityUtil mUtil = null;
	ProgressDialog mDialog = null;

	WebView webView = null;
	EditText urlView = null;
	Button goButton = null;
	Button backButton = null;
	View progressView = null;

	WebViewClient webViewClient = new WebViewClient() {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			urlView.setText(url);
			goButton.setVisibility(View.GONE);
			progressView.setVisibility(View.VISIBLE);
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			progressView.setVisibility(View.GONE);
			goButton.setVisibility(View.VISIBLE);
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mUtil = new ActivityUtil();
		mUtil.onCreate(icicle, this);
		setContentView(R.layout.web_browser);
		webView = (WebView) findViewById(R.id.web_browser_view);
		webView.setWebViewClient(webViewClient);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl("http://www.baidu.com/");

		// ((ViewGroup)findViewById(R.id.web_browser_zoom)).addView(webView.getZoomControls());
		// // some pad error, null pointer
		webView.invokeZoomPicker();
		try {
			urlView = (EditText) findViewById(R.id.web_browser_url);
			urlView.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						webView.loadUrl(getUrl());
						return true;
					}
					return false;
				}
			});
		} catch (Exception e) {
			System.out.println("Exception 2!" + e.toString());
		}
		initializeUi();

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return mUtil.onCreateDialog(id);
	}

	@Override
	public void onStart() {
		super.onStart();
		mUtil.onStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		mUtil.onStop();
		webView.clearCache(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		mUtil.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mUtil.onPause();
	}

	public void handleReceivedPacket(AbstractPacket reply) {

	}

	String getUrl() {
		return URLUtil.guessUrl(urlView.getText().toString());
	}

	void initializeUi() {
		progressView = findViewById(R.id.progress);
		goButton = (Button) findViewById(R.id.web_browser_go);
		goButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				webView.loadUrl(getUrl());
			}
		});

		backButton = (Button) findViewById(R.id.web_back);
		backButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		Button button = (Button) findViewById(R.id.web_browser_launch);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mUtil.send(new LaunchUrlPacket(getUrl()));
				webView.loadUrl(getUrl());
			}
		});

		webView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				v.requestFocusFromTouch();
				return false;
			}
		});

		webView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				webView.requestFocus();
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView.canGoBack()) {
				webView.goBack();
			} else {
				finish();
			}
			return true;
		}
		return false;
	}
}
