package collector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

// Useful since DateFormat is not thread safe

public class Timestamp {
	
	public static DateFormat df;
	
	Timestamp () {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		if ( df == null ) {
			System.out.println("Error in timestamp initialization");
		}
		df.setTimeZone(tz);
		System.out.println("Timestamping has been configured");
	}
}
