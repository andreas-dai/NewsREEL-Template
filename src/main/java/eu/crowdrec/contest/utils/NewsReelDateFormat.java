package eu.crowdrec.contest.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * In the CLEF-NewsREEL challenge we have to cope with several different date formats.
 * This may results in confusion since code working well in one scenario, throws exceptions 
 * in another scenario. A well known problem is the small differences between the date formats
 * used in the online and the offline version of the challenge.
 * 
 *  This class provides a parser that should support all date formats used until now.
 *  The key points are
 *  <ul>
 *  <li>Time and date can be separated by either space or T. We allow any char.
 *  <li>The time can contain milliseconds and time-zone. This parser ignores this information and only considers hours, minutes, and seconds.
 *  </ul>
 * 
 *  The implementation should be thread-safe.
 * 
 * @author andreas
 *
 */
public class NewsReelDateFormat extends SimpleDateFormat {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * define the NewsREEL data format.
	 */
	private static final Pattern pattern = Pattern.compile("([0-9]{4})\\-([0-9]{1,2})\\-([0-9]{1,2}).([0-9]{1,2})\\:([0-9]{1,2})\\:([0-9]{1,2})(.*)");
	
	
	public Date parse(String dateString) {
		
		Matcher m = pattern.matcher(dateString);
		if (m.find( )) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.set(Calendar.YEAR, Integer.valueOf(m.group(1)));
			cal.set(Calendar.MONTH, Integer.valueOf(m.group(2))-1);
			cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(m.group(3)));
			cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(m.group(4)));
			cal.set(Calendar.MINUTE, Integer.valueOf(m.group(5)));
			cal.set(Calendar.SECOND, Integer.valueOf(m.group(6)));
			
			return new Date(cal.getTimeInMillis());

		} else {
			throw new IllegalArgumentException("invalid dateString: " + dateString);
		}
	}
	
	/**
	 * The main method was used for testing and debugging different input strings.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		//new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ssZ");
		//new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		String[] exampleDateStrings = {
				"2013-10-20T00:00:00+0200",
				"2016-05-01 00:00:00,008",
				"2016-00-01 00:00:00,016",
				"2016-01-01 00:00:00,016",
				"2016-12-01 00:00:00,016",
				"2016-13-01 00:00:00,016",
		};
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		NewsReelDateFormat ndf = new NewsReelDateFormat();
		
		for (String dateString : exampleDateStrings) {
			
			try {
				Date d = ndf.parse(dateString);
				System.out.println("match + \t" + dateString + "  cal=" + d);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//Date d = sdf.parse(dateString);
			//System.out.println("d + \t" + dateString);
		}
	}
}
