package tac.kbp.utils.string;

public class StringUtils {
	
	public static boolean isUpper(String s) {
		
		for(char c : s.toCharArray()) {
			if(!Character.isUpperCase(c))
	            return false;
	    }
	   return true;
	}
	
	public static String cleanString(String sense) {
		
		/*
		'Du Wei'
		'Du Wei(footballer)']
		[u'Du_wei'
		*/
		
		String cleaned =  sense.replace("[u'","").replace("']", "").replace("u'", "").replace("[","").
				replace("'","").replace("['", "").trim().replace("_", " ");
		
		return cleaned;
	}
}
