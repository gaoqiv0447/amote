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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gmote.common.ServerInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class IpManually extends Activity {
	private ListView mListView = null;
	private EditText mAddressEdit = null;
	private EditText mTCPEdit = null;
	private EditText mUDPEdit = null;
	private ImageButton mRemoveBtn = null;
	private ImageButton mConnectBtn = null;
	
	private static final String FILE_NAME = "IP.txt";
	private IPAdapter mIPAdapter;
	private List<String> mIPList = new ArrayList<String>();
	private String[] mIPContentList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_form);
		
		//write("111.111.11.11-222.222.22.22-333.333.33.33-444.444.44.44-555.555.55.55");
		InitView();
	}
	
	private void InitView() {
		String  ip_content = ReadFile();
		mIPContentList = ip_content.split("-");
		for(int i = 0; i < mIPContentList.length; i++)
		{
			mIPList.add(mIPContentList[i]);
		}
		
		mIPAdapter= new IPAdapter(this);
		mListView = (ListView)findViewById(R.id.first_serverlist_view);
		mListView.setAdapter(mIPAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				for(int i = 0; i< mIPList.size();i++)
				{
					mIPAdapter.colorStatus.set(i,"#FFFFFF");
				}
				mIPAdapter.colorStatus.set(arg2,"#C0FF3E");
				mIPAdapter.dataLoaded();
				mAddressEdit.setText(mIPList.get(arg2));
			}
			
		});
		
		mAddressEdit = (EditText)findViewById(R.id.server_edit);
		mAddressEdit.setText(mIPList.get(0));
		mTCPEdit = (EditText)findViewById(R.id.tcp_port_edit);
		mUDPEdit = (EditText)findViewById(R.id.udp_port_edit);
		
		mRemoveBtn = (ImageButton)findViewById(R.id.new_first_remove);
		mConnectBtn = (ImageButton)findViewById(R.id.new_first_connect);
		mConnectBtn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				String name = mAddressEdit.getText().toString();
	              int port;
	              try {
	                port = Integer.parseInt(mTCPEdit.getText().toString());
	              } catch (Exception e) {
	                port = ServerInfo.DEFAULT_PORT;
	              }
	              int udpPort;
	              try {
	                udpPort = Integer.parseInt(mUDPEdit.getText().toString());
	              } catch (Exception e) {
	                udpPort = ServerInfo.DEFAULT_UDP_PORT;
	              }
	              

	              ServerInfo server = new ServerInfo(name, port, udpPort);
	              SaveServerPrefererences(server, true);
	              Remote.getInstance().setServer(server);
	              Toast.makeText(IpManually.this, getString(R.string.set_server_to) + server,
	                  Toast.LENGTH_LONG).show();
	              StartController();
				
			}
		});
	}
	
	private void SaveServerPrefererences(ServerInfo server, boolean isManualIp) {
		SharedPreferences.Editor editor = getSharedPreferences(
				GmoteClient.PREFS, MODE_WORLD_WRITEABLE).edit();
		editor.putString(GmoteClient.KEY_SERVER, server.getIp());
		editor.putInt(GmoteClient.KEY_PORT, server.getPort());
		editor.putInt(GmoteClient.KEY_UDP_PORT, server.getUdpPort());
		editor.putBoolean(GmoteClient.KEY_IS_MANUAL_IP, isManualIp);
		editor.commit();
	}
	
	private void StartController() {
		Intent intent = new Intent();
		// intent.setClass(IpManually.this, HomeActivity.class);
		intent.setClass(IpManually.this, MainActivity.class);
		startActivity(intent);
		finish();
	}
	
	private void WriteFile(String content) {
		try {
			FileOutputStream fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
			fos.write(content.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String ReadFile() {
		try {
			FileInputStream fis = openFileInput(FILE_NAME);
			byte[] bytes = new byte[fis.available()];
			fis.read(bytes);
			return new String(bytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	class IPAdapter extends BaseAdapter {
		private Context mContext = null;
		private LayoutInflater mInflater;
		private ArrayList<String> colorStatus;

		public IPAdapter(Context context) {
			mContext = context;
			mInflater = LayoutInflater.from(mContext);
			colorStatus = new ArrayList<String>();
			for (int i = 0; i < mIPList.size(); i++) {
				colorStatus.add("#FFFFFF");
			}
			colorStatus.set(0,"#C0FF3E");	
		}


		public int getCount() {
			return mIPList.size();
		}

		
		public Object getItem(int position) {
			return mIPList.get(position);
		}

	
		public long getItemId(int position) {
			return position;
		}

	
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = null;
			final TextView indexChannel;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.ip_item, null);
			}
			indexChannel = (TextView)convertView.findViewById(R.id.serviceip);
			indexChannel.setText(mIPList.get(position).toString());
			indexChannel.setHeight(40);
			indexChannel.setId(position);
			indexChannel.setBackgroundColor(Color.parseColor(colorStatus.get(position)));

			return convertView;
		}

		public void dataLoaded() {
			notifyDataSetChanged();
		}
	}
}