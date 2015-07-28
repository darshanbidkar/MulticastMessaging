/**
 * 
 */
package com.multicast;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import com.multicast.handlers.TCPHandler;
import com.multicast.networking.NetworkInterface;

/**
 * @author darshanbidkar
 *
 */
public class Client extends NetworkInterface {

	private TCPHandler parentHandler;
	private String groupName;
	private String parentIP;

	public Client(String parentIP, String groupName) {
		super(true);
		this.parentIP = parentIP;
		this.groupName = groupName;
		parentHandler = super.createClientSocket(parentIP);
	}

	private void sendJoin() {

	}

	private void leaveGroup() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Client client = new Client("", "");
		try {
			client.sendJoin();
			client.wait();

			// Join successful, let user type any multicast message
			Scanner scanner = new Scanner(System.in);
			String message = scanner.nextLine();
			while (!message.equalsIgnoreCase("end")) {
				client.udphandler.sendUDPMessage(message, client.parentIP);
			}
			scanner.close();
			System.out.println("Leaving group: " + client.groupName);
			client.leaveGroup();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void messageReceived(String message) {

		this.notify();
	}

	@Override
	public boolean shouldAccommodate() {
		return false;
	}

	@Override
	public void addConnection(Socket newSock) {

	}

}
