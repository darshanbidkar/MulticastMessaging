/**
 * 
 */
package com.multicast;

import java.net.Socket;

import com.multicast.handlers.TCPHandler;
import com.multicast.networking.NetworkInterface;

/**
 * @author darshanbidkar
 *
 */
public class Proxy extends NetworkInterface {

	private String parentIP;
	private TCPHandler parentHandler;
	private final int PROXY_CAPACITY = 5, CLIENT_CAPACITY = 3;
	private int currentProxyCapacity = 0, currentClientCapacity = 0;

	public Proxy(String parentIP) {
		super(false);
		this.parentIP = parentIP;
		parentHandler = super.createClientSocket(parentIP);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Proxy proxy = new Proxy("");
	}

	@Override
	public void messageReceived(String message) {

	}

	@Override
	public boolean shouldAccommodate() {
		return false;
	}

	@Override
	public void addConnection(Socket newSock) {
		TCPHandler tcphandler = new TCPHandler(newSock, this);
	}

}
