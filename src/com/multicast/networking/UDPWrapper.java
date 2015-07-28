/**
 * 
 */
package com.multicast.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author darshanbidkar
 *
 */
public class UDPWrapper {
	private DatagramSocket mServerSocket;
	private byte[] buf = new byte[1024];
	private DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
	private NetworkInterface mInterface;
	private volatile boolean isRunning = true;
	private static UDPWrapper sUDPWrapper;

	private UDPWrapper(NetworkInterface nInterface) {
		this.mInterface = nInterface;
		try {
			mServerSocket = new DatagramSocket(5050);
			new Thread() {
				@Override
				public void run() {
					while (isRunning) {
						try {
							mServerSocket.receive(receivePacket);
							String s = new String(receivePacket.getData());
							System.out.println("Received: " + s);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public synchronized static UDPWrapper getInstance(
			NetworkInterface nInterface) {
		if (UDPWrapper.sUDPWrapper == null) {
			UDPWrapper.sUDPWrapper = new UDPWrapper(nInterface);
		}
		return sUDPWrapper;
	}

	public void sendMessage(final String message, final String address,
			final int port) {
		new Thread() {

			@Override
			public void run() {
				try {
					DatagramPacket packet = new DatagramPacket(
							message.getBytes(), message.length(),
							InetAddress.getByName(address), port);
					mServerSocket.send(packet);
				} catch (UnknownHostException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		}.start();
	}
}
