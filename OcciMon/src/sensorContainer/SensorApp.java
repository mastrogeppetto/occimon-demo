package sensorContainer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sensor.Sensor;

public class SensorApp {
	public static void main(String[] args) {
		ExecutorService sensors = Executors.newCachedThreadPool();
		if ( args.length < 2) {
			System.out.println("Usage: Demo <id> <registry>");
			System.exit(1);
		}
		Sensor demo = new Sensor(args[0], args[1]);
		sensors.submit(demo);
	}
}
