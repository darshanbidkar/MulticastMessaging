/**
 * 
 */
package com.multicast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
public class Proxy extends NetworkInterface {

	private TCPHandler parentHandler;
	private final int PROXY_CAPACITY = 5, CLIENT_CAPACITY = 3;
	private int currentProxyCapacity = 0, currentClientCapacity = 0;

	private HashMap<String, TCPHandler> childConnectionMap;
	private HashMap<String, HashSet<String>> groupMap;

	private String selfIP;

	public Proxy(String parentIP) {
		super(true);
		parentHandler = super.createClientSocket(parentIP);

		childConnectionMap = new HashMap<String, TCPHandler>();
		groupMap = new HashMap<String, HashSet<String>>();

		try {
			selfIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			selfIP = null;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Proxy(args[0]);

	}

	private JSONObject addIPToSource(JSONObject request) {
		try {
			String srcfield = request.getJSONObject(NetworkConstants.PAYLOAD)
					.getString(NetworkConstants.SOURCE);
			srcfield += "," + selfIP;

			request.getJSONObject(NetworkConstants.PAYLOAD).remove(
					NetworkConstants.SOURCE);
			request.getJSONObject(NetworkConstants.PAYLOAD).put(
					NetworkConstants.SOURCE, srcfield);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return request;
	}

	private boolean isClient(JSONObject request) {
		try {
			String src = request.getJSONObject(NetworkConstants.PAYLOAD)
					.getString(NetworkConstants.SOURCE);
			return !src.contains(",");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getDestinationIP(JSONObject request) {
		String destinationIP = null;
		try {
			String src = request.getJSONObject(NetworkConstants.PAYLOAD)
					.getString(NetworkConstants.SOURCE);
			destinationIP = src.substring(src.lastIndexOf(",") + 1);
			if (isClient(request)) {
				src = src.substring(0, src.lastIndexOf(","));
				request.getJSONObject(NetworkConstants.PAYLOAD).remove(
						NetworkConstants.SOURCE);
				request.getJSONObject(NetworkConstants.PAYLOAD).put(
						NetworkConstants.SOURCE, src);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return destinationIP;
	}

	private void forwardMessage(JSONObject request) {
		String destinationIP = getDestinationIP(request);
		TCPHandler handler = childConnectionMap.get(destinationIP);
		System.out.println(destinationIP);
		handler.sendMessage(request.toString());
	}

	private void removeSelfIP(JSONObject request) {
		try {
			String src = request.getJSONObject(NetworkConstants.PAYLOAD)
					.getString(NetworkConstants.SOURCE);
			src = src.substring(0, src.lastIndexOf(","));
			request.getJSONObject(NetworkConstants.PAYLOAD).remove(
					NetworkConstants.SOURCE);
			request.getJSONObject(NetworkConstants.PAYLOAD).put(
					NetworkConstants.SOURCE, src);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void messageReceived(String message) {
		System.out.println("received: " + message);
		try {
			JSONObject object = new JSONObject(message);
			switch (object.getString(NetworkConstants.TYPE)) {
			case NetworkConstants.JOIN_CLIENT:
				if (isClient(object)) {
					if (currentClientCapacity == CLIENT_CAPACITY) {
						object.remove(NetworkConstants.TYPE);
						object.put(NetworkConstants.TYPE,
								NetworkConstants.JOIN_RESPONSE);
						object.getJSONObject(NetworkConstants.PAYLOAD).put(
								NetworkConstants.MESSAGE,
								NetworkConstants.CLIENT_CAPACITY_ERROR);
						object.getJSONObject(NetworkConstants.PAYLOAD).put(
								NetworkConstants.STATUS, false);
						forwardMessage(object);
						return;
					}
				} else {
					if (currentProxyCapacity == PROXY_CAPACITY) {
						object.remove(NetworkConstants.TYPE);
						object.put(NetworkConstants.TYPE,
								NetworkConstants.JOIN_RESPONSE);
						object.getJSONObject(NetworkConstants.PAYLOAD).put(
								NetworkConstants.MESSAGE,
								NetworkConstants.PROXY_CAPACITY_ERROR);
						object.getJSONObject(NetworkConstants.PAYLOAD).put(
								NetworkConstants.STATUS, false);
						forwardMessage(object);
						return;
					}
				}
				JSONObject request = addIPToSource(object);
				parentHandler.sendMessage(request.toString());
				break;

			case NetworkConstants.JOIN_RESPONSE:
				removeSelfIP(object);
				if (object.has(NetworkConstants.PAYLOAD)
						&& object.getJSONObject(NetworkConstants.PAYLOAD)
								.getBoolean(NetworkConstants.STATUS)) {
					if (isClient(object)) {
						currentClientCapacity++;
					} else {
						currentProxyCapacity++;
					}
					String destinationIP = getDestinationIP(object);
					String groupName = object.getJSONObject(
							NetworkConstants.PAYLOAD).getString(
							NetworkConstants.GROUP_NAME);
					addGroup(destinationIP, groupName);
					forwardMessage(object);
				} else {
					object.getJSONObject(NetworkConstants.PAYLOAD).put(
							NetworkConstants.MESSAGE,
							NetworkConstants.SERVER_CAPACITY_ERROR);
					forwardMessage(object);
				}
				break;

			case NetworkConstants.LEAVE_CLIENT:
				removeFromGroup(object);
				object = addIPToSource(object);
				parentHandler.sendMessage(object.toString());
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
			} else if (object.getJSONObject(NetworkConstants.PAYLOAD)
					.getBoolean(NetworkConstants.IS_LAST)) {
				groupMap.get(groupName).remove(getDestinationIP(object));
				object.getJSONObject(NetworkConstants.PAYLOAD).put(
						NetworkConstants.IS_LAST, false);
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
		proxies.add(destinationIP);
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
