package com.multicast.networking;

/**
 * @author darshanbidkar
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPWrapper {

	private ServerSocket mServerSocket;
	private NetworkInterface mInterface;
	private static TCPWrapper sNetworkWrapper;
	private volatile boolean isRunning = true;
	public SocketHandler handler;

	private TCPWrapper(NetworkInterface nInterface) {
		this.mInterface = nInterface;
		try {
			mServerSocket = new ServerSocket(5050);
			new Thread() {
				@Override
				public void run() {
					while (isRunning) {
						Socket sock;
						try {
							sock = mServerSocket.accept();
							handler = new SocketHandler(sock);
							// mInterface.addConnection(handler);
							Thread t = new Thread(handler);
							t.start();
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

	public synchronized static TCPWrapper getInstance(
			NetworkInterface nInterface) {
		if (TCPWrapper.sNetworkWrapper == null) {
			TCPWrapper.sNetworkWrapper = new TCPWrapper(nInterface);
		}
		return sNetworkWrapper;
	}

	class SocketHandler implements Runnable {
		private Socket mSock;
		private BufferedReader mReader;
		private String message;

		public SocketHandler(Socket newSock) {

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
				mSock.close();
			} catch (IOException e) {
			}
		}

		public void sendMessage(final String message) {
			System.out.println("Sending: " + message);
			if (mSock.isClosed()) {
				System.out.println("Socket already closed");
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
			while (isRunning && mServerSocket != null && !mSock.isClosed()) {
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
				TCPWrapper.this.mInterface.messageReceived(finalMessage);
			}
		}
	}

	public void closeServer() {
		if (this.isRunning && this.mServerSocket != null) {
			System.out.println("closing server");
			this.isRunning = false;
			try {
				this.mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
