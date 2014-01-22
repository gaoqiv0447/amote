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

import java.io.StreamCorruptedException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.gmote.common.DataReceiverIF;
import org.gmote.common.PasswordProvider;
import org.gmote.common.TcpConnection;
import org.gmote.common.security.AuthenticationHandler;

import com.aisino.server.media.MediaInfoUpdater;
import com.aisino.server.visualtouchpad.VisualTouchpad;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TcpConnectionHandler {
    private static final String TAG = "TcpConnectionHandler";

    private static TcpConnectionHandler instance = null;

    private static DataReceiverIF dataReceiver;

    private static final int MAX_OLD_SESSION_IDS = 5;

    private List<String> addressListeningOn = new ArrayList<String>();

    private UpnpUtil upnpUtil = new UpnpUtil();

    private Handler mHandler;

    private TcpConnection con = null;

    public static synchronized TcpConnectionHandler instance() {
        if (instance == null) {
          instance = new TcpConnectionHandler();
        }
        return instance;
      }

      public static synchronized TcpConnectionHandler instance(DataReceiverIF newDataReceiver) {
        dataReceiver = newDataReceiver;
        if (instance == null) {
          instance = new TcpConnectionHandler();
        }
        return instance;
      }

      public synchronized void addConnectionListener(InetAddress address) {
          addressListeningOn.add(address.getHostAddress().toLowerCase());
          int port = upnpUtil.getPort(address);
          Log.d(TAG, "**********addConnectionListener***********");
          sendMessageToService(address,port);
          new Thread(new ConnectionReceiverThread(port)).start();
		  Log.d(TAG,"==========ConnectionReceiverThread Start!!!");
        }

        public boolean isListeningOnAddress(InetAddress address) {
            return addressListeningOn.contains(address.getHostAddress().toLowerCase());
        }


        public synchronized void listenOnAllIpAddresses(Handler handler) throws SocketException {
            mHandler = handler;
            for (InetAddress address : ServerUtil.findAllLocalIpAddresses(true)) {
              addConnectionListener(address);
            }
          }

        private class ConnectionReceiverThread implements Runnable {

            private int tcpPort;

            public ConnectionReceiverThread(int tcpPort) {
              this.tcpPort = tcpPort;
            }

            @Override
            public void run() {
              // Circular list that will contain the last 5 session ids.
              List<String> latestSessionIds = new ArrayList<String>(MAX_OLD_SESSION_IDS);

              PasswordProvider passProvider = new PasswordProvider() {
                public String fetchPassword() {
                  return StringEncrypter.readPasswordFromFile();
                }
              };

              AuthenticationHandler authHandler = new AuthenticationHandler(GmoteServer.VERSION,
                  GmoteServer.MINIMUM_CLIENT_VERSION);



              while (true) {
//                TcpConnection con = null;
                try {
                  Log.d(TAG,"--->Waiting for TCP connection on port: " + tcpPort);
                  con = new TcpConnection(authHandler);
                  Thread thread = con.listenForConnections(tcpPort, dataReceiver, passProvider);
                  if (thread != null) {
                    MediaInfoUpdater.instance().setClientConnection(con);
                    MulticastServerThread.setConnectedClientIp(con.getConnectedClientAddress());
                    addToSessionList(con.getSessionId(), latestSessionIds);
                    VisualTouchpad.instance().setConnection(con);
                    GmoteServer.mTcpCon = con;
                    Log.d(TAG,"*****VisualTouchpad still has somethings to do!***** client ip=" + con.getConnectedClientAddress());
                    sendMessageToService(con.getConnectedClientAddress().getHostAddress(), AisinoService.RECEVIE_AUTHENTICATION_SUCCESS_MSG);
                        if (Settings.getKeySensorPlugin() || Settings.JUST_FOR_OURSELF) {
                            HalSocket hs = HalSocket.getInstance();
                            // start a new thread wait for the cmd enable &
                            // disable
                            hs.startListenThread(con);
                            // send sensor enable state Hash map.
                            hs.sendSensorEnableState();
                        }

                  }else{
                      Log.d(TAG,"*****handleServerSideAuthentication failed*****");
                      sendMessageToService(con.getConnectedClientAddress().getHostAddress(), AisinoService.RECEVIE_AUTHENTICATION_FAILED_MSG);
                      //hs.stopListenThread(con);
                  }
                } catch (StreamCorruptedException e) {
                  // This may be an HTTP request. Try to handle it.
                  Log.d(TAG,"Encountered a StreamCorruptedException: trying to handle it as an HTTP request");
                  Socket connectionSocket;
                  if (con != null && (connectionSocket = con.getConnectionSocket()) != null) {
                    GmoteHttpServer httpServer = new GmoteHttpServer(connectionSocket);
                    httpServer.handleHttpRequestAsync(latestSessionIds);
                  } else {
                    Log.d(TAG,
                        "Unable to handle StreamCorruptedException: " + e.getMessage(), e);
                  }

                } catch (BindException e) {
                  // The port is already in use. We'll exit.
                  Log.d(TAG, e.getMessage(), e);
                  String errorMessage = "Unable to use port: "
                      + tcpPort
                      + ". There may be an instance of"
                      + " Gmote already running. Please close it and try again. For more help, please visit:";
                  Log.d(TAG,errorMessage);
                  Log.d(TAG,"******ConnectionReceiverThread*********");
                  //JOptionPane.showMessageDialog(null, errorMessage);
                  System.exit(1);
                } catch (Exception e) {
                  // Catching all exceptions since this is the top layer of our app and
                  // we'll try to recover from these exceptions.
                  Log.d(TAG, e.getMessage(), e);
                }
              }

            }

            private void addToSessionList(String sessionId, List<String> latestSessionIds) {
              if (latestSessionIds.size() >= MAX_OLD_SESSION_IDS) {
                latestSessionIds.remove(0);
              }
              latestSessionIds.add(sessionId);
            }

          }
        private void sendMessageToService(InetAddress address,int port){
            Message msg = mHandler.obtainMessage();
            msg.what = AisinoService.RECEVIE_NETWORK_ADDRESSES;
            msg.arg1 = port;
            msg.obj = address;
            mHandler.sendMessage(msg);
        }
        private void sendMessageToService(String obj , int what){
            Message msg = mHandler.obtainMessage();
            msg.obj = obj;
            msg.what = what;
            mHandler.sendMessage(msg);
        }

}
