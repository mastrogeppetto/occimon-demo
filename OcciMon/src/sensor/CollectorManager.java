package sensor;

import java.rmi.Naming;
import java.util.concurrent.Callable;

import org.json.simple.JSONObject;

import systemSpecification.SystemSpecification;
import collector.MetricContainerIF;

public class CollectorManager implements Callable<Object> {
	
	int sensorPort;
	String sensorIP;
	String collectorId;
	String registry;

	public CollectorManager(int sensorPort, String sensorIP, String collectorId, String registry) {
		super();
		this.sensorPort= sensorPort;
		this.sensorIP = sensorIP;
		this.collectorId = collectorId;
		this.registry = registry;
	}

	@Override
	public Object call() throws Exception {		
		SystemSpecification x = new SystemSpecification(registry);
		// Load collector source id
		JSONObject collector= x.getById(collectorId);
		String source=(String) collector.get("source");
		// Load MetricContainer mixin attributes
		JSONObject attrs= x.getAttributesById(source);
		String MetricContainerHostname=(String) attrs.get("MetricContainerIPAddress");
		int MetricContainerPort=((Number) attrs.get("MetricContainerPort")).intValue();
		// The RMI endpoint of the collector container
		System.out.println("Sensor launching collector from "+
							MetricContainerHostname+":"+MetricContainerPort);
		MetricContainerIF MetricContainer = 
				(MetricContainerIF) Naming.lookup("rmi://"+
						MetricContainerHostname+":"+
						MetricContainerPort+"/MetricContainer");
		// Launch the collector metrics
		MetricContainer.launchMetricContainer(sensorIP, sensorPort, collector);
		return null;
	}
}
