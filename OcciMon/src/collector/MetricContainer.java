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
	// Maximum number of retries when connecting to the callback socket on the sensor
	// TODO: To refine (this should be a parameter)
	static final int MAXRETRY = 10;
	// The id of this metric container
	private static String myId;
	// The URL of the registry containing the system specifications
	private static String registry;
	// ISO time-stamps generator
	static final Timestamp timestamp = new Timestamp();
	// The thread method
	protected MetricContainer() throws RemoteException {
		super();
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
		// Management of the arguments on the command line
		if ( args.length < 2) {
			System.out.println("Usage: MetricContainer <id> <registry>");
			System.exit(1);
		}
		myId=args[0];
		registry=args[1];
		System.out.println("Launch collector endpoint "+registry+myId);
		// Create a handle for system specifications management
		SystemSpecification x = new SystemSpecification(registry);
		// Load MetricContainer mixin attributes
		// Load local endpoint
		// TODO: this info should not be in a mixin, but in a configuration file");
		JSONObject attrs= x.getAttributesById(myId);
		String hostname=(String) attrs.get("MetricContainerIPAddress");
		int port=((Number) attrs.get("MetricContainerPort")).intValue();
		// Setup RMI service endpoint
		System.setProperty("java.rmi.server.hostname", hostname);
		Registry reg = LocateRegistry.createRegistry(port);
		// Start metric container
		reg.rebind("MetricContainer", new MetricContainer());
		System.out.println("Metric container is ready ("+hostname+":"+port+")"); 
	}

	// This method is exposed through RMI. It is used by the sensor to launch a
	// collector end-point on the metric container (aka probe)
	// Takes as arguments the transport address IP:port of the callback socket on
	// the sensor, and the description of the collector
	public String launchMetricContainer(String sensorIP, int sensorPort, JSONObject descr) 
			throws InstantiationException, IllegalAccessException, 
				ClassNotFoundException, InterruptedException, 
				IOException, ParseException {
		// Tries to open the callback socket to the sensor
		int retry=0;
		while ( retry < MAXRETRY ) {
			try ( Socket outSocket = new Socket(sensorIP, sensorPort) ) {
				// When the socket accepts the connection, calls the launchMetrics
				// launchMetrics returns when with a list of exitcodes from the
				// measurement threads
				// TODO: exit codes are not managed (report measure termination...)
				List<Future<Object>> exitcodes = launchMetrics(descr, outSocket);
				// here add exitcodes management
				System.out.println("Collector finished");
				return null;
			} catch (IOException e) {
				// If sensor socket not ready, retry...
				System.err.println("Sensor not ready - retrying");
				retry++;
				Thread.sleep(1000);
			}
		}
		// This is reached only if the sensor is not responding...
		System.err.println("Giving up...");
		return null;
	}
	// The launchMetrics takes as arguments the sensor callback socket and the JSON
	// description of a collector. It calls all measurement threads associated with
	// mixins, and waits for their termination 
	private static List<Future<Object>> launchMetrics(JSONObject descr, Socket outSocket)
			throws IOException, ParseException, InstantiationException,
			IllegalAccessException, InterruptedException {
		// This is the channel to the callback socket on the sensor (TODO: better
		// to allocate it in the launchMetricCollector?)
		PrintWriter channel = 
				new PrintWriter(outSocket.getOutputStream(), true);
		// Container for measurement threads associated with mixins
		ExecutorService meters = Executors.newCachedThreadPool();
		// Collection of callable objects that implement the measurements
		List<Callable<Object>> metrics = new ArrayList <Callable<Object>>();
		// Get the attributes of the collector from system specification
		JSONObject attrs = (JSONObject) descr.get("attributes");
		// Gets the attributes rooted in "occi.", that are the standard ones
		JSONObject collectorAttributes= 
				(JSONObject) ((JSONObject) attrs.get("occi")).get("collector");
		// TODO: this should process the standard collector attributes, now only the
		// mandatory "period" attribute
		int period = ((Number) collectorAttributes.get("period")).intValue()*1000;
		// Strip provider prefix from mixin attributes
		// TODO (make it parametric!!)
		JSONObject mixinAttributes= 
				(JSONObject) (
						(JSONObject) (
								(JSONObject) (
										(JSONObject) attrs.get("com")).get("example")).get("occi")).get("monitoring");
		// Scans and adds to the collection all the mixins
		for ( Object mx : mixinAttributes.keySet() ) {
			// Each key in the mx JSONOject corresponds to a mixin Id: convert to a String
			String metricName = (String) mx;
			// ... and instantiates a callable that implements the measurement
			try {
				// Create an object by reflection, using the metric name
				MetricMixin m = (MetricMixin) Class.forName("metric."+metricName).newInstance();
				System.out.println("Now collecting "+metricName);
				// Set the MetricMixin attributes using:
				// - the period attribute of the collector
				// - the specific attributes of the mixin
				// - the socket to send measurements to the sensor
				m.set(  collectorAttributes,
						(JSONObject) mixinAttributes.get(metricName),
						channel,
						period);
				// Add the new measurement callable to the collection
				metrics.add(m);
			} catch ( LinkageError | ClassNotFoundException e ) {
				// This is to trap problems in the instantiation of the measurement 
				System.out.println("Class "+metricName+" not found");
				break;
			}
		}
		// Launches all measurements and returns when all terminate
		return meters.invokeAll(metrics);
	}
	
	// Converts a java JSON Number into a port number (checking it falls within 
	// port interval)
	int JSONtoPort(Number x) {
		int xint=x.intValue();
		if ( ( xint != x.floatValue() ) || ( xint < 0 ) || ( xint > 65535 ) ) {
			throw new IllegalArgumentException(x + " error converting port number");
		}
		return x.intValue();
	}
}
