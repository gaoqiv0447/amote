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
import org.gmote.common.ServerInfo;
import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.MouseEvent;
import org.gmote.common.Protocol.ServerErrorType;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.MouseClickPacket;
import org.gmote.common.packet.ServerErrorPacket;
import org.gmote.common.packet.SimplePacket;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Base class that provides the common functionalities of all activities in the
 * Amote client.
 *
 * @author Mimi
 *
 */
public class ActivityUtil {
	static final String DEBUG_TAG = "ActivityUtil";
	static final int DIALOG_ENTER_PASSWORD = 1;
	static final int VIEW_VISIBLE = 0;
	static final int VIEW_GONE = 2;
	static final int DIALOG_UPDATE_CLIENT = 3;
	static final int DIALOG_BROWSE_VIEW_SETTINGS = 4;
	static final int DIALOG_HELP = 5;
	static final int DIALOG_SERVER_OUT_OF_DATE = 6;
	static final int DIALOG_SERVER_OUT_OF_DATE_NO_UPDATER = 7;
	static final int DIALOG_APP_EXIT = 8;

	Activity mActivity = null;
	View mPasswordEntryView = null;
	ProgressDialog mDialog = null;
	Menu mMenu = null;
	Remote mRemote;
	AbstractPacket mPacket = null;
	static WifiLock wifiLock = null;
	static WakeLock wakeLock = null;

	/** Called when the activity is first created. */
	public void onCreate(Bundle icicle, Activity activity) {
		mActivity = activity;
		mRemote = Remote.getInstance(mHandler);
		if (mRemote.getServer() == null) {
			// This can happen if there is an uncaught exception in the program.
			// Our
			// variables will get reset, but the GmoteClient activity won't get
			// launched (it only goes back to the previous activity that is on
			// the
			// stack).
			SharedPreferences prefs = mActivity.getSharedPreferences(
					GmoteClient.PREFS, Context.MODE_WORLD_READABLE);
			String serverAddress = prefs
					.getString(GmoteClient.KEY_SERVER, null);
			if (serverAddress != null && serverAddress.length() != 0) {
				GmoteClient.setServerIpAndPassword(prefs, serverAddress);
			}
		}
	}

	public void onStart(Activity activity) {
		mActivity = activity;
		if (wifiLock == null) {
			PowerManager powerManager = (PowerManager) mActivity
					.getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					DEBUG_TAG);
			wakeLock.setReferenceCounted(true);

			WifiManager wifiManager = (WifiManager) mActivity
					.getSystemService(Context.WIFI_SERVICE);
			wifiLock = wifiManager.createWifiLock(DEBUG_TAG);
			wifiLock.setReferenceCounted(true);
		}
		Log.e(mActivity.getClass().getName(), "ACQUIRE");

