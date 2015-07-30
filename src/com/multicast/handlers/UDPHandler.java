/**
 * 
 */
package com.multicast.handlers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.multicast.networking.NetworkInterface;

/**
 * @author darshanbidkar
 *
 */
public class UDPHandler implements Runnable {
	private volatile boolean isRunning = true;
	private DatagramSocket mUDPSocket;
	private NetworkInterface mInterface;

	public UDPHandler(DatagramSocket udpSocket, NetworkInterface nInterface) {
		mUDPSocket = udpSocket;
		try {
			mUDPSocket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.mInterface = nInterface;
	}

	public void closeServer() {
		isRunning = false;
	}

	public void sendUDPMessage(final String message, final String address) {

		new Thread() {
			@Override
			public void run() {
				try {
					DatagramPacket packet = new DatagramPacket(
							message.getBytes(), message.length(),
							InetAddress.getByName(address), 5051);
					mUDPSocket.send(packet);
				} catch (UnknownHostException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		}.start();
	}

	public void run() {
		final byte[] buf = new byte[1024];

		new Thread() {
			DatagramPacket receivePacket;

			@Override
			public void run() {
				while (isRunning) {
					receivePacket = new DatagramPacket(buf, buf.length);
					try {
						mUDPSocket.receive(receivePacket);
						String s = new String(receivePacket.getData());
						System.out.println("Received: " + s);
						mInterface.messageReceived(s);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

}
