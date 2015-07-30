/**
 * 
 */
package com.multicast;

import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.multicast.handlers.TCPHandler;
import com.multicast.networking.NetworkInterface;

/**
 * @author darshanbidkar
 *
 */
public class Server extends NetworkInterface {

	// private HashMap<String, ArrayList<String>> proxyMap = new HashMap<String,
	// ArrayList<String>>();
	private HashMap<String, HashSet<String>> groupMap;
	private HashMap<String, TCPHandler> childConnectionMap;
	private final int STREAM_CAPACITY = 5;
	private int streams;

	public Server() {
		super();
		groupMap = new HashMap<String, HashSet<String>>();
		childConnectionMap = new HashMap<String, TCPHandler>();
		streams = 0;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Server();
	}

	private void forwardMessage(JSONObject request) {
		String destinationIP = getDestinationIP(request);
		TCPHandler hanlder = childConnectionMap.get(destinationIP);
		hanlder.sendMessage(request.toString());
	}

	private String getDestinationIP(JSONObject request) {
		String destinationIP = null;
		try {
			String src = request.getString(NetworkConstants.SOURCE);
			destinationIP = src.substring(src.lastIndexOf(",") + 1);
			if (isClient(request)) {
				src = src.substring(0, src.lastIndexOf(","));
				request.remove(NetworkConstants.SOURCE);
				request.put(NetworkConstants.SOURCE, src);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return destinationIP;
	}

	private boolean isClient(JSONObject request) {
		try {
			String src = request.getString(NetworkConstants.SOURCE);
			return !src.contains(",");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void messageReceived(String message) {

		try {
			JSONObject object = new JSONObject(message);
			switch (object.getString(NetworkConstants.TYPE)) {
			case NetworkConstants.JOIN_CLIENT:
				if (isClient(object)) {
					object.put(NetworkConstants.TYPE,
							NetworkConstants.JOIN_RESPONSE);
					object.getJSONObject(NetworkConstants.PAYLOAD).put(
							NetworkConstants.MESSAGE,
							NetworkConstants.CLIENT_SERVER_CONNECT_ERROR);
					object.getJSONObject(NetworkConstants.PAYLOAD).put(
							NetworkConstants.STATUS, false);
					forwardMessage(object);
					return;
				}
				if (streams == STREAM_CAPACITY) {
					object.remove(NetworkConstants.TYPE);
					object.put(NetworkConstants.TYPE,
							NetworkConstants.JOIN_RESPONSE);
					object.getJSONObject(NetworkConstants.PAYLOAD).put(
							NetworkConstants.MESSAGE,
							NetworkConstants.SERVER_CAPACITY_ERROR);
					object.getJSONObject(NetworkConstants.PAYLOAD).put(
							NetworkConstants.STATUS, false);
					forwardMessage(object);
					return;
				}

				// accepting the connection
				addGroup(getDestinationIP(object),
						object.getJSONObject(NetworkConstants.PAYLOAD)
								.getString(NetworkConstants.GROUP_NAME));
				object.getJSONObject(NetworkConstants.PAYLOAD).put(
						NetworkConstants.STATUS, true);
				object.put(NetworkConstants.TYPE,
						NetworkConstants.JOIN_RESPONSE);
				forwardMessage(object);
				break;

			case NetworkConstants.LEAVE_CLIENT:
				removeFromGroup(object);
				break;

			case NetworkConstants.DATA:
				// TODO
				break;

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void removeFromGroup(JSONObject object) {
		try {
			String groupName = object.getJSONObject(NetworkConstants.PAYLOAD)
					.getString(NetworkConstants.GROUP_NAME);
			boolean shouldRemove = !groupMap.containsKey(groupName)
					|| groupMap.get(groupName).size() == 1;
			shouldRemove &= object.getJSONObject(NetworkConstants.PAYLOAD)
					.getBoolean(NetworkConstants.IS_LAST);
			if (shouldRemove) {
				object.getJSONObject(NetworkConstants.PAYLOAD).put(
						NetworkConstants.IS_LAST, true);
				groupMap.remove(groupName);
				streams--;
			} else if (object.getJSONObject(NetworkConstants.PAYLOAD)
					.getBoolean(NetworkConstants.IS_LAST)) {
				groupMap.get(groupName).remove(getDestinationIP(object));
				object.getJSONObject(NetworkConstants.PAYLOAD).put(
						NetworkConstants.IS_LAST, false);
				streams--;
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void addGroup(String destinationIP, String groupName) {
		HashSet<String> proxies = groupMap.get(groupName);
		if (proxies == null) {
			proxies = new HashSet<String>();
		}
		if (proxies.add(destinationIP))
			streams++;
		groupMap.put(groupName, proxies);
	}

	@Override
	public boolean shouldAccommodate() {
		return false;
	}

	@Override
	public void addConnection(Socket newSock) {
		TCPHandler tcphandler = new TCPHandler(newSock, this);
		new Thread(tcphandler).start();
		childConnectionMap.put(newSock.getInetAddress().getHostAddress(),
				tcphandler);

	}

}
