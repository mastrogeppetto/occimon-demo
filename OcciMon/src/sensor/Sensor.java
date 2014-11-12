package sensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mixin.SensorMixinIF;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import systemSpecification.SystemSpecification;

public class Sensor implements Callable<Object> {

	String sensorId;
	String registryURL;
	int sensorPort;
	String sensorIP;

	public Sensor( String sensorId, String registryURL) {
		this.sensorId = sensorId;
		this.registryURL = registryURL;
	}

	public class InputManager implements Runnable{

		protected Socket clientSocket = null;
		protected HashMap<String, PrintWriter> writerMap   = null;

		public InputManager(
				Socket clientSocket, 
				HashMap<String, PrintWriter> writerMap) {
			this.clientSocket = clientSocket;
			this.writerMap = writerMap;
		}

		public void run() {
			JSONParser parser = new JSONParser();
			System.out.println("New collector connected");
			try ( BufferedReader in = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream())) ) {
				while (true) {
					String record = in.readLine();
					JSONObject data = (JSONObject) parser.parse(record);
//					System.out.println(data);
					String id = (String) data.get("id");
					writerMap.get(id).println(data.get("value"));
				} 
			} catch (IOException | ParseException e) {
				//report exception somewhere.
				e.printStackTrace();
				return;
			}
		}
	}

	public Object call() 
			throws Exception {
		//		sensorId="urn:uuid:1111";
		System.out.println("Launching sensor "+sensorId);
		SystemSpecification sysSpec = new SystemSpecification(registryURL);
		JSONObject sensorAttrs = sysSpec.getAttributesById(sensorId);
		// The id of the scope is that of the sensor
		String scopeId=sensorId;
		List<String> collectors = sysSpec.getLinksByTarget(scopeId);
		ExecutorService collectorsPool = Executors.newCachedThreadPool();
		ExecutorService mixinsPool = Executors.newCachedThreadPool();
		ExecutorService inputPool = Executors.newCachedThreadPool();
		// Create socket for input measurements
		try ( ServerSocket sensorSocket = new ServerSocket(0) ) {
			sensorPort=sensorSocket.getLocalPort();
			String sensorIF = (String) sensorAttrs.get("networkInterface");
			NetworkInterface nic = NetworkInterface.getByName(sensorIF);
			Enumeration<InetAddress> l = nic.getInetAddresses();
			while ( l.hasMoreElements()) {
				InetAddress a = l.nextElement();
				if ( Inet4Address.class == a.getClass() ) {
					sensorIP=a.toString().split("/")[1];
					break;
				}
			}
			System.out.println("Sensor now receiving from TCP socket "+sensorIP+":"+sensorPort);
			// Launch remote collectors
			System.out.println("Launching remote collectors: "+collectors);
			for ( String collectorId: collectors ) {
				if ( sysSpec.getKindById(collectorId).equals("collector") ) {
					collectorsPool.submit(
							new CollectorManager(sensorPort,sensorIP,collectorId,registryURL));
				}
			}
			// Create input pipes for mixins
			HashMap<String, PipedInputStream> pipeInMap = 
					sysSpec.inputPipes("http://example.com/occi/monitoring",sensorId);
			//		System.out.println(pipeInMap);
			// Create output pipes for mixins
			HashMap<String, PrintWriter> writerMap = 
					sysSpec.outputPipes("http://example.com/occi/monitoring",sensorId,pipeInMap);
			// Compute mixins
			HashMap<String,JSONObject> mixinDescriptions = 
					sysSpec.getMixinsById("http://example.com/occi/monitoring", sensorId);
			List<SensorMixinIF> mixins = new ArrayList <>();
			// Populate list of mixins to launch
			for ( String mixinId: mixinDescriptions.keySet() ) {
				SensorMixinIF m = (SensorMixinIF) Class.forName("mixin."+mixinId).newInstance();
				m.set(  sensorAttrs,
						mixinDescriptions.get(mixinId),
						pipeInMap,
						writerMap);
				mixins.add(m);
			}
			for ( SensorMixinIF r: mixins ) {
				mixinsPool.submit(r);
			}

			// Start receiving from the collector port
			// TODO: verify thread safe since this allows multiple collectors!
			while ( true ) {
				try {
					Socket clientSocket = sensorSocket.accept();
					inputPool.submit(
							new InputManager(clientSocket, writerMap));
				} catch ( IOException e) {
					System.out.println("Exception caught");
					e.printStackTrace();
				}
			}
		}
	}
}
