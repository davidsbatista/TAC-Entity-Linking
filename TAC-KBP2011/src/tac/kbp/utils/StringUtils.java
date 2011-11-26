package tac.kbp.utils;

public class StringUtils {
	
	public static boolean isUpper(String s) {
		
		for(char c : s.toCharArray()) {
			if(!Character.isUpperCase(c))
	            return false;
	    }
	   return true;
	}
}
