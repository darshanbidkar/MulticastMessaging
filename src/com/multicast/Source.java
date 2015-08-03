package com.multicast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import com.multicast.handlers.TCPHandler;
import com.multicast.networking.NetworkInterface;

public class Source extends NetworkInterface {

	// private TCPHandler parentHandler;
	private String groupName;
	private String serverIP;
	private String selfIP;
	private TCPHandler parentHandler;

	public Source(String serverIP, String groupName) {
		parentHandler = super.createClientSocket(serverIP);
		this.groupName = groupName;
		this.serverIP = serverIP;
		try {
			selfIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			selfIP = null;
		}
	}

	private JSONObject createMessage(String message) {
		try {
			JSONObject messageObject = new JSONObject();
			messageObject.put(NetworkConstants.TYPE, NetworkConstants.DATA);
			JSONObject payload = new JSONObject();
			payload.put(NetworkConstants.DATA, message);
			payload.put(NetworkConstants.SOURCE, selfIP);
			payload.put(NetworkConstants.GROUP_NAME, groupName);
			payload.put(NetworkConstants.IS_UPSTREAM_MESSAGE, true);
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
		Source source = new Source("10.21.17.226", "RED");
		
		//User type any multicast message
		Scanner scanner = new Scanner(System.in);
		String message = scanner.nextLine();
		while (!message.equalsIgnoreCase("end")) {
			JSONObject object = source.createMessage(message);
			source.parentHandler.sendMessage(object.toString());
			message = scanner.nextLine();
		}
		scanner.close();
		System.out.println("Leaving group: " + source.groupName);
		source.closeTCPServer();
	}

	@Override
	public void messageReceived(String message) {
		//source do not receive any message
	}

	@Override
	public boolean shouldAccommodate() {
		return false;
	}

	@Override
	public void addConnection(Socket newSock) {

	}

}
