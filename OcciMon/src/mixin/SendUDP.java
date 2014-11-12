package mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SendUDP extends SensorMixin {

	public void run() {
		String data;
		DatagramPacket dg;
		byte[] dataBuffer = new byte[256];
		BufferedReader input = getReaderByName("input");
		String[] UDPAddr=((String) mat.get("udpAddr")).split(":");
		try ( DatagramSocket socket = new DatagramSocket() ) {
			int port=Integer.parseInt(UDPAddr[1]);
			InetAddress address= InetAddress.getByName(UDPAddr[0]);
			try {
				while ( ( data=input.readLine() ) != null ) {
					System.out.println("sendudp: sending "+data);
					data="Data: "+data+"\n";
					dataBuffer = data.getBytes();
					dg = new DatagramPacket(dataBuffer,dataBuffer.length,address,port);
					socket.send(dg);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			return;
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		System.out.println("SendUDP terminated");
	}
}
