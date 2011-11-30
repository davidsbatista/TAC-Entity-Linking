package tac.kbp.utils.string;

public class StringUtils {
	
	public static boolean isUpper(String s) {
		
		for(char c : s.toCharArray()) {
			if(!Character.isUpperCase(c))
	            return false;
	    }
	   return true;
	}
	
	public static String removeTags(String text) { 
		return text.replaceAll("\\<.*?\\>", "");
	}
		
	public static String cleanString(String sense) {
				
		String cleaned =  sense.replaceAll("\\[u'","").replaceAll("'\\]", "").replaceAll("u'", "").replaceAll("\\[","").
		replaceAll("'","").replaceAll("\\['", "").replaceAll("_", " ").replaceAll("\\(disambiguation\\)","").
		replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\"", "").replaceAll("#", "").replaceAll("\\|", "").trim();
		
		return cleaned;
	}
}
