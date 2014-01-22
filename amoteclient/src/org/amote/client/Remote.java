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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.amote.client.android.ScreenInfo;
import org.amote.utils.SensorHub;
import org.gmote.common.DataReceiverIF;
import org.gmote.common.ISuccessResponse;
import org.gmote.common.MulticastClient;
import org.gmote.common.MulticastClient.ServerFoundHandler;
import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.CommandEvent;
import org.gmote.common.ServerInfo;
import org.gmote.common.ServerOutOfDateException;
import org.gmote.common.TcpConnection;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.CommandPacket;
import org.gmote.common.packet.SensorStatePacket;
import org.gmote.common.packet.SimplePacket;
import org.gmote.common.security.AuthenticationException;
import org.gmote.common.security.AuthenticationHandler;

import android.content.Intent;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Wrapper class responsible for finding and communicating with the server.
 *
 * @author Aisino
 *
 */
public class Remote implements DataReceiverIF {
	// Current version of the Gmote Client. We don't use the value that is in
	// the
	// manifest since its possible that we don't have access to this value (for
	// example, when the program crashes and gets restarted by android)
	public static final String GMOTE_CLIENT_VERSION = "2.0.2";
	public static final String MINIMUM_SERVER_VERSION = "2.0.0";

	// Response codes
	public static final int NORMAL = 0;
	public static final int CONNECTION_FAILURE = 1;
	public static final int CONNECTING = 2;
	public static final int CONNECTED = 6;
	public static final int SEARCHING = 3;
	public static final int AUTHENTICATION_FAILURE = 4;
	public static final int SERVER_LIST_ADD_SERVER = 5;
	public static final int SERVER_LIST_DONE = 6;
	public static final int SERVER_OUT_OF_DATE = 7;
	public static final int IP_LIST_SAVE = 8;
	public static final int LONG_PRESS_UP_CONFIRM = 9;
	public static final String FILE_NAME = "IP.txt";
	/** heart beat Interval 10 min */
	private static final int HEARTBEATTIME = 10*1000*60;

	// Timing constants
	public static final int MAX_ATTEMPTS = 3; // number of connection attempts
												// before report giving up
	public static final int TIMEOUT = 3000; // milliseconds server connection
											// timeout
	public static final int FINDSERVERS_TIMEOUT = 6500; // milliseconds
	private static final String DEBUG_TAG = "Remote";

	private ServerInfo server = null;
	public String password = "";
	private static Remote remote = new Remote();

	private Handler callback;
	private TcpConnection con = null;
	private Thread worker = null;
	private BlockingQueue<AbstractPacket> packetQueue = new LinkedBlockingQueue<AbstractPacket>(200);
	InetAddress serverInetAddress = null;
	public Boolean flag = true;

	// add the heartbeat function by zhangdawei
	private  Timer mTimer;
	private  TimerTask mTimerTask;

	private Remote() {
		// Start a new thread that will send packets for us.
		worker = new Thread(new PacketSender());
		worker.start();

		// add by zhangdawei, in order to solve the disconnection after several minutes.
		mTimer = new Timer(true);
		mTimerTask = new TimerTask() {
	        public void run() {
	            if (remote.isConnected()) {
	                remote.queuePacket(new SimplePacket(Command.BEATHEART));
	                System.out.println("###############BeatHeart Packet queued!");
	            }
	        }
	    };
        mTimer.schedule(mTimerTask, 0, HEARTBEATTIME);
        System.out.println("BeatHeart Timer StarTing!");
        // end add
	}

	private void setCallback(Handler callback) {
		this.callback = callback;
	}

	public static synchronized Remote getInstance(Handler handler) {
		remote.setCallback(handler);
		return remote;
	}

	public static Remote getInstance() {
		return remote;
	}

	public synchronized void setServer(ServerInfo serverInfo) {
		server = serverInfo;
		Log.d(DEBUG_TAG, "Gmote# set server to: " + server.getServer() + ":"
				+ server.getPort());
		try {
			if (serverInfo == null || serverInfo.getIp() == null) {
				serverInetAddress = null;
			} else {
				serverInetAddress = InetAddress.getByName(serverInfo.getIp());
			}
		} catch (final UnknownHostException e) {
			Log.e(DEBUG_TAG, e.getMessage(), e);
			serverInetAddress = null;
		}
		disconnect();
	}

	public InetAddress getServerInetAddress() {
		return serverInetAddress;
	}

	protected synchronized void disconnect() {
		if (con != null) {
			con.closeConnection();
			con = null;
		}
		packetQueue.clear();
	}

	public synchronized void setPassword(String newPassword) {
		password = newPassword;
		Log.d(DEBUG_TAG, "Remote# set password");
	}

