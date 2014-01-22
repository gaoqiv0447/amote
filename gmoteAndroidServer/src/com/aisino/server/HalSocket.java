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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.gmote.common.FileInfo;
import org.gmote.common.Protocol.CommandEvent;
import org.gmote.common.Protocol.ServerErrorType;
import org.gmote.common.TcpConnection;
import org.gmote.common.packet.AbstractPacket;
import org.gmote.common.packet.CommandPacket;
import org.gmote.common.packet.ListReplyPacket;
import org.gmote.common.packet.SensorStatePacket;
import org.gmote.common.utils.TypeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import android.net.LocalServerSocket;
//import android.net.LocalSocket;
//import android.net.LocalSocketAddress;
import android.hardware.Sensor;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

/**
 * communicate with HAL.
 * @author gaoqi
 * single instance mode
 * include UDP & TCP, UDP for send Sensor Event got from client to HAL, TCP for send "OK" to
 * process:
 * 1. Hal: create UDP server
 * 2. Java Server(here): create TCP server
 * 3. Java Server(here): before waiting, create UDP client and send "OK" to Hal
 * 4. Hal: get "OK", set bit zone
 * 5. udp socket node:"/data/amote_udp", abstract namespace:"amote_tcp".
 *
 * modified by:
 * weidawei 2012-3-15 1. delete some debug code.
 *                    2. add synchronized to getinstance to avoid construct at the same time.
 *                    3. change the thread stop method.
 */
public final class HalSocket {

	/**
	 * TAG name, for print Log.
	 */
	private static final String TAG = "HalSocket";
	/** abstract namespace for TCP, in order to send "ok" to HAL.*/
	public static final String SOCKET_NAME = "amote_tcp";
	/** for single instance.*/
	private static HalSocket halSocket = null;
	/** store the Hash table, mapping amote's Sensor type(definde in HAL) to Android Sensor type(client used). */
    private  HashMap<Integer, Boolean> SERVERSENSENSORSTATE;


	/** .*/
	private LocalServerSocket mServerSocket;

	private LocalSocket mLocalSocket = null;

	/** udp socket description.*/
	private int udpsocketid;

	/**. */
	private Thread t;

	private Thread temp;
	/** .*/
	private TcpConnection con;

	/** flag used to stop thread.*/
	private boolean mStop = false;

	/** constant value used to sub. */
	private static final int CHAR_ZERO = 48;

	/** HAL accelerometor type. */
	private static final int ASENSORTYPE_ACC 	= 0;
	/** HAL magnetic field type. */
	private static final int ASENSORTYPE_MAG 	= 1;
	/** HAL orientation type. */
	private static final int ASENSORTYPE_ORI 	= 2;
    /** HAL gryoscope type. */
    private static final int ASENSORTYPE_GYRO   = 3;
	/** HAL light type. */
	private static final int ASENSORTYPE_LIGHT 	= 4;
	/** HAL pressure type. */
	private static final int ASENSORTYPE_PRS 	= 5;
	/** HAL temperature type. */
	private static final int ASENSORTYPE_TEM 	= 6;
	   /** HAL proximity type. */
    private static final int ASENSORTYPE_PRO    = 7;

    private OutputStream os = null;
    private OutputStreamWriter osw = null;

	static {
		try {
			System.loadLibrary("native-udp-socket");
		} catch (final UnsatisfiedLinkError ule) {
	        System.err.println("WARNING: Could not load library native-udp-socket!");
	    }
	}

	/**
	 * createUdpSocket. JNI invoke C function.
	 * @return socket description
	 */
	native int createUdpSocket();

	/**
	 * sendUdpPacket: send udp to HAL, JNI invoke C function.
	 * @param socketid socket description
	 * @param udpPacket the packet of udp data
	 * @return the status of send packet
	 */
	native int sendUdpPacket(final int socketid, final byte[] udpPacket);

	/**
	 * Singleton Pattern HalSocket.
	 * @return single instance to be created
	 */
	public static synchronized HalSocket getInstance() {
		if (halSocket == null) {
			halSocket = new HalSocket();
		}
		return halSocket;

	}

