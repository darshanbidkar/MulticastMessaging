package com.multicast.networking;

import java.util.Scanner;

public class TempTest implements NetworkInterface {

	TCPWrapper tcpWrapper;

	TempTest() {
		tcpWrapper = TCPWrapper.getInstance(this);
		addShutdownHook(this);

		String message = "";
		Scanner scanner = new Scanner(System.in);
		message = scanner.nextLine();
		while (!message.equalsIgnoreCase("end")) {
			tcpWrapper.handler.sendMessage(message);
			message = scanner.nextLine();
		}
		scanner.close();
	}

	public static void main(String[] args) {
		new TempTest();
	}

	@Override
	public void messageReceived(String message) {

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
