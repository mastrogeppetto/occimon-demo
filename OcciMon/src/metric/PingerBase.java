package metric;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.lang.System;

import org.json.simple.JSONObject;

import collector.Timestamp;

public class PingerBase implements MetricMixin {
	JSONObject collectorAttributes;
	JSONObject mixinAttributes;
	PrintWriter channel;
	final int MAXRETRY=10;
	int period=1000;
	Timestamp timestamp=null;

	@Override
	public void set(JSONObject cat, JSONObject mat, PrintWriter channel, int period) {
		this.collectorAttributes=cat;
		this.mixinAttributes=mat;
		this.channel=channel;
		this.period=period;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object call() {

		String id = (String) mixinAttributes.get("out");
		int maxDelay = ((Number) mixinAttributes.get("maxdelay")).intValue();
		String[] UDPAddr=((String) mixinAttributes.get("target")).split(":");
		int port=Integer.parseInt(UDPAddr[1]);
		InetAddress address;
		try {
			address = InetAddress.getByName(UDPAddr[0]);
		} catch (UnknownHostException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return null;
		}

		try ( DatagramSocket socket = new DatagramSocket() ) {
			int sequence_number = 0;
			while ( true ) {
				JSONObject value = new JSONObject();
				JSONObject rttMeasure = new JSONObject();
				long now = System.nanoTime();
				String str = "PING " + sequence_number + " " + now + " ";
				System.out.println("Sending "+str);
				byte[] buf = new byte[1024];
				buf = str.getBytes();
				DatagramPacket ping = new DatagramPacket(buf, buf.length, address, port);
				rttMeasure.put("timestamp", Timestamp.df.format(new Date()));
				socket.send(ping);
				try {
					socket.setSoTimeout(maxDelay);
					DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
					//
					socket.receive(response);
					String[] content = (new String(response.getData())).split(" ");
					int sequencenumber;
					long T0;
					try {
						sequencenumber = Integer.parseInt(content[1]);
						T0 = Long.parseLong(content[2]);
					} catch ( NumberFormatException e ) {
						e.printStackTrace();
						return null;
					}
					//
					System.out.println("R: "+sequencenumber+" -"+content[2]+"-");
					long T1 = System.nanoTime();
					rttMeasure.put("rtt", (T1-T0)/1000000.0);
					rttMeasure.put("isReachable", true);
					System.out.println("ok");
				} catch (SocketTimeoutException e) {
					rttMeasure.put("rtt", null); // RTT msec
					rttMeasure.put("isReachable", false);
					System.out.println("Timeout for packet " + sequence_number);
				}
				value.put("value", rttMeasure);
				value.put("id", id);
				System.out.println(value);
				channel.println(value);
				if ( channel.checkError() ) {
					System.err.println("Sensor shutdown");
					break;
				}
				// next packet
				sequence_number ++;
				try {
					Thread.sleep(period);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
}