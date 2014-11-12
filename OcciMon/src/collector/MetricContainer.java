package collector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import metric.MetricMixin;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import systemSpecification.SystemSpecification;;

public class MetricContainer  extends UnicastRemoteObject implements MetricContainerIF {
	private static final long serialVersionUID = 1L;
	static final int MAXRETRY = 10;
	// To refine (this should be a parameter)
	private static String myId;
	private static String registry;
	static Timestamp timestamp;

	protected MetricContainer() throws RemoteException {
		super();
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
		timestamp = new Timestamp();
		if ( args.length < 2) {
			System.out.println("Usage: MetricContainer <id> <registry>");
			System.exit(1);
		}
		myId=args[0];
		registry=args[1];
		System.out.println("Launch collector endpoint "+registry+myId);
		SystemSpecification x = new SystemSpecification(registry);
		// Load MetricContainer mixin attributes
		JSONObject attrs= x.getAttributesById(myId);
		String hostname=(String) attrs.get("MetricContainerIPAddress");
		int port=((Number) attrs.get("MetricContainerPort")).intValue();
		// Setup RMI service (to refine)
		System.setProperty("java.rmi.server.hostname", hostname);
		Registry reg = LocateRegistry.createRegistry(port);
		// Start metric container
		reg.rebind("MetricContainer", new MetricContainer());
		System.out.println("Metric container is ready ("+hostname+":"+port+")"); 
	}
	
	public String launchMetricContainer(String sensorIP,
			int sensorPort, JSONObject descr) 
			throws InstantiationException, IllegalAccessException, 
				ClassNotFoundException, InterruptedException, 
				IOException, ParseException {
		int retry=0;
//		SystemSpecification x = new SystemSpecification(registry);
		// Load MetricContainer mixin attributes
//		JSONObject sensor= x.getAttributesById(sensorId);
//		String hostName=(String) sensor.get("IPAddress"); 
//		int portNumber=((Number) sensor.get("port")).intValue();
		
		while ( retry < MAXRETRY ) {
			try ( Socket outSocket = new Socket(sensorIP, sensorPort) ) {
				List<Future<Object>> exitcodes = launchMetrics(descr, outSocket, timestamp);
				System.out.println("Collector finished");
				return null;
			} catch (IOException e) {
				System.err.println("Sensor not ready - retrying");
				retry++;
				Thread.sleep(1000);
			}
		}
		System.err.println("Exiting");
		return null;
	}
	
	private static List<Future<Object>> launchMetrics(JSONObject descr, Socket outSocket, Timestamp timestamp2)
			throws IOException, ParseException, InstantiationException,
			IllegalAccessException, InterruptedException {
		PrintWriter channel = 
				new PrintWriter(outSocket.getOutputStream(), true);
		// Container for metric mixin threads
		ExecutorService meters = Executors.newCachedThreadPool();
		// Collection of metric mixins
		List<Callable<Object>> metrics = new ArrayList <Callable<Object>>();
		// Estrae gli attributi...
		JSONObject attrs = (JSONObject) descr.get("attributes");
		// ...del collettore (temporizzazione)...
		JSONObject collectorAttributes= 
				(JSONObject) ((JSONObject) attrs.get("occi")).get("collector");
		// da perfezionare...
		int period = ((Number) collectorAttributes.get("period")).intValue()*1000;
		// ...e quelli dei mixin
		JSONObject mixinAttributes= 
				(JSONObject) (
						(JSONObject) (
								(JSONObject) (
										(JSONObject) attrs.get("com")).get("example")).get("occi")).get("monitoring");
		// Scans and adds to the collection all the mixins
		for ( Object mx : mixinAttributes.keySet() ) {
			String metricName = (String) mx;
			// istanziazione del mixin
			try {
				MetricMixin m = (MetricMixin) Class.forName("metric."+metricName).newInstance();
				System.out.println("Now collecting "+metricName);
				m.set(  collectorAttributes,
						(JSONObject) mixinAttributes.get(metricName),
						channel,
						period);
				metrics.add(m);
			} catch ( LinkageError | 
						ClassNotFoundException e ) {
				System.out.println("Class "+metricName+" not found");
				break;
			}
		}
		return meters.invokeAll(metrics);
	}
	
	int JSONtoPort(Number x) {
		int xint=x.intValue();
		if ( ( xint != x.floatValue() ) || ( xint < 0 ) || ( xint > 65535 ) ) {
			throw new IllegalArgumentException(x + " error converting port number");
		}
		return x.intValue();
	}
}
