/**
 * 
 */
package com.multicast.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import com.multicast.networking.NetworkInterface;

/**
 * @author darshanbidkar
 *
 */
public class TCPHandler implements Runnable {

	private Socket mSock;
	private BufferedReader mReader;
	private volatile boolean isRunning;
	private NetworkInterface mInterface;

	public TCPHandler(Socket newSock, NetworkInterface nInterface) {
		System.out.println("Creating TCP Handler for: "
				+ newSock.getInetAddress().getHostAddress());
		mInterface = nInterface;
		isRunning = true;
		try {
			mSock = newSock;
			mReader = new BufferedReader(new InputStreamReader(
					mSock.getInputStream()));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeSocket() {
		try {
			isRunning = false;
			mSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(final String message) {
		System.out.println("Sending: " + message);
		if (mSock.isClosed()) {
			System.out.println("Socket already closed");
			isRunning = false;
			return;
		}

		new Thread() {
			private PrintWriter mWriter;

			@Override
			public void run() {
				try {
					mWriter = new PrintWriter(mSock.getOutputStream());
					mWriter.println(message);
					mWriter.flush();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	// Reading from socket stream
	@Override
	public void run() {
		String message;
		while (isRunning && !mSock.isClosed()) {
			String finalMessage = "";
			try {
				while ((message = mReader.readLine()) != null) {
					System.out.println("Client: " + message);
					finalMessage += message;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Received: " + finalMessage);
			mInterface.messageReceived(finalMessage);
		}
	}
}
