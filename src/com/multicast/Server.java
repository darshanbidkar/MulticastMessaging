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
public class Server {

	private TCPWrapper tcpWrapper;
	private UDPWrapper udpWrapper;
	private HashMap<Proxy, ArrayList<String>> proxyMap = new HashMap<Proxy, ArrayList<String>>();
	private HashMap<String, ArrayList<Proxy>> groupMap = new HashMap<String, ArrayList<Proxy>>();

	int streams = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static void addShutdownHook(NetworkInterface p) {
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new Thread() {
			@Override
			public void run() {
				TCPWrapper.getInstance(p).closeServer();
			}
		});
	}

}