		wifiLock.acquire();
		wakeLock.acquire();
	}

	public void onStop() {
		if (wifiLock != null) {
			Log.e(mActivity.getClass().getName(), "RELEASE");
			wifiLock.release();
			wakeLock.release();
		}
	}

	public void onResume() {
		mRemote = Remote.getInstance(mHandler);
	}

	public void onPause() {
		cancelDialog();
	}

	public boolean startActivity(View v) {
		switch (v.getId()) {
			case R.id.main_remote:
				startActivityByClass(HomeActivity.class);
				break;
			case R.id.main_touchpad:
				startActivityByClass(Touchpad.class);
				break;
			case R.id.main_game:
				startActivityByClass(ScreenShowActivity.class);
				break;
			case R.id.main_findserver:
				startActivityByClass(ListServers.class);
				break;
			case R.id.main_tv_file:
				startActivityByClass(Browse.class);
				break;
			case R.id.main_web:
				startActivityByClass(WebBrowser.class);
				break;
			case R.id.main_help:
				mActivity.showDialog(DIALOG_HELP);
				break;
			default:
				Log.i(DEBUG_TAG, "unknown activity to start!");
				break;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	void startActivityByClass(Class c) {
		Intent intent = new Intent();
		intent.setClass(mActivity, c);
		mActivity.startActivity(intent);
	}

	void startActivityByClassName(String name) {
		Intent intent = new Intent();
		try {
			intent.setClass(mActivity, Class.forName(name));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		mActivity.startActivity(intent);
		mActivity.finish();  // add by zhangdawei
	}


	/**
	 * Extracts packet from a message and handles messages common to most
	 * activities.
	 *
	 * @param msg
	 * @return AbstractPacket or null
	 */
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == Remote.CONNECTING) {
				if (mDialog == null) {
					System.out.println("mDialog");
					mDialog = ProgressDialog.show(mActivity, null,
					        mActivity.getString(R.string.connecting_to_the_server));
				}
				return;
			}

			if(msg.what == Remote.LONG_PRESS_UP_CONFIRM){
			    send(new MouseClickPacket(MouseEvent.LEFT_MOUSE_UP));
			    mHandler.sendEmptyMessageDelayed(Remote.LONG_PRESS_UP_CONFIRM, 500);
			    return;
			}

			if (msg.what == Remote.IP_LIST_SAVE) {
				ServerInfo server = (ServerInfo) msg.obj;
				if (server != null) {
					StringBuilder save_ip = new StringBuilder();
					save_ip.append(server.getIp());
					save_ip.append(":");
					save_ip.append(server.getPort());
					save_ip.append(":");
					save_ip.append(Remote.getInstance().password);

					String ip_content = ReadFile();
					if(ip_content == null)
					{
						WriteFile(save_ip.toString(),false);
					}else{
						if(!ip_content.contains(save_ip.toString()))
							WriteFile(save_ip.toString(),true);
					}
				}
			}else if (msg.what == Remote.AUTHENTICATION_FAILURE) {
				cancelDialog();
				mActivity.showDialog(DIALOG_ENTER_PASSWORD);
			} else if (msg.what == Remote.CONNECTED) {
			    SimplePacket sp = (SimplePacket) msg.obj;
				cancelDialog();
			} else if (msg.what == Remote.CONNECTION_FAILURE || msg.obj == null) {
				cancelDialog();

				Toast.makeText(mActivity, mActivity.getString(R.string.connection_problem),
						Toast.LENGTH_SHORT).show();

				Intent intent = new Intent();
				intent.setClass(mActivity, ListServers.class);

				boolean skipFindServers = GmoteClient.isManualIp(mActivity
						.getSharedPreferences(GmoteClient.PREFS,
								Context.MODE_WORLD_READABLE));
				//intent.putExtra(ListServers.SKIP_FIND_SERVERS, skipFindServers);
				intent.putExtra(ListServers.SKIP_FIND_SERVERS, false);
				mActivity.startActivity(intent);
				//mActivity.finish(); // modify by zhangdawei
			} else if (msg.what == Remote.SERVER_OUT_OF_DATE) {
				cancelDialog();
				String serverVersion = (String) msg.obj;
				if (serverVersion.equalsIgnoreCase("1.2")) {
					// Version 1.2 of the server did not have an updater.
					mActivity.showDialog(DIALOG_SERVER_OUT_OF_DATE_NO_UPDATER);
				} else {
					mActivity.showDialog(DIALOG_SERVER_OUT_OF_DATE);
				}

			} else {

				AbstractPacket reply = (AbstractPacket) msg.obj;
				if (reply.getCommand() != Command.MEDIA_INFO) {
					cancelDialog();
				}
				if(reply.getCommand() == Command.MOUSE_CLICK_REQ){
				   // mHandler.removeMessages(Remote.LONG_PRESS_UP_CONFIRM);
				}
				if (reply.getCommand() == Command.SERVER_ERROR) {
					ServerErrorPacket errorPacket = (ServerErrorPacket) reply;
					int errorTypeOrdinal = errorPacket.getErrorTypeOrdinal();
					ServerErrorType errorType;
					// Determine which kind of error this is, making sure to
					// handle new types of errors properly.
					if (errorTypeOrdinal < ServerErrorType.values().length) {
						errorType = ServerErrorType.values()[errorTypeOrdinal];
					} else {
						errorType = ServerErrorType.UNSPECIFIED_ERROR;
					}

					if (errorType == ServerErrorType.INCOMPATIBLE_CLIENT) {
						mRemote.detach();
						mActivity.showDialog(DIALOG_UPDATE_CLIENT);
					} else {
						Toast.makeText(mActivity,
								errorPacket.getErrorDescription(),
								Toast.LENGTH_LONG).show();
					}

				} else if(reply.getCommand() == Command.CLIENT_EXIT_FORCE){
				    Toast.makeText(mActivity,
                            mActivity.getString(R.string.amoteServer_exit),
                            Toast.LENGTH_LONG).show();
				}else if(reply.getCommand() == Command.MOUSE_UP_REQ){
				  //  mHandler.removeMessages(Remote.LONG_PRESS_UP_CONFIRM);
				}
				else {
					((BaseActivity) mActivity).handleReceivedPacket(reply);
				}
			}
		}
	};

	private String ReadFile() {
		try {
			FileInputStream fis = mActivity.openFileInput(Remote.FILE_NAME);
			byte[] bytes = new byte[fis.available()];
			fis.read(bytes);
			fis.close();
			return new String(bytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void WriteFile(String content,Boolean isAppend) {
		try {
			if(isAppend)
			{
				FileOutputStream fos = mActivity.openFileOutput(Remote.FILE_NAME,
						Context.MODE_APPEND);
				fos.write('-');
				fos.write(content.getBytes());
				fos.close();
			}else{
				FileOutputStream fos = mActivity.openFileOutput(Remote.FILE_NAME,
						Context.MODE_PRIVATE);
				fos.write(content.getBytes());
				fos.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	void send(AbstractPacket packet) {
		mPacket = packet;
		mRemote.queuePacket(packet);
	}

	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_ENTER_PASSWORD) {

			LayoutInflater factory = LayoutInflater.from(mActivity);
			mPasswordEntryView = factory.inflate(R.layout.password_form, null);
			return new AlertDialog.Builder(mActivity)
					.setTitle(mActivity.getString(R.string.enter_password))
					.setView(mPasswordEntryView)
					.setPositiveButton(mActivity.getString(R.string.ok_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									EditText passwordEdit = (EditText) mPasswordEntryView
											.findViewById(R.id.password_edit);
									String password = passwordEdit.getText()
											.toString();
									Remote.getInstance().setPassword(password);

									if (mPacket != null) {
										mRemote.queuePacket(mPacket);
									}

									SharedPreferences.Editor editor = mActivity
											.getSharedPreferences(
													GmoteClient.PREFS,
													Activity.MODE_WORLD_WRITEABLE)
											.edit();
									editor.putString(GmoteClient.KEY_PASSWORD,
											password);
									editor.commit();
								}
							})
					.setNegativeButton(mActivity.getString(R.string.cancel_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									mRemote.flag = true;
								}
							}).create();
		} else if (id == DIALOG_UPDATE_CLIENT) {

			LayoutInflater factory = LayoutInflater.from(mActivity);
			final View updateClientView = factory.inflate(
					R.layout.update_client, null);
			return new AlertDialog.Builder(mActivity)
					.setTitle(mActivity.getString(R.string.client_update_required))
					.setView(updateClientView)
					.setPositiveButton(mActivity.getString(R.string.ok_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Intent intent = new Intent(
											Intent.ACTION_VIEW,
											Uri.parse("market://search?q=pname:org.gmote.client.android"));
									mActivity.startActivity(intent);
									mActivity.finish();
								}
							})
					.setNegativeButton(mActivity.getString(R.string.cancel_btn),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									mActivity.finish();
								}
							}).create();
		} else if (id == DIALOG_HELP) {
			return createHelpDialog();
		} else if (id == DIALOG_SERVER_OUT_OF_DATE) {
			return createServerOutOfDateDialog();
		} else if (id == DIALOG_SERVER_OUT_OF_DATE_NO_UPDATER) {
			return createServerOutOfDateNoUpdaterDialog();
		} else if (id == DIALOG_APP_EXIT) {
		    return new AlertDialog.Builder(mActivity)
            .setTitle(R.string.exit_btn)
            .setMessage(R.string.exit_warning)
            .setPositiveButton(R.string.exit_btn,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            mActivity.finish();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
            .setNegativeButton(R.string.cancel_btn,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                        }
                    }).create();
		}

		return null;
	}

	private Dialog createServerOutOfDateNoUpdaterDialog() {
		//String dialogMessage = "The Gmote server is out of date and is incompatible with the current version of Gmote that is on your phone.\n\nPlease exit the Gmote Server and install the latest server software from http://www.gmote.org/server on your computer.\n";
		String dialogMessage = mActivity.getString(R.string.update_msg_noupdater);
	    final View alertDialogView = createLinkifiedAlertView(dialogMessage);

		return new AlertDialog.Builder(mActivity).setView(alertDialogView)
				.setTitle(mActivity.getString(R.string.update_title))
				.setPositiveButton(mActivity.getString(R.string.ok_btn), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Remote.getInstance().disconnect();
						dialog.dismiss();
					}
				}).create();
	}

	private Dialog createServerOutOfDateDialog() {
		//String dialogMessage = "The Gmote server is out of date and is incompatible with the current version of Gmote that is on your phone.\n\nClick Ok to update your server. Clicking cancel will terminate your connection to the server.\n";
		String dialogMessage = mActivity.getString(R.string.update_msg_outofdate);
	    final View alertDialogView = createLinkifiedAlertView(dialogMessage);

		return new AlertDialog.Builder(mActivity)
				.setView(alertDialogView)
				.setTitle(mActivity.getString(R.string.update_title))
				.setPositiveButton(mActivity.getString(R.string.update_title),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								send(new SimplePacket(
										Command.UPDATE_SERVER_REQUEST));
								try {
									Thread.sleep(300);
								} catch (InterruptedException e) {
									Log.e(DEBUG_TAG, e.getMessage(), e);
								}
								Remote.getInstance().disconnect();
								dialog.dismiss();
								mActivity.finish();
							}
						})
				.setNegativeButton(mActivity.getString(R.string.cancel_btn),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								Remote.getInstance().disconnect();
								dialog.dismiss();
								mActivity.finish();
							}
						}).create();
	}

	private Dialog createHelpDialog() {

		//String dialogMessage = "If you are unable to connect to the Gmote server or are experiencing other technical issues, please visit:\nhttp://www.gmote.org/faq\n\nTo lean how to use Gmote, please visit:\nhttp://www.gmote.org/howto\n\nTo learn about GmoteTouch, please visit:\nhttp://www.gmote.org/gmotetouch\n\nTip: To skip to the next song in a playlist, hold down the right arrow button for 2 seconds.\n\nTip: Try using android's 'back' arrow key to return to your media list instead of browsing to it again.\n\nTip: To right click in GmoteTouch mode, press down on the screen for 2 seconds.\n\nTip: Press the 'play' button while in PowerPoint, PDF or ImageViewer mode to launch a slide show. Press the close button (top right circle button) to close the slideshow.\n\n";
		String dialogMessage = mActivity.getResources().getString(R.string.help_explanation);
		final View alertDialogView = createLinkifiedAlertView(dialogMessage);
Log.i(DEBUG_TAG, "create Help dialog");
		return new AlertDialog.Builder(mActivity).setView(alertDialogView)
				.setTitle(mActivity.getResources().getString(R.string.help_title))
				.setPositiveButton(mActivity.getString(R.string.ok_btn), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				}).create();
	}

	private View createLinkifiedAlertView(String dialogMessage) {
		LayoutInflater factory = LayoutInflater.from(mActivity);
		final View alertDialogView = factory.inflate(
				R.layout.alert_dialog_text_view, null);
		TextView tv = (TextView) alertDialogView
				.findViewById(R.id.alert_text_view);
		tv.setText(dialogMessage);
		return alertDialogView;
	}

	public static AlertDialog showMessageBox(Context context, String title,
			String message) {
		return new AlertDialog.Builder(context).setTitle(title)
				.setMessage(message)
				.setPositiveButton(R.string.ok_btn, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				}).show();
	}

	void showProgressDialog(String text) {
		mDialog = ProgressDialog.show(mActivity, null, text);
	}

	void cancelDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}
}