	public void detach() {
		callback = null;
	}

	public String getServerString() {
		if (server != null)
			return server.toString();
		return "";
	}

	public String getServerIp() {
		if (server != null)
			return server.getIp();
		return "";
	}

	public int getServerPort() {
		if (server != null) {
			return server.getPort();
		}
		return 8851;
	}

	public int getServerUdpPort() {
		if (server != null) {
			return server.getUdpPort();
		}
		return ServerInfo.DEFAULT_UDP_PORT;
	}

	public synchronized boolean isConnected() {
		return con == null ? false : con.isConnected();
	}

	public synchronized boolean connect(boolean ignoreErrors) {
		if (callback == null) {
			Log.w(DEBUG_TAG, "Callback is null in connect()");
			return false;
		}

		if(flag) {
			flag = false;
			callback.sendEmptyMessage(CONNECTING);
		}

		if (server == null) {
			Log.w(DEBUG_TAG, "Server was null in connect");
			disconnect();
			if (!ignoreErrors) {
				callback.sendEmptyMessage(CONNECTION_FAILURE);
			}
			flag = true;
			return false;
		}

		for (int i = 0; i < MAX_ATTEMPTS && callback != null; i++) {
			try {
				connectToServer();
				if (callback != null) {
					callback.sendEmptyMessage(CONNECTED);
				}
				return true;
			} catch (IOException e) {
				Log.e(DEBUG_TAG,
						"Connection attempt " + i + " failed: "
								+ e.getMessage(), e);
				flag = true;
			} catch (AuthenticationException e) {
				Log.e(DEBUG_TAG, "Authentication failure: " + e.getMessage(), e);
				disconnect();
				if (callback != null) {
					callback.sendEmptyMessage(AUTHENTICATION_FAILURE);
				} else {
					Log.w(DEBUG_TAG,
							"Authentication failure with callback = null. We won't be able to notify anyone");
				}
				return false;
			} catch (ServerOutOfDateException e) {
				Log.e(DEBUG_TAG, "Server out of date error: " + e.getMessage(),
						e);
				flag = true;
				if (callback != null) {
					callback.sendMessage(Message.obtain(callback,
							SERVER_OUT_OF_DATE, e.getServerVersion()));
					return true;
				} else {
					Log.e(DEBUG_TAG,
							"The server is out of date, but no callback was found. This means we won't be able to notify the user of the current error.");
					disconnect();
					return false;
				}
			}
		}

		Log.w(DEBUG_TAG, "Failed to connect after " + MAX_ATTEMPTS
				+ " attempts. Aborting.");
		if (callback != null) {
			if (!ignoreErrors) {
				callback.sendEmptyMessage(CONNECTION_FAILURE);
			}
		} else {
			Log.w(DEBUG_TAG, "Connection failure, and call back is null");
		}
		disconnect();
		flag = true;
		return false;
	}

	private synchronized void connectToServer() throws IOException,
			AuthenticationException, ServerOutOfDateException {

		con = new TcpConnection(new AuthenticationHandler(GMOTE_CLIENT_VERSION,
				MINIMUM_SERVER_VERSION),successResponse);
		Log.i(DEBUG_TAG, "Connecting to server: " + server.getIp() + ":"
				+ server.getPort() + ":" + password);
		con.connectToServerAsync(server.getPort(), server.getIp(),
				(DataReceiverIF) Remote.this, TIMEOUT, password);
	}

	/** push the packet into the packet sending queue. */
	protected synchronized void queuePacket(AbstractPacket packet) {
		try {
			packetQueue.put(packet);
		} catch (InterruptedException e) {
			Log.e(DEBUG_TAG, e.getMessage(), e);
		}
	}

	/** handle the data received from server, by TCP. */
	public void handleReceiveData(final AbstractPacket reply, final TcpConnection connection) {
		if (callback != null) {
		    Log.w(DEBUG_TAG,"==========remote handleRecieveData1");
		    switch (reply.getCommand()) {
		    	case BEATHEART_REPLY:
		    		System.out.println("======heart beat from server Pong! Pong!");
		    		break;
		    	case COMMAND_EVENT:
		    		int sensorType = ((CommandPacket) reply).getType();
					CommandEvent ce = ((CommandPacket) reply).getCommandEvent();
					if (ce == CommandEvent.CMD_ENABLE){
						Log.i(DEBUG_TAG, "get server TCP packet: enable Sensor");
						SensorHub.getInstance().saveSensorState(new Integer(sensorType), true);
					} else if (ce == CommandEvent.CMD_DISABLE){
						Log.i(DEBUG_TAG, "get server TCP packet: disable Sensor");
						SensorHub.getInstance().saveSensorState(new Integer(sensorType), false);
					} else {
						Log.e(DEBUG_TAG, "get server TCP packet: unknown command packet!");
					}
		    		break;
		    	case SENSOR_STATE_EVENT:
		    		SensorHub.getInstance().saveSensorState(((SensorStatePacket) reply).getSensorStateHashMap()) ;
					Log.i(DEBUG_TAG, "get sensor State hash map!");
		    		break;
		    	default:
		    		callback.sendMessage(Message.obtain(callback, -1, reply));
		    		break;
		    }
		} else {
            Log.w(DEBUG_TAG,
                    "Received a packet, but call back is null, " +
                    "so I won't be able to deliver it to anyone.");
        }
	}

