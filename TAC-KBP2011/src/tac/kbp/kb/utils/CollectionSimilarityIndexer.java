package tac.kbp.kb.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.DefaultSimilarity;

/**
 * @author dsbatista
 *
 */

public class CollectionSimilarityIndexer extends DefaultSimilarity {

	private static final long serialVersionUID = -2212275281313452563L;
	
	private static Map< String,Long> length = new HashMap<String, Long>();

	  @Override
	  public float lengthNorm(String fieldName, int numTokens) {
		  Long aux = CollectionSimilarityIndexer.length.get(fieldName);
		  
		  if (aux==null)
			  aux = new Long(0);
		  
		  aux+=numTokens;
		  CollectionSimilarityIndexer.length.put(fieldName,aux);
		  
		  return super.lengthNorm(fieldName, numTokens);
	  }
	  
	  public static long getLength(String field){
	    return CollectionSimilarityIndexer.length.get(field);
	  }	  
}
