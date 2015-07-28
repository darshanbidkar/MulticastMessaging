/**
 * 
 */
package com.multicast;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author darshanbidkar
 *
 */
public class Server {

	
	private HashMap<Proxy, ArrayList<String>> proxyMap = new HashMap<Proxy, ArrayList<String>>();
	private HashMap<String, ArrayList<Proxy>> groupMap = new HashMap<String, ArrayList<Proxy>>();

	int streams = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
