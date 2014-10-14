package mixin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.json.simple.JSONObject;

public class SensorMixin implements SensorMixinIF {

	JSONObject sat;
	protected JSONObject mat;
	HashMap<String, PipedInputStream> pipeInMap;
	HashMap<String, PrintWriter> writerOutMap;

	public SensorMixin() {
		super();
	}
	
	public void set(
			JSONObject sat, 
			JSONObject mat, 
			HashMap<String, PipedInputStream> pipeInMap,
			HashMap<String, PrintWriter> writerMap) {
		this.sat=sat;
		this.mat=mat;
		this.pipeInMap=pipeInMap;
		this.writerOutMap=writerMap;
	}
	protected BufferedReader getReaderByName(String name) {
		String id = (String) mat.get(name);
		PipedInputStream pipe=pipeInMap.get(id);
		return new BufferedReader(
				new InputStreamReader(
				new BufferedInputStream(pipe)));
	}
	
	protected PrintWriter getWriterByName(String name) {
		String id = (String) mat.get(name);
		return writerOutMap.get(id);
	}

	@Override
	public void run() {
	}

}