package collector;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public interface MetricContainerIF extends Remote {
	public String launchMetricContainer (String sensorIP, int sensorPort, JSONObject descr) throws RemoteException, InstantiationException, IllegalAccessException, ClassNotFoundException, InterruptedException, IOException, ParseException;
}
