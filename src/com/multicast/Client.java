/**
 * 
 */
package com.multicast;

import com.multicast.networking.NetworkInterface;
import com.multicast.networking.TCPWrapper;
import com.multicast.networking.UDPWrapper;

/**
 * @author darshanbidkar
 *
 */
public class Client implements NetworkInterface {

	private TCPWrapper tcpWrapper;
	private UDPWrapper udpWrapper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.multicast.networking.NetworkInterface#messageReceived(java.lang.String
	 * )
	 */
	@Override
	public void messageReceived(String message) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
