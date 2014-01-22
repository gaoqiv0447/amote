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

import 	java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.List;

import org.gmote.common.MulticastClient;
import org.gmote.common.Protocol.UdpPacketTypes;
import org.gmote.common.ScreenInfo;
import org.gmote.common.MotionEventStruct;
import org.gmote.common.packet.MotionEventPacket;
import org.gmote.common.utils.TypeUtils;

import com.aisino.server.settings.PreferredPorts;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MulticastServerThread implements Runnable {
    public static final int MULTICAST_LISTENING_PORT = 9901;

    public static final String GROUP_NAME = "230.0.0.1";


    public static final String TAG = "MulticastServerThread";

    private static final boolean DEBUG = false;

    // The port used to exchange udp data. This is where we exchange data such
    // as
    // mouse packets. It can be the same as the udp service discovery port, or
    // different.
    private static int serverUdpListeningPort = MULTICAST_LISTENING_PORT;

    private String groupName;

    // The listening port of the current socket.
    private int socketListeningPort;

    private static InetAddress connectedClientIp = null;

    private static Handler mHandler;

    /**
     * Creates a thread that will send out discovery notifications. We
     * explicitly listen on each local interface since this resolves the
     * following bug which is related to having multiple network cards:
     * <p>
     * 1. Listen on socket without providing a specific ip (by default, java
     * will listen on all network interfaces)
     * </p>
     * <p>
     * 2. Receive an IP request from a client
     * </p>
     * <p>
     * 3. If we have multiple local ip's (ex: a wifi connection and wired
     * connection), we won't know which ip to return (and will often return the
     * wrong one if we simply call InetAddress.getLocalHost) Note: If we pass in
     * null for 'localIpAddress', the we will revert back to listening on all
     * local ip addresses and take a guess as to which ip we should return to
     * the client. This mechanism should only be used if there is a problem
     * listening on local interfaces.
     * </p>
     */
    public MulticastServerThread(String groupName, int socketListeningPort) {

        this.groupName = groupName;
        this.socketListeningPort = socketListeningPort;
    }

    public static void listenForIpRequests(int udpPort,Handler handler) {
        mHandler = handler;
        // Ports that the server is listening on.
        serverUdpListeningPort = udpPort;

        MulticastServerThread multiCon;
        if (udpPort != MULTICAST_LISTENING_PORT) {
            // The user has chosen to listen for mouse events on a different
            // port than
            // service discovery. Make sure we listen on that port as well.
            multiCon = new MulticastServerThread(null, udpPort);
            new Thread(multiCon, "UdpMouseThread").start();
        }

        multiCon = new MulticastServerThread(GROUP_NAME, MULTICAST_LISTENING_PORT);
        new Thread(multiCon, "MulticastThread").start();
    }

    @Override
    public void run() {
        MulticastSocket socket = join(groupName, socketListeningPort);
        if (socket == null) {
            String message = "Unable to join the multicast socket. Is the computer connected to a network? Exiting Gmote.";
            Log.d(TAG, message);
            mHandler.sendEmptyMessage(AisinoService.NETWORK_DISABLE);
            return;
        }
        byte[] inBuffer = new byte[500*2];
        Log.d(TAG, "Listening for udp packets on " + socketListeningPort);
        int errorCount = 0;
        while (true) {
            try {
                receivePacket(socket, inBuffer);
            } catch (Exception e) {
                // Catching all exceptions here since this is the top level of
                // our
                // multicast thread and we never want it to die.
                Log.d(TAG, e.getMessage());
                errorCount++;
                if (errorCount >= 10) {
                    // JOptionPane.showMessageDialog(null,
                    // "Too many errors in udp class. Please see the logs for more details or visit http://www.gmote.org/faq -- "
                    // + e.getMessage());
                    System.exit(1);
                }

            }
        }
    }

    private MulticastSocket join(String groupName, int udpPort) {
        try {

            MulticastSocket msocket;
            msocket = new MulticastSocket(udpPort);
            if (groupName != null) {
                InetAddress group = InetAddress.getByName(groupName);
                msocket.joinGroup(group);
            }
            return msocket;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    public void receivePacket(MulticastSocket multicastSocket, byte[] inBuffer) {
        try {
            DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);

            // Wait for packet
            Log.d(TAG, "--->Waiting for udp packet request on port " + multicastSocket.getLocalPort() + " buffer[0]"+inBuffer[0]);
            multicastSocket.receive(packet);
            if (inBuffer[0] == UdpPacketTypes.SERVICE_DISCOVERY.getId()) {
               // Log.d(TAG, "--->found udp packet request UdpPacketTypes.SERVICE_DISCOVERY");
                handleServiceDiscoveryRequest(multicastSocket, packet, inBuffer);
            } else if (inBuffer[0] == UdpPacketTypes.MOUSE_MOVE.getId()) {
               // Log.d(TAG, "--->found udp packet request UdpPacketTypes.MOUSE_MOVE");
                handleMouseMoveRequest(packet, inBuffer);
            } else if (inBuffer[0] == UdpPacketTypes.SENSOR_EVENT.getId()) {
            	// Log.d(TAG, "--->found udp packet request UdpPacketTypes.SENSOR_EVENT");
            	 handleSensorEventRequest(inBuffer);
            } else if(inBuffer[0] == UdpPacketTypes.MOTION_EVENT.getId()) {
				//Log.d(TAG, "--->found udp packet request UdpPacketTypes.MOTION_EVENT");
				handleMotionEventRequest(packet,inBuffer);
			}    else {
                Log.d(TAG, "---->Received unrecognized udp packet. Ignoring it.");
                // handleLegacyServiceDiscoveryRequest(multicastSocket, packet);
            }
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * handle the udp packet, send packet to HAL with local socket(tcp)
     * @param data
     * @throws IOException
     *
     *
     *
     */
    private void handleSensorEventRequest( byte[] data) throws IOException {
       // byte[] tmp ={data[10],data[11],data[12],data[13]};
        if (DEBUG)
            Log.d(TAG, "---->Received Sensor udp packet. name=" + Thread.currentThread().getName());
        if (Settings.getKeySensorPlugin() || Settings.JUST_FOR_OURSELF) {
            HalSocket hs = HalSocket.getInstance();
            int res = hs.sendUDPPacket(data);

            if (res < 0) {
                mHandler.sendEmptyMessage(AisinoService.HAL_SERVER_NOT_AVIABLE);
            }
        }
    }

    /**
     * Handles a mouse move request from the client. In order to accept the mouse
     * move event, the client must have an active tcp connection with our server
     * (we verify this by matching the ip address).
     *
     * TODO(mstogaitis): Consider a better security mechanism such as signing the
     * mouse packets with the password.
     *
     * @param packet
     * @param data
     *          a 5 byte packet, byte 0 contains an identifier, bytes 1 and 2
     *          contain a 'short' describing the XMovement, and bytes 3 and 4
     *          contain a short describing the YMovement.
     */
    private void handleMouseMoveRequest(DatagramPacket packet, byte[] data) {

      InetAddress clientAddress = packet.getAddress();
      if (isCorrectIp(clientAddress)) {
        if (packet.getLength() == 5) {
          short xDiff = data[2];
          xDiff = (short) ((xDiff << 8) & 0xFF00);
          xDiff = (short) (xDiff | (data[1] & 0x00FF));

          short yDiff = data[4];
          yDiff = (short) ((yDiff << 8 & 0xFF00));
          yDiff = (short) (yDiff | (data[3] & 0x00FF));
        // if(DEBUG) Log.d(TAG, "----->handleMouseMoveRequest xdiff=" +xDiff +" yDiff="+yDiff);
          TrackpadHandler.instance().handleMoveMouseCommand(xDiff, yDiff);
        }
      } else {
        Log.d(TAG, "----->Received a mouse move request from an ip who is not connected to us: packetIp="
                + clientAddress + " tcpConnectionIp=" + connectedClientIp + ". Ignoring the packet.");
      }
    }

	private void handleMotionEventRequest(DatagramPacket packet, byte[] data) throws IOException {
		try {
			ByteArrayInputStream bint = new ByteArrayInputStream(data,1,data.length-1);
			ObjectInputStream oint = new ObjectInputStream(bint);
			//Object obj = oint.readObject();
			//Log.d(TAG,"thisisisisis:\n"+obj.toString());
			MotionEventStruct eStruct = (MotionEventStruct)oint.readObject();
			MotionEventPacket event_packet = new MotionEventPacket(eStruct);
			TrackpadHandler.instance().handeMotionEvent((MotionEventPacket)event_packet);
		}
		catch(Exception ex) {
		   ex.printStackTrace();
		}
	}

    private static synchronized boolean isCorrectIp(InetAddress clientAddress) {
        return clientAddress.equals(connectedClientIp);
    }

    /**
     * Sends a packet to the client identifying our server's name and ip.
     *
     * @param multicastSocket
     * @param packet
     * @param data Byte 0 is the packet id, bytes 1 to 4 is an int identifying
     *            the port of the client to which we should send a reply.
     * @throws IOException
     * @throws UnknownHostException
     */
    private void handleServiceDiscoveryRequest(MulticastSocket multicastSocket,
            DatagramPacket packet, byte data[]) throws UnknownHostException, IOException {

        if (packet.getLength() == 5) {
            int port = data[4] << (24 & 0xFF000000);
            port = port | ((data[3] << 16) & 0x00FF0000);
            port = port | ((data[2] << 8) & 0x0000FF00);
            port = port | (data[1] & 0x000000FF);
            Log.d(TAG, "------->Received an ip request,client port number is="+port);
            sendDiscoveryReply(multicastSocket, packet, port);
        }
    }

    /**
     * @param multicastSocket
     * @param packet
     * @throws IOException
     */

    private void sendDiscoveryReply(MulticastSocket multicastSocket, DatagramPacket packet,
            int remotePort) throws UnknownHostException, IOException {
//        // Reply with all local IPs
        byte[] replyBuff;

        List<InetAddress> addresses = ServerUtil.findAllLocalIpAddresses(true);
        for (InetAddress address : addresses) {
            Log.d(TAG, "---->local address="+address.getHostAddress()+" name="+address.getHostName());
            if (!TcpConnectionHandler.instance().isListeningOnAddress(address)) {
                Log.d(TAG,
                        "---->Multicase server thread noticed a local ip that we are not listening on. Adding it to the listening pool");
                TcpConnectionHandler.instance().addConnectionListener(address);
            }

            Integer port = PreferredPorts.instance().getPreferredPort(address.getHostAddress());

            if (port == null) {
                Log.d(TAG,"Prefered port is null for connection that should have been added. "
                        + address.getHostAddress() + " "
                        + PreferredPorts.instance().getPreferedPorts());
                continue;
            }

            replyBuff = createIpReply(address.getHostAddress(), InetAddress.getLocalHost()
                    .getHostName(), port);
            DatagramPacket replyPacket = new DatagramPacket(replyBuff, replyBuff.length);
            replyPacket.setAddress(packet.getAddress());
            replyPacket.setPort(remotePort);
            Log.d(TAG, "Sending packet to: " + packet.getAddress());

            multicastSocket.send(replyPacket);
        }
    }

    public static synchronized void setConnectedClientIp(InetAddress clientIp) {
        connectedClientIp = clientIp;
      }

    /**
     * Create an IP | Hostname reply packet to send to the clients. We can't just
     * send the hostName since Android clients run under linux which has problems
     * with windows host names.
     * Still don't know it can be run under Android
     * @param port
     *
     * @return
     */
    private byte[] createIpReply(String localAddress, String hostName, int port) {
       StringBuffer sb = new StringBuffer(localAddress);
       sb.append(MulticastClient.FIELD_SEPARATOR);
       sb.append(hostName);
       sb.append( MulticastClient.FIELD_SEPARATOR);
       sb.append(port);
       sb.append(MulticastClient.FIELD_SEPARATOR);
       sb.append(serverUdpListeningPort);
      return sb.toString().getBytes();
    }



}
