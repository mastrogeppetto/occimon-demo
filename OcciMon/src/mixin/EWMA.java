package mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class EWMA extends SensorMixin implements SensorMixinIF {

	public void run() {
		String data; 
		Float value=null;
		BufferedReader instream = getReaderByName("instream");
		Long gain = (Long) mat.get("gain");
		PrintWriter outstream = getWriterByName("outstream");
		
		try {
			while ( ( data=instream.readLine() ) != null ) {
				System.out.println("ewma input: "+data);
				if ( value == null ) {
					value = Float.valueOf(data);
				} else {
					value = ( Float.valueOf(data) + (gain * value) ) / (gain + 1);
				}
				outstream.println(value);
			}
		} catch (IOException e) {
			System.out.println("Pipe error");
			e.printStackTrace();
			return;
		}
		return;
	}

}