	public void getServerList(Handler findServerCallback) {
		Thread serverFinder = new Thread(new ServerFinder(findServerCallback));
		serverFinder.start();
	}

	protected class ServerFinder implements Runnable {

		private Handler findServerCallback;

		public ServerFinder(Handler findServerCallback) {
			this.findServerCallback = findServerCallback;
		}

		public void run() {
			Log.e(DEBUG_TAG, "Creating MC");
			MulticastClient mc = new MulticastClient();

			final ServerFoundHandler serverFoundHandler = new ServerFoundHandler() {

				public void onServerFound(final ServerInfo server) {
					if (findServerCallback != null) {
						findServerCallback.sendMessage(Message.obtain(
								findServerCallback, SERVER_LIST_ADD_SERVER,
								server));
					} else {
						Log.w(DEBUG_TAG,
								"Find Server callback was null. We can't notify anyone that we found a new server.");
					}
				}
			};

			mc.findServers(FINDSERVERS_TIMEOUT, serverFoundHandler);
			Log.e(DEBUG_TAG, "Got Servers");
			if (findServerCallback != null) {
				findServerCallback.sendMessage(Message.obtain(
						findServerCallback, SERVER_LIST_DONE));
			} else {
				Log.w(DEBUG_TAG,
						"Find Server callback was null. We can't notify anyone that find server has finished.");
			}
		}
	}

	/** basic thread class, for send Packet. */
	class PacketSender implements Runnable {

		public void run() {
			AbstractPacket packet;
			while (true) {
				// Get the packet that is at the head of the queue, waiting if
				// necessary.
				try {
					packet = packetQueue.take();
				} catch (final InterruptedException e) {
					Log.w(DEBUG_TAG, e.getMessage(), e);
					packet = null;
				} catch (final Exception e) {
					Log.e(DEBUG_TAG, e.getMessage(), e);
					packet = null;
					createNewQueue();
				}

				if (packet != null) {
					try {
						sendPacketToServer(packet);
					} catch (final Exception e) {
						Log.d(DEBUG_TAG,
								"Send packet failed. " + e.getMessage(), e);
						disconnect();
					}
				}

			}
		}

		/** recreate the queue if problem occured. */
		private synchronized void createNewQueue() {
			packetQueue = new LinkedBlockingQueue<AbstractPacket>();
		}

		/**
		 * send Packet to server.
		 * @param packet
		 * @throws IOException
		 */
		private synchronized void sendPacketToServer(final AbstractPacket packet)
				throws IOException {
			// Try to connect.
			// We'll try this twice since the connection may be down but we
			// don't know about it.
		    Log.e(DEBUG_TAG,"========================client sendPacketToServer");
			boolean tryAgain = false;
			do {
				if (con != null || connect(false)) {
					try {
						con.sendPacket(packet);
						tryAgain = false;
					} catch (final IOException e) {
						Log.e(DEBUG_TAG, e.getMessage(), e);
						disconnect();
						tryAgain = (tryAgain == false);
						if (!tryAgain) {
							if (callback != null) {
								callback.sendEmptyMessage(CONNECTION_FAILURE);
							} else {
								Log.e(DEBUG_TAG, "Unable to notify client of io error in send packet since callback is null");
							}
						}
					}
				} else {
					tryAgain = false;
				}
			} while (tryAgain);
		}
	}

	public String getSessionId() {
		if (con == null) {
			return null;
		}
		return con.getSessionId();
	}

	public ServerInfo getServer() {
		return server;
	}

	ISuccessResponse successResponse = new ISuccessResponse(){
		public void CheckSuccess(AbstractPacket packet) {
			Log.i(DEBUG_TAG, "&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&CheckSuccess running!");
			callback.sendMessage(Message.obtain(callback, IP_LIST_SAVE, server));
			flag = true;
			final SimplePacket sp = (SimplePacket) packet;
			ScreenInfo.handleScreenType(sp.getWidth(), sp.getHeight(), sp.getdensityDpi());
		}
	};
}