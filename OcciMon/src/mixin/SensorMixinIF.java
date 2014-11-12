package mixin;

import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.json.simple.JSONObject;

public interface SensorMixinIF extends Runnable {

	void set(JSONObject sat,
			JSONObject mat,
			HashMap<String, PipedInputStream> pipeInMap,
			HashMap<String, PrintWriter> writerMap);
	
}
