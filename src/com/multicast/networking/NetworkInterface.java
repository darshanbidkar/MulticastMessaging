/**
 * 
 */
package com.multicast.networking;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.multicast.handlers.TCPHandler;
import com.multicast.handlers.UDPHandler;

/**
 * @author darshanbidkar
 *
 */
public abstract class NetworkInterface {

	private ServerSocket mServerSocket;
	private DatagramSocket mUDPSocket;
	protected volatile boolean isRunning = true;
	protected UDPHandler udphandler;

	public NetworkInterface() {
		createUDPServer();
		createTCPServer();
	}

	public NetworkInterface(boolean isProxy) {
		createUDPServer();
		if (isProxy) {
			createTCPServer();
		}
	}

	private void createUDPServer() {
		try {
			mUDPSocket = new DatagramSocket(5051);
			udphandler = new UDPHandler(mUDPSocket, this);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void createTCPServer() {
		try {
			mServerSocket = new ServerSocket(5050);
			new Thread() {
				@Override
				public void run() {
					while (isRunning) {
						Socket sock;
						try {
							sock = mServerSocket.accept();
							System.out.println("Got incoming connection: "
									+ sock.getInetAddress().getHostAddress());
							addConnection(sock);
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

	public void closeServers() {
		isRunning = false;
		closeTCPServer();
		udphandler.closeServer();
	}

	protected TCPHandler createClientSocket(String parentIP) {
		try {
			Socket socket = new Socket(parentIP, 5050);
			TCPHandler handler = new TCPHandler(socket, this);
			new Thread(handler).start();
			return handler;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void closeTCPServer() {
		this.isRunning = false;
		if (this.mServerSocket != null) {
			System.out.println("closing server");
			try {
				this.mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public abstract void messageReceived(String message);

	public abstract boolean shouldAccommodate();

	public abstract void addConnection(Socket newSock);
}
