package tac.kbp.utils.string;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import tac.kbp.entitylinking.bin.Definitions;


public class StringUtils {
	
	public static boolean isUpper(String s) {
		
		for(char c : s.toCharArray()) {
			if(!Character.isUpperCase(c))
	            return false;
	    }
	   return true;
	}
	
	public static Map<String, Integer> tokenized(String s) throws IOException {
    	
    	WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
    	
    	Map<String, Integer> result = new HashMap<String, Integer>();
    	TokenStream stream  = analyzer.tokenStream(null, new StringReader(s));
    	//OffsetAttribute offsetAttribute = stream.getAttribute(OffsetAttribute.class);
		TermAttribute termAttribute = stream.getAttribute(TermAttribute.class);
    	String term = new String();
		
		while (stream.incrementToken()) {
    		//int startOffset = offsetAttribute.startOffset();
    		//int endOffset = offsetAttribute.endOffset();
    		term = termAttribute.term();
    		
    		// don't include stopwords
    		if (Definitions.stop_words.contains(term)) {
    			continue;
			}
    		try {
    			int value = result.get(term);
    			result.put(term, value + 1);
    		} catch (NullPointerException e) {
    			result.put(term, 1);
    		}
		}
		return result;
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
