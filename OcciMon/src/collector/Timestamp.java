package collector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
	
	public String get() {
		try {
		return df.format(new Date());
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}
	}

}
