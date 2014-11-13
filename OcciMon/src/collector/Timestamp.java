package collector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

// Useful since DateFormat is not thread-safe
// Usage: 
// instantiate a static final Timestamp in the application
//    static final Timestamp timestamp =new Timestamp();
// Use the generate() method to obtain a timestamp:
//    String ts = timestamp.generate(),

public class Timestamp {
	
	static DateFormat df;
	
	Timestamp () {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		if ( df == null ) {
			System.out.println("Problem with timestamp");
		}
		df.setTimeZone(tz);
		System.out.println("Timestamping is configured");
	}
	
	public static String generate() {
		return df.format(new Date());
	}
}
