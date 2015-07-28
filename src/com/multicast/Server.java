/**
 * 
 */
package com.multicast;

import java.util.ArrayList;
import java.util.HashMap;

import com.multicast.networking.NetworkInterface;
import com.multicast.networking.TCPWrapper;
import com.multicast.networking.UDPWrapper;

/**
 * @author darshanbidkar
 *
 */
public class Server implements NetworkInterface {

	private TCPWrapper tcpWrapper;
	private UDPWrapper udpWrapper;
	HashMap proxyMap = new HashMap<Proxy, ArrayList<String>>();
	HashMap groupMap = new HashMap<String, ArrayList<Proxy>>();

	int streams = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.multicast.networking.NetworkInterface#messageReceived(java.lang.String
	 * )
	 */
	@Override
	public void messageReceived(String message) {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
