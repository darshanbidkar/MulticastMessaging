/**
 * 
 */
package com.multicast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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

	private void forwardUDPMessage(JSONObject request) {
		String destinationIP = getDestinationIP(request);
		super.udphandler.sendUDPMessage(request.toString(), destinationIP);
	}

	@Override
	public synchronized void messageReceived(String message) {
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
						forwardUDPMessage(object);
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
						forwardUDPMessage(object);
						return;
					}
				}
				JSONObject request = addIPToSource(object);
				parentHandler.sendMessage(request.toString());
				break;

			case NetworkConstants.JOIN_RESPONSE:
				removeSelfIP(object);
				boolean isClient = isClient(object);
				if (object.has(NetworkConstants.PAYLOAD)
						&& object.getJSONObject(NetworkConstants.PAYLOAD)
								.getBoolean(NetworkConstants.STATUS)) {
					if (isClient) {
						currentClientCapacity++;
					} else {
						currentProxyCapacity++;
					}
					String destinationIP = getDestinationIP(object);
					String groupName = object.getJSONObject(
							NetworkConstants.PAYLOAD).getString(
							NetworkConstants.GROUP_NAME);
					addGroup(destinationIP, groupName);
				}
				if (isClient)
					forwardUDPMessage(object);
				else
					forwardMessage(object);
				break;

			case NetworkConstants.LEAVE_CLIENT:
				removeFromGroup(object);
				break;

			case NetworkConstants.DATA:
				sendToChildren(new JSONObject(object.toString()));
				if (object.getJSONObject(NetworkConstants.PAYLOAD).getBoolean(
						NetworkConstants.IS_UPSTREAM_MESSAGE)) {
					parentHandler.sendMessage(object.toString());
				}
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void sendToChildren(JSONObject object) {
		try {
			String destinationIP = getDestinationIP(object);
			object.getJSONObject(NetworkConstants.PAYLOAD).remove(
					NetworkConstants.SOURCE);
			object.getJSONObject(NetworkConstants.PAYLOAD).put(
					NetworkConstants.SOURCE, selfIP);
			object.getJSONObject(NetworkConstants.PAYLOAD).remove(
					NetworkConstants.IS_UPSTREAM_MESSAGE);
			object.getJSONObject(NetworkConstants.PAYLOAD).put(
					NetworkConstants.IS_UPSTREAM_MESSAGE, false);
			String groupName = object.getJSONObject(NetworkConstants.PAYLOAD)
					.getString(NetworkConstants.GROUP_NAME);
			Iterator<String> childrenIPs = groupMap.get(groupName).iterator();
			while (childrenIPs.hasNext()) {
				String ip = childrenIPs.next();
				if (destinationIP.equalsIgnoreCase(ip))
					continue;
				if (childConnectionMap.containsKey(ip)) {
					childConnectionMap.get(ip).sendMessage(object.toString());
				} else {
					super.udphandler.sendUDPMessage(object.toString(), ip);
				}
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
				object = addIPToSource(object);
				parentHandler.sendMessage(object.toString());
			} else if (object.getJSONObject(NetworkConstants.PAYLOAD)
					.getBoolean(NetworkConstants.IS_LAST)) {
				groupMap.get(groupName).remove(getDestinationIP(object));
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
