package mixin;

import java.io.BufferedReader;
import java.io.IOException;

public class EmailAlarm extends SensorMixin {

	public void run() {
		String data;
		BufferedReader input = getReaderByName("input");
		
		try {
			while ( ( data=input.readLine() ) != null ) {
				System.out.println("emailalarm: "+data);
			}
		} catch (IOException e) {
			return;
		}
		return;
	}

}
