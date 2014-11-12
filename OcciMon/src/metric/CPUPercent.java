package metric;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import org.json.simple.JSONObject;

import collector.Timestamp;

public class CPUPercent implements MetricMixin {
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
		// Funzionalità di comunicazione
		String id = (String) mixinAttributes.get("out");
		try {
			while (true) {
				Thread.sleep(period);
				double x = 100*ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
//				System.out.println(x);
				JSONObject j = new JSONObject();
				j.put("id",id);
				j.put("value",x);
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