	/** private constructor.*/
	private HalSocket() {
		try {
			mServerSocket = new LocalServerSocket(SOCKET_NAME);
			if(mServerSocket == null){
			    throw new IOException();
			}
			// here send UDP command "ok", tell HAL the java server is created, and running
			udpsocketid = createUdpSocket();

			if(udpsocketid < 0){
			    Log.d(TAG, "------->createUdpSocket failed!");
			}else{
		         final byte [] cmdOk = new byte[] {'o', 'k'};
		            sendUdpPacket(udpsocketid, cmdOk);
			}

			SERVERSENSENSORSTATE = new HashMap<Integer, Boolean>(8);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_ACCELEROMETER, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_MAGNETIC_FIELD, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_ORIENTATION, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_GYROSCOPE, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_LIGHT, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_PRESSURE, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_TEMPERATURE, false);
			SERVERSENSENSORSTATE.put(Sensor.TYPE_PROXIMITY, false);
			// end
		} catch (final IOException e) {
			Log.v(TAG, "in onCreate, making server socket: " + e);
		} finally {
			Log.v(TAG, "HalSocket finally ");
		}

		t = new Thread() {

			public void run() {
				try {
							Log.v(TAG, "Waiting for connection...");
							mLocalSocket = mServerSocket.accept();
							if (mLocalSocket != null) {
								Log.v(TAG, ".....Got socket: "
										+ mLocalSocket.getFileDescriptor());
								if (mLocalSocket != null) {
									startEchoThread(mLocalSocket);
								} else {
									return; // socket shutdown?
								}
							} else {
								// do nothing
								Log.v(TAG, "connecting not successed!");
							}
					} catch (final IOException e) {
						Log.v(TAG, "in accept: " + e);
					}
			};
		};
	}

	/**
	 * Send UDP packet to HAL.
	 * @param packet the data of udp packet
	 * @return 0
	 * @throws IOException ignore the Exception
	 */
	public int sendUDPPacket(final byte[] packet) throws IOException {
	    /* convert the local type to hal type.*/
	    if(udpsocketid <0){
	        return -1;
	    }
		sendUdpPacket(udpsocketid, packet);
		return 0;
	}

	/**
	 * start listen HAL TCP packet.
	 * @param tcon the connection
	 */
	public synchronized void startListenThread(final TcpConnection tcon) {
		this.con = tcon;
		if (null == t && null != temp) {
		    Log.v(TAG, "startListenThread: t== null null != temp");
		    return;
		}
		if(temp!=null && temp.isAlive()) {
		    return;
		}
		if (null != t && !t.isAlive()) {
		    mStop = false;
		    t.start();
		}
	}

	/**
	 * stop listen HAL TCP packet.
	 * @param tcon the connection
	 * @throws IOException
	 */
	public synchronized void stopListenThread() throws IOException {
		mStop = true;
		if (null != mLocalSocket) {
		    mLocalSocket.close();
		}
		if (null != mServerSocket) {
		    mServerSocket.close();
		}
		t = null;
		temp = null;
	}

	public synchronized void destoryInstance(){
	    con.closeConnection();
	    halSocket = null;
	}

	/**
	 * send sensor enable state to client.
	 * @throws IOException
	 */
	public void sendSensorEnableState() throws IOException {
		if (con != null && con.isConnected()) {
			Log.i(TAG, "Sensors State TCP packet is goning to be send");
			final SensorStatePacket ssp = new SensorStatePacket(SERVERSENSENSORSTATE);
			con.sendPacket(ssp);
		} else {
			Log.i(TAG, "Could not send Sensor enable State, because no connect.");
		}
	}
	/**
	 * send TCP packet to client, cmd1:enable, cmd2:disable.
	 *
	 * @param packet char[] for the char sequence to be send
	 * @throws IOException ignore Exception
	 */
	private void sendTCPPacketToClient(final char[] packet) throws IOException {
		CommandPacket cmdPacket;
		Log.i(TAG, "get TCP packet:" + packet[0] + "&" + packet[1]);
		if (con != null && con.isConnected()) {
		    Log.i(TAG, "get TCP packet is goning to be send.");
		      if (packet[0] == 'e') {
		            final Byte b = new Byte((byte) packet[1]);
		            final int key = b.intValue() - CHAR_ZERO;
		            cmdPacket = new CommandPacket(CommandEvent.CMD_ENABLE, key + 1);
		            SERVERSENSENSORSTATE.put(key + 1, true);
		            con.sendPacket(cmdPacket);
		        } else if (packet[0] == 'd') {
		            final Byte b = new Byte((byte) packet[1]);
		            final int key = b.intValue() - CHAR_ZERO;
		            cmdPacket = new CommandPacket(CommandEvent.CMD_DISABLE, key + 1);
		            SERVERSENSENSORSTATE.put(key + 1, false);
		            con.sendPacket(cmdPacket);
		        } else {
		            cmdPacket = null;
		            Log.e(TAG, "receive unkown command!");
		        }
		        return;
		}
	}

	   /**
     * send files to client.
     *
     * @param files the json string.
     * @throws IOException ignore Exception
	 * @throws JSONException
     */
    private void sendFileInfosToClient(String files) throws IOException {

        if (con != null && con.isConnected()) {
            JSONObject jsonObject;
            try {
                if(files == null){
                    FileInfo[] fileInfos = new FileInfo[0];
                    ListReplyPacket packet = new ListReplyPacket(fileInfos);
                    packet.setErrorType(ServerErrorType.NO_THIS_FUNCTION);
                    con.sendPacket(packet);
                }

                jsonObject = new JSONObject(files).getJSONObject("file");
                JSONArray jsonArray = jsonObject.getJSONArray("files");
                Log.d(TAG, "------>jsonArray len="+jsonArray.length());
                if(jsonArray.length() == 0){
                    FileInfo[] fileInfos = new FileInfo[0];
                    ListReplyPacket packet = new ListReplyPacket(fileInfos);
                    con.sendPacket(packet);
                    return;
                }

                FileInfo[] fileInfos = new FileInfo[jsonArray.length() -1];
                for(int i=0, index = 0;i<jsonArray.length();i++){
                    JSONObject fi = (JSONObject)jsonArray.opt(i);
                    if(null == fi){
                        continue;
                    }
                    String name = fi.getString("fn");
                    String path = fi.getString("pt");
                    int isDir = fi.getInt("dir");
                    Log.d(TAG, "------>name"+i+"= "+name+" path="+path+" isDir="+isDir);
                    FileInfo fileinfo = ServerUtil.instance().fileInfoFromjson(name, isDir, path);
                    fileInfos[index] = fileinfo;
                    index++;
                }
                Arrays.sort(fileInfos);
                ListReplyPacket packet = new ListReplyPacket(fileInfos);
                con.sendPacket(packet);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }


	public boolean sendFilesRequestToHAL(String path) {
        try {
            if(osw == null){
               return false;
            }
            osw.write(path.toCharArray());
            osw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
	}

	/**
	 * startListenThread start this thread.
	 * @param socket LocalSocket instances
	 */
	private void startEchoThread(final LocalSocket socket) {
        temp = new Thread() {
            @Override
            public void run() {
                InputStream is = null;
                InputStreamReader isr = null;
                try {
                    is = socket.getInputStream();
                    isr = new InputStreamReader(is);
                    os = mLocalSocket.getOutputStream();
                    osw = new OutputStreamWriter(os);

                    while (!mStop) {
                        char[] buffer = new char[4]; // 5 is to test, need
                                                           // repair
                        byte[] data = new byte[4];
                        Log.v(TAG, ".......before read: ");
                        try {

                            int n= isr.read(buffer);

                            Log.d(TAG, " ###isr_read n="+n);
                            if (buffer[0] != 0) {
                                Log.i(TAG, "get TCP packet:" + buffer[0] + buffer[1]);
                            }

                            if ((buffer[0] == 'e' || buffer[0] == 'd') && n == 3) {
                                if (buffer[1] >= '0' && buffer[1] <= '7') {
                                    Log.i(TAG, "send TCP packet above to client");
                                    sendTCPPacketToClient(buffer);
                                }
                              //  Log.d(TAG, " ###isendFilesRequestToHAL");
                               // sendFilesRequestToHAL("/mnt/sdcard");
                            }else if(n == 4){
                                data = TypeUtils.charsToBytes(buffer,0,n);
                                for(int i= 0;i<n;i++){
                                    Log.d(TAG, " ### data["+i+"]="+data[i]);
                                }
                                int len = TypeUtils.byte2Int(data);
                                if(len == 0){
                                    continue;
                                }
                                Log.d(TAG, " ###len ="+len);
                                buffer = new char[len];
                                int dirlen = isr.read(buffer);
                                sendFileInfosToClient(String.valueOf(buffer, 0, dirlen));
                                Log.d(TAG,"########buffer="+ String.valueOf(buffer, 0, dirlen));
                                n = 0;
                            }

                        } catch (final IOException e) {
                            Log.v(TAG, "----->>>>>>>in echo thread loop: " + e.getMessage());
                        }
                        Log.v(TAG, ".......after read: ");
                    }
                } catch (final IOException e) {
                    Log.v(TAG, "out echo thread loop: " + e.getStackTrace());
                } finally {
                    try {
                        if (null != isr) {
                            isr.close();
                        }

                    } catch (final IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        if (null != os) {
                            os.close();
                        }

                    } catch (final IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        if (null != osw) {
                            osw.close();
                        }

                    } catch (final IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            };
            temp.start();
    }
}


