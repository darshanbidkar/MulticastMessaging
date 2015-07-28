/**
 * 
 */
package com.multicast.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author darshanbidkar
 *
 */
public class UDPWrapper {
	private ServerSocket mServerSocket;
	private NetworkInterface mInterface;
	private static UDPWrapper sNetworkWrapper;
	private volatile boolean isRunning = true;

	private UDPWrapper(NetworkInterface nInterface) {
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
							Thread t = new Thread(new ClientHandler(sock));
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

	public synchronized static UDPWrapper getInstance(
			NetworkInterface nInterface) {
		if (UDPWrapper.sNetworkWrapper == null) {
			UDPWrapper.sNetworkWrapper = new UDPWrapper(nInterface);
		}
		return sNetworkWrapper;
	}

	public void sendMessage(final String message, final String address,
			final int port) {
		new Thread() {
			private PrintWriter mWriter;

			@Override
			public void run() {
				try {
					Socket sock = new Socket(address, port);
					mWriter = new PrintWriter(sock.getOutputStream());
					mWriter.println(message);
					mWriter.flush();
					sock.close();
				} catch (UnknownHostException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		}.start();
	}

	private class ClientHandler implements Runnable {
		private Socket mSock;
		private BufferedReader mReader;
		private String message;

		public ClientHandler(Socket newSock) {

			try {
				mSock = newSock;
				mReader = new BufferedReader(new InputStreamReader(
						mSock.getInputStream()));

			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			String finalMessage = "";
			try {
				while ((message = mReader.readLine()) != null) {
					System.out.println("Client: " + message);
					finalMessage += message;
				}
			} catch (IOException e) {

			}
			UDPWrapper.this.mInterface.messageReceived(finalMessage);
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
