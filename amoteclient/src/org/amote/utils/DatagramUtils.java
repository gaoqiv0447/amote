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

package org.amote.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.amote.client.Remote;

import android.util.Log;

/**
 *
 * @author weidawei
 *  class used to send datagram.
 */
public final class DatagramUtils {
    private static final boolean DEBUG = true;
    /**
     * Tag for debug.
     */
    private static final String DEBUG_TAG = "DatagramUtils";

    /**
     * The singleton instance of Remote, use this to get udp port and send data.
     */
    private Remote remoteInstance;

    /**
     * The singleton instance.
     */
    private static DatagramUtils mInstance = null;

    /**
     * Object that we will wait on when the mouse is not moving.
     */
    private Object waitForSensorData ;

    /**
     * Byte array is going to be send.
     */
    private byte[] data;

    /**
     * DatagramPacket to contains send data.
     */
    private DatagramPacket packet = null;
    /**
     * Construct the remoteInstance.
     */
    private boolean start = true;

    private DatagramUtils() {
        remoteInstance = Remote.getInstance();
        waitForSensorData = DatagramUtils.class;
    }
    /**
     *
     * @return the singleton instance.
     */
    public static synchronized  DatagramUtils instance() {
        if (mInstance == null) {
            mInstance = new DatagramUtils();

        }
        return mInstance;
    }

    /**
     * convert bytes to DatagramPacket.
     * @param data
     *          the bytes to packet.
     */
    public synchronized void makeDatagramPacket(final byte[] data) {
        packet = new DatagramPacket(data, data.length, remoteInstance.getServerInetAddress(),
                remoteInstance.getServerUdpPort());
        if(DEBUG)  Log.i(DEBUG_TAG, "makeDatagramPacket notify:" + remoteInstance.getServerInetAddress() 
        		+ ":" + remoteInstance.getServerUdpPort());
        synchronized (waitForSensorData) {
            if(DEBUG)  Log.i(DEBUG_TAG, "makeDatagramPacket notify-inner");
            waitForSensorData.notify();
        }
    }

    /**
     * Stop the runing thread.
     */
    public void stopThread(){
        start = false;
        mInstance = null;
    }

    /**
     *
     * @author weidawei
     *  the thread used to send data to server.
     */
     class SensorSendingThread implements Runnable {
         /**
          * use waitForSensorData.wait() to block the looping.
          */
        public void run() {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                while (start) {

                    if (packet != null) {
                        if(DEBUG) Log.i(DEBUG_TAG, "Sending packet");
                        try {
                            if (!remoteInstance.isConnected()) {
                                remoteInstance.connect(false);
                            }
                            socket.send(packet);

                         if(DEBUG)  Log.i(DEBUG_TAG, "Packet sent");
                        } catch (final IOException e) {
                            Log.e(DEBUG_TAG, e.getMessage(), e);
                        } finally {
                            packet = null;
                        }
                    } else {
                        try {
                            synchronized (waitForSensorData) {
                                waitForSensorData.wait();
                            }
                        } catch (final InterruptedException e) {
                            Log.e(DEBUG_TAG, e.getMessage(), e);
                            return;
                        }
                    }
                }
            } catch (final SocketException e) {
                Log.e(DEBUG_TAG, e.getMessage(), e);
            }finally{
                if(socket!=null){
                    socket.close();
                }
            }


        }
    }

}
