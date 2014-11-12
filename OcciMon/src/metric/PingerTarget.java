package metric;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.json.simple.JSONObject;

import collector.Timestamp;

public class PingerTarget implements MetricMixin {
	JSONObject collectorAttributes;
	JSONObject mixinAttributes;
	PrintWriter channel;
	final int MAXRETRY=10;
	int period=1000;

	@Override
	public void set(JSONObject cat, JSONObject mat, PrintWriter channel, int period) {
		this.collectorAttributes=cat;
		this.mixinAttributes=mat;
		this.channel=channel;
		this.period=period;
	}

	@Override
	public Object call() {

		String[] UDPAddr=((String) mixinAttributes.get("target")).split(":");
		int port=Integer.parseInt(UDPAddr[1]);
		System.out.println("Start target on "+port);
		
		try ( DatagramSocket socket = new DatagramSocket(port) ) {
			while (true) {
				DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
				socket.receive(request);
				InetAddress clientHost = request.getAddress();
				int clientPort = request.getPort();
				byte[] buf = request.getData();
				DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
				socket.send(reply);
				System.out.println("Reply sent.");
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
}