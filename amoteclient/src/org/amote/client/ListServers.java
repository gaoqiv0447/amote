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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gmote.common.ServerInfo;
import org.gmote.common.packet.ListReplyPacket;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The ListServers activity. Finds available servers using a multicast call and
 * lists the results.
 * @author Aisino
 */
public class ListServers extends Activity {
	private static final String DEBUG_TAG = "ListServers";
	private static final int DIALOG_SERVER_NOT_FOUND = 1;
	private static final int DIALOG_ENTER_IP = 2;
	private static final int DIALOG_NO_WIFI = 3;
	private static final int DIALOG_NONE = -1;
	protected static final String SKIP_FIND_SERVERS = "skip_find_servers";
	private static boolean isFirstEntry = true;
	private static int DEFAULT_PASSWORD = 1234;
	private static int COLOR_ORANGE = Color.argb(0xff, 0xff, 0xa5, 0);

	private ListView mListView = null;
	private EditText mAddressEdit = null;
	private EditText mTCPEdit = null;
	private EditText mUDPEdit = null;
	private ImageButton mRemoveBtn = null;
	private ImageButton mConnectBtn = null;
	private View ov = null;

	int currentDialog = DIALOG_NONE;
	ListReplyPacket reply;
	private Remote mRemote;
	String mPath = null;
	List<String> saveIpList = new ArrayList<String>();
	int clickPos = -1;
	/** For show the listView */
	SimpleAdapter simpleAdapter = null;
	List<Map<String, Object>> listItems;
	ProgressDialog mDialog = null;
	View mManualEntryView;
	ActivityUtil mUtil = null;
	ConnectHandler connectHandler;
	private int[] imageIds = new int[]
			{ R.drawable.scan , R.drawable.history};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_form);
		connectHandler = new ConnectHandler();
		mUtil = new ActivityUtil();
		mUtil.onCreate(savedInstanceState, this);
		InitView();
	}

	/** Add Server info to be show.*/
	private void addItemToAdapter(final ServerInfo sInfo, final int isManual) {
		final HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("serverinfo", sInfo);
		map.put("isManual", imageIds[isManual % 2]);
		listItems.add(map);
		simpleAdapter.notifyDataSetChanged();
	}

	/**
	 * Delete Server info to be show.
	 * @param sInfo
	 */
	private void deleteItemFromAdapter(final ServerInfo sInfo) {
		final int size = listItems.size();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("serverinfo", sInfo);
		map.put("isManual", imageIds[1]);
		if (size > 0) {
			final int index = listItems.indexOf(map);
			if (-1 != index) {
				Log.i(DEBUG_TAG, "delete listItems of:" + index);
				listItems.remove(index);
				simpleAdapter.notifyDataSetChanged();
			} else {
				Log.i(DEBUG_TAG, "no index: " + index);
			}
		}
	}

	/** Whether Server info in Adapter.*/
	private boolean containItemInAdapter(ServerInfo sInfo) {
		int size = listItems.size();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("serverinfo", sInfo);
		map.put("isManual", imageIds[0]);
		int location = 0;
			int index = listItems.indexOf(map);
			if (-1 != index) {
				return true;
			} else {
				return false;
			}
	}

	private void InitView() {
		String ip_content = ReadFile();

		listItems = new ArrayList<Map<String, Object>>();
		simpleAdapter = new SimpleAdapter(this, listItems, R.layout.ip_item,
				new String[] { "isManual", "serverinfo" }, new int[] {
						R.id.icon, R.id.serviceip });
		if (ip_content != null) {
			String[] mIPContentList = ip_content.split("-");
			for (int i = 0; i < mIPContentList.length; i++) {
				String[] mTcpContentList = mIPContentList[i].split(":");
				if (mTcpContentList.length == 2) {
					addItemToAdapter(new ServerInfo(mTcpContentList[0], Integer
							.parseInt(mTcpContentList[1]),
							ServerInfo.DEFAULT_UDP_PORT, -1), 1);
				} else {
					addItemToAdapter(new ServerInfo(mTcpContentList[0], Integer
							.parseInt(mTcpContentList[1]),
							ServerInfo.DEFAULT_UDP_PORT, Integer
									.parseInt(mTcpContentList[2])), 1);
				}
				saveIpList.add(mIPContentList[i]);
			}
		}
		mListView = (ListView) findViewById(R.id.first_serverlist_view);
		mListView.setAdapter(simpleAdapter);
		mListView.setTextFilterEnabled(true);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View v, int arg2,
					long arg3) {
				// make the the selected list item background color "orange",
				if(ov == null)	{
					v.setBackgroundColor(COLOR_ORANGE);
					// remember the last view of highlight
					ov = v;
				} else {
					v.setBackgroundColor(COLOR_ORANGE);
					ov.setBackgroundColor(Color.WHITE);
					ov = v;
				}

				final ServerInfo si = (ServerInfo) listItems.get(arg2).get("serverinfo");
				mAddressEdit.setText(si.getIp());
				final Integer tcp = (Integer) si.getPort();
				mTCPEdit.setText(tcp.toString());
				clickPos = arg2;
			}
		});

		mAddressEdit = (EditText) findViewById(R.id.server_edit);
		mTCPEdit = (EditText) findViewById(R.id.tcp_port_edit);
		mTCPEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
		mUDPEdit = (EditText) findViewById(R.id.udp_port_edit);
		mUDPEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

		mRemoveBtn = (ImageButton) findViewById(R.id.new_first_remove);
		mRemoveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				Log.i(DEBUG_TAG, "remove 1 clicked" + clickPos + ":"
						+ saveIpList.size());
				if (clickPos >= 0 && clickPos < saveIpList.size()) {
					deleteItemFromAdapter((ServerInfo) listItems.get(clickPos).get("serverinfo"));
//					deleteItemFromAdapter(new ServerInfo(mAddressEdit.getText()
//							.toString(), Integer.parseInt(mTCPEdit.getText()
//							.toString()), ServerInfo.DEFAULT_UDP_PORT,
//							((ServerInfo) listItems.get(clickPos).get("serverinfo")).getPasswd()));
					saveIpList.remove(clickPos);
					SaveIPList2File();
					mAddressEdit.setText("");
					mTCPEdit.setText("8851");
				} else {
					Toast.makeText(ListServers.this,
							getString(R.string.couldnot_delete_scan_server),
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		mConnectBtn = (ImageButton) findViewById(R.id.new_first_connect);
		mConnectBtn.setFocusable(true);
		mConnectBtn.setFocusableInTouchMode(true);

		mConnectBtn.setFocusable(true);
		mConnectBtn.setFocusableInTouchMode(true);
		mConnectBtn.requestFocus();
		mConnectBtn.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				Log.i(DEBUG_TAG,
						"mConnectBtn onClick");
				// 显示连接缓冲...
				mDialog = ProgressDialog.show(ListServers.this, null,
						"Connecting...");
				final String name = mAddressEdit.getText().toString();
				int port;
				try {
					port = Integer.parseInt(mTCPEdit.getText().toString());
				} catch (Exception e) {
					port = ServerInfo.DEFAULT_PORT;
				}
				int udpPort;
				try {
					udpPort = Integer.parseInt(mUDPEdit.getText().toString());
				} catch (final Exception e) {
					udpPort = ServerInfo.DEFAULT_UDP_PORT;
				}

				ServerInfo server = null;
				if (clickPos >= 0 && clickPos < saveIpList.size()) {
					server = new ServerInfo(name, port, udpPort, ((ServerInfo) listItems.get(clickPos).get("serverinfo")).getPasswd());
					mRemote.setPassword(String.valueOf(((ServerInfo) listItems.get(clickPos).get("serverinfo")).getPasswd()));
					Log.i(DEBUG_TAG,
							"======setPassword  @   ListServer mConnectBtn-if()");
				} else {
					server = new ServerInfo(name, port, udpPort,
							DEFAULT_PASSWORD);
					mRemote.setPassword("1234");
				}

				saveServerPrefererences(server, true);
				Remote.getInstance().setServer(server);
				
				startController(server);
			}
		});
	}

	private String ReadFile() {
		try {
			final FileInputStream fis = openFileInput(Remote.FILE_NAME);
			final byte[] bytes = new byte[fis.available()];
			fis.read(bytes);
			fis.close();

			if(bytes.length == 0){
			    return null;
			}

			return new String(bytes);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void WriteFile(final String content) {
		try {
			final FileOutputStream fos = openFileOutput(Remote.FILE_NAME, MODE_PRIVATE);
			fos.write(content.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	private void SaveIPList2File(){
		final StringBuilder save_ip = new StringBuilder();
		for (int i = 0; i < saveIpList.size(); i++) {
			save_ip.append(saveIpList.get(i));
			if (i != saveIpList.size() - 1) {
				save_ip.append("-");
			}
		}
		Log.i(DEBUG_TAG, "***********" + save_ip.toString());
		WriteFile(save_ip.toString());
	}

	@Override
	public void onStart() {
		super.onStart();
		mRemote = Remote.getInstance(mHandler);
		final Intent intent = getIntent();
		final boolean skipFindServers = intent.getBooleanExtra(
				SKIP_FIND_SERVERS, false);
		if (!skipFindServers) {
			listServers();
		} else {
			showDialog(DIALOG_SERVER_NOT_FOUND);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mDialog != null) {
			mDialog.dismiss();
		}
		mRemote.detach();
	}

	@Override
	public void onResume() {
		super.onResume();
		mRemote = Remote.getInstance(mHandler);
	}

	private void saveServerPrefererences(ServerInfo server, boolean isManualIp) {
		final SharedPreferences.Editor editor = getSharedPreferences(
				GmoteClient.PREFS, MODE_WORLD_WRITEABLE).edit();
		editor.putString(GmoteClient.KEY_SERVER, server.getIp());
		editor.putInt(GmoteClient.KEY_PORT, server.getPort());
		editor.putInt(GmoteClient.KEY_UDP_PORT, server.getUdpPort());
		editor.putString(GmoteClient.KEY_PASSWORD, "" + server.getPasswd());
		editor.putBoolean(GmoteClient.KEY_IS_MANUAL_IP, isManualIp);
		editor.commit();
	}

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (mDialog != null) {
				mDialog.dismiss();
				mDialog = null;
			}

			if (msg.what == Remote.SERVER_LIST_ADD_SERVER) {
				final ServerInfo server = (ServerInfo) msg.obj;
				final ServerInfo newserver = new ServerInfo(server.getIp()
						, server.getPort(), server.getUdpPort());

				if (!containItemInAdapter(newserver)) {
					System.out.println("----server:add server!");
					// Auto scan server
					addItemToAdapter(newserver, 0);
				}
			} else if (msg.what == Remote.SERVER_LIST_DONE) {
				if (simpleAdapter.isEmpty()) {
					showDialog(DIALOG_SERVER_NOT_FOUND);
				}
			}
		}
	};

	void listServers() {
		final WifiManager wifiManager = (WifiManager) this
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			fetchServerList();
		} else {
			showDialog(DIALOG_NO_WIFI);
		}
	}

	void fetchServerList() {
		// arrayAdapter.clear();
		mDialog = ProgressDialog.show(ListServers.this, null,
				getString(R.string.search_server));
		mRemote.getServerList(mHandler);
	}

	void startController(final ServerInfo server) {
		// 运行连接线程
		new Thread(new Runnable(){
			@Override
			public void run() {
				String strConnInfo = server.getIp() + ":" + server.getPort() + ":" + server.getPasswd();
				if (!mRemote.connect(false)) {
					Log.i(DEBUG_TAG, "connect failed!");
					Message msg = new Message();
					msg.what = 0x12;
					Bundle b = new Bundle();
					b.putString("info", strConnInfo);
					msg.setData(b);
					connectHandler.sendMessage(msg);
				} else {
					Log.i(DEBUG_TAG, "connect success!");
					Message msg = new Message();
					msg.what = 0x13;
					Bundle b = new Bundle();
					b.putString("info", strConnInfo);
					msg.setData(b);
					connectHandler.sendMessage(msg);
				}	
				// 取消掉缓冲dialog
			}
		}).start();
	}

	@Override
	protected Dialog onCreateDialog(final int id) { // TODO(mimi): string
													// constants
		currentDialog = id;
		switch (id) {
		case DIALOG_NO_WIFI:
			System.out.println("create no_wifi dialog");
			return new AlertDialog.Builder(ListServers.this)
					.setTitle(getString(R.string.alert))
					.setMessage(getString(R.string.connect_to_wifi_msg))
					.setPositiveButton(getString(R.string.ok_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									currentDialog = DIALOG_NONE;
									startActivity(new Intent(
											WifiManager.ACTION_PICK_WIFI_NETWORK));
								}
							})
					.setNegativeButton(getString(R.string.cancel_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									currentDialog = DIALOG_NONE;
									// showDialog(DIALOG_ENTER_IP);
								}
							}).create();
		case DIALOG_SERVER_NOT_FOUND:
			return new AlertDialog.Builder(ListServers.this)
					.setTitle(getString(R.string.setup))
					.setMessage(getString(R.string.install_server_msg))
					.setPositiveButton(getString(R.string.ok_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									currentDialog = DIALOG_NONE;
									// listServers();
								}
							})
					.setNegativeButton(getString(R.string.cancel_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									currentDialog = DIALOG_NONE;

								}
							}).create();
		case DIALOG_ENTER_IP:
			LayoutInflater factory = LayoutInflater.from(this);
			mManualEntryView = factory.inflate(R.layout.server_form, null);
			ScrollView scrollView = new ScrollView(this);
			scrollView.addView(mManualEntryView);
			EditText serverEdit = (EditText) mManualEntryView
					.findViewById(R.id.server_edit);
			serverEdit.setText(mRemote.getServerIp());
			AlertDialog serverDialog = new AlertDialog.Builder(ListServers.this)
					.setView(scrollView)
					.setTitle(getString(R.string.enter_ip_manually))
					.setPositiveButton(getString(R.string.done_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									currentDialog = DIALOG_NONE;
									EditText serverEdit = (EditText) mManualEntryView
											.findViewById(R.id.server_edit);
									EditText portEdit = (EditText) mManualEntryView
											.findViewById(R.id.port_edit);
									EditText udpPortEdit = (EditText) mManualEntryView
											.findViewById(R.id.udp_port_edit);
									String name = serverEdit.getText()
											.toString();
									int port;
									try {
										port = Integer.parseInt(portEdit
												.getText().toString());
									} catch (Exception e) {
										port = ServerInfo.DEFAULT_PORT;
									}
									int udpPort;
									try {
										udpPort = Integer.parseInt(udpPortEdit
												.getText().toString());
									} catch (Exception e) {
										udpPort = ServerInfo.DEFAULT_UDP_PORT;
									}

									final ServerInfo server = new ServerInfo(name,
											port, udpPort);
									saveServerPrefererences(server, true);
									Remote.getInstance().setServer(server);
									startController(server);
								}
							})
					.setNegativeButton(getString(R.string.cancel_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									currentDialog = DIALOG_NONE;
								}
							}).create();
			return serverDialog;
		default:
			break;
		}
		return null;
	}
	
	class ConnectHandler extends Handler {
		public ConnectHandler(){
			
		}
		
		@Override
		public void handleMessage(Message msg) {
			Log.d("MyHandler", "handleMessage......");
			super.handleMessage(msg);
			if (msg.what == 0x12){
				// 连接成功
				mUtil.cancelDialog();
				Toast.makeText(ListServers.this,
						getString(R.string.connect_server_failure),
						Toast.LENGTH_SHORT).show();
			} else if(msg.what == 0x13){
				// 连接失败
				mUtil.cancelDialog();
				Toast.makeText(ListServers.this,
						getString(R.string.connect_server_success),
						Toast.LENGTH_SHORT).show();
				String strConnInfo = msg.getData().getString("info");
				if (!saveIpList.contains(strConnInfo)) {
					saveIpList.add(strConnInfo);
					SaveIPList2File();
				}
				if (isFirstEntry) {
					isFirstEntry = false;
					final Intent intent = new Intent();
					intent.setClass(ListServers.this, MainActivity.class);
					startActivity(intent);
				}
				finish();
			}
			
		}
	}

}
