/**
 * 
 */
package tac.kbp.utils.misc;

import java.text.DecimalFormat;

/**
 * @author dsbatista
 *
 */

public class TimeFormat {

	
	private static final String	sm_sUnknownText = "00:00:00";

	private static final DecimalFormat	m_oneDigitFormat = new DecimalFormat("####0");
	private static final DecimalFormat	m_twoDigitFormat = new DecimalFormat("###00");
	//private static final DecimalFormat	m_threeDigitFormat = new DecimalFormat("##000");


	public static String toString(long secs) {
		if (secs >= 0) {
			
			long lMinutes = secs / 60;
			secs %= 60;
			
			long lHours = lMinutes / 60;
			lMinutes %= 60;
			
			return 	m_oneDigitFormat.format(lHours) + ":" + 
					m_twoDigitFormat.format(lMinutes) + ":" + 
					m_twoDigitFormat.format(secs) + "."; //m_threeDigitFormat.format(lMilliSeconds);
		}
		
		else {	
			return sm_sUnknownText;
		}
	}
}