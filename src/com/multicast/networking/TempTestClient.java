/**
 * 
 */
package com.multicast.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author darshanbidkar
 *
 */
public class TempTestClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			final Socket socket = new Socket("192.168.43.197", 5050);
			new Thread() {
				@Override
				public void run() {
					while (true) {
						String finalMessage = "";
						try {
							BufferedReader mReader = new BufferedReader(
									new InputStreamReader(
											socket.getInputStream()));
							String message = "";
							while ((message = mReader.readLine()) != null) {
								System.out.println("Client: " + message);
								finalMessage += message;
							}
							System.out.println(finalMessage);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			String msg;
			PrintWriter writer = new PrintWriter(socket.getOutputStream());
			while (!(msg = br.readLine()).equalsIgnoreCase("end")) {
				writer.println(msg);
				writer.flush();
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
