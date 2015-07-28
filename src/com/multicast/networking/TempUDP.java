package com.multicast.networking;

import java.util.Scanner;

public class TempUDP implements NetworkInterface {

	TempUDP() {
		UDPWrapper tcpWrapper = UDPWrapper.getInstance(this);
		addShutdownHook(this);

		String message = "";
		Scanner scanner = new Scanner(System.in);
		message = scanner.nextLine();
		while (!message.equalsIgnoreCase("end")) {
			tcpWrapper.sendMessage(message, "192.168.43.197", 5050);
			message = scanner.nextLine();
		}
		scanner.close();
	}

	public static void main(String[] args) {

	}

	@Override
	public void messageReceived(String message) {
		new TempUDP();
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
