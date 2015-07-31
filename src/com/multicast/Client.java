/**
 * 
 */
package com.multicast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import com.multicast.networking.NetworkInterface;

/**
 * @author darshanbidkar
 *
 */
public class Client extends NetworkInterface {

	// private TCPHandler parentHandler;
	private String groupName;
	private String parentIP;

	private String selfIP;

	public Client(String parentIP, String groupName) {
		super(false);
		this.parentIP = parentIP;
		this.groupName = groupName;
		try {
			selfIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			selfIP = null;
		}
	}

	private void sendJoin() {
		try {
			JSONObject object = new JSONObject();
			object.put(NetworkConstants.TYPE, NetworkConstants.JOIN_CLIENT);
			JSONObject payload = new JSONObject();
			payload.put(NetworkConstants.SOURCE, selfIP);
			payload.put(NetworkConstants.GROUP_NAME, groupName);
			object.put(NetworkConstants.PAYLOAD, payload);
			super.udphandler.sendUDPMessage(object.toString(), parentIP);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void leaveGroup() {
		try {
			JSONObject object = new JSONObject();
			object.put(NetworkConstants.TYPE, NetworkConstants.LEAVE_CLIENT);
			JSONObject payload = new JSONObject();
			payload.put(NetworkConstants.SOURCE, selfIP);
			payload.put(NetworkConstants.GROUP_NAME, groupName);
			payload.put(NetworkConstants.IS_LAST, true);
			object.put(NetworkConstants.PAYLOAD, payload);
			super.udphandler.sendUDPMessage(object.toString(), parentIP);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private JSONObject createMessage(String message) {
		try {
			JSONObject messageObject = new JSONObject();
			messageObject.put(NetworkConstants.TYPE, NetworkConstants.DATA);
			JSONObject payload = new JSONObject();
			payload.put(NetworkConstants.DATA, message);
			messageObject.put(NetworkConstants.PAYLOAD, payload);
			return messageObject;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], args[1]);
		client.sendJoin();
		// client.wait();

		// Join successful, let user type any multicast message
		Scanner scanner = new Scanner(System.in);
		String message = scanner.nextLine();
		while (!message.equalsIgnoreCase("end")) {
			JSONObject object = client.createMessage(message);
			client.udphandler
					.sendUDPMessage(object.toString(), client.parentIP);
			message = scanner.nextLine();
		}
		scanner.close();
		System.out.println("Leaving group: " + client.groupName);
		client.leaveGroup();
		client.closeServers();
	}

	@Override
	public void messageReceived(String message) {
		try {
			JSONObject object = new JSONObject(message);
			switch (object.getString(NetworkConstants.TYPE)) {
			case NetworkConstants.JOIN_RESPONSE:
				// this.notify();
				if (object.getJSONObject(NetworkConstants.PAYLOAD).getBoolean(
						NetworkConstants.STATUS)) {
					System.out.println("Connected to proxy!\nSend a Message: ");
				} else {
					System.out.println(object.getJSONObject(
							NetworkConstants.PAYLOAD).get(
							NetworkConstants.MESSAGE));
					super.closeServers();
					System.exit(0);
				}
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean shouldAccommodate() {
		return false;
	}

	@Override
	public void addConnection(Socket newSock) {

	}

}
