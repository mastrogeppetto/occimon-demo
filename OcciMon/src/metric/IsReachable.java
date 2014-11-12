package metric;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import org.json.simple.JSONObject;

import collector.Timestamp;

public class IsReachable implements MetricMixin {
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

	@SuppressWarnings("unchecked")
	@Override
	public Object call() {
		InetAddress target;
		Boolean resp;

		String id = (String) mixinAttributes.get("out");
		String hostname = (String) mixinAttributes.get("hostname");
		int maxDelay = ((Number) mixinAttributes.get("maxdelay")).intValue();
		try {
			while (true) {
				Thread.sleep(period);
				try {
					target = InetAddress.getByName(hostname);
					resp = target.isReachable(maxDelay);
				} catch ( IOException e1) {
					resp = false;
				} 
				JSONObject j = new JSONObject();
				j.put("id",id);
				j.put("value",resp);
				channel.println(j);
				if ( channel.checkError() ) {
					System.err.println("Sensor shutdown");
					break;
				}
			} 
		} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		return null;
		}
	}