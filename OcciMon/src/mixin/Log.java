package mixin;

import java.io.BufferedReader;
import java.io.IOException;

public class Log extends SensorMixin {
	
	public void run() {
		String data;
		BufferedReader in_msg = getReaderByName("in_msg");
		try {
			while ( ( data=in_msg.readLine() ) != null ) {
				System.out.println("Logging "+data+" to "+mat.get("filename"));
			}
		} catch (IOException e) {
			System.out.println("Pipe error");
			e.printStackTrace();
			return;
		}
		return;
	}
}
