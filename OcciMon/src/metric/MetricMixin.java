package metric;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

import org.json.simple.JSONObject;

import collector.Timestamp;

public interface MetricMixin extends Callable<Object> {
	public void set(JSONObject cat, JSONObject mat, PrintWriter channel, int period);
}
