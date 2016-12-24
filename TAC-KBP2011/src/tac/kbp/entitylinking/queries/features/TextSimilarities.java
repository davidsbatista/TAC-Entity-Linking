package tac.kbp.entitylinking.queries.features;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;

import tac.kbp.configuration.Definitions;

public final class TextSimilarities {

    public Integer numDocs;
    private IndexReader reader;
    private static Map<String, Integer> docFreqCache;
    
    /** Singleton. **/
    public static final TextSimilarities INSTANCE = new TextSimilarities();
    
    private TextSimilarities() {
        
        try {       	
        	reader = Definitions.searcher.getIndexReader();
            numDocs = reader.maxDoc();            
            docFreqCache = new HashMap<String, Integer>();
            
            TermEnum e = reader.terms( );
            
            while( e.next() ) {
                docFreqCache.put(e.term().text(), e.docFreq());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inverse Document Frequency, as computed in Lucene.
     * 
     * @param t The term.
     * @return The idf score.
     */
    public double idf(String t) {
        return 1.0 + Math.log(numDocs / (df(t) + 1.0));
    }
    
    public int df(String t) {
        try {
            Integer df = docFreqCache.get(t);
            return df == null ? 0 : df;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return 0;
    }
  
    public Map<String, Integer> tokenized(String s) throws IOException {
    	
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
    		try {
    			int value = result.get(term);
    			result.put(term, value + 1);
    		} catch (NullPointerException e) {
    			result.put(term, 1);
    		}
		}
		return result;
	}
    
    /**
     * 
     * Get the vector space model score, according to the cosine similarity and tf-idf weights.
     * Texts are pre-processed with Lucene's StandardAnalyzer.
     * 
     * @param q Documtent's text content.
     * @param d Documtent's text content.
     * @return The similarity value (>= 0.0).
     * @throws IOException 
     */
    public Double getSimilarity(String q, String d) throws IOException {

        if (q == null || d == null || q.length() == 0 || d.length() == 0) return 0.0;
        
        Double sim = 0.0;
        Map<String, Integer> wordsQ = tokenized(q);
        Map<String, Integer> wordsD = tokenized(d);
        
        Double normQ = 0.0;
        Double normD = 0.0;
        
        for (Map.Entry<String, Integer> e : wordsQ.entrySet()) {
            String t = e.getKey();
            Integer tfd = wordsD.get(t);
            Double idf = idf(t);
            
            if (tfd != null) {
                sim += e.getValue()*tfd*Math.pow(idf,2);
                normD += Math.pow(tfd*idf,2);
            }
            
            normQ += Math.pow(e.getValue()*idf, 2);
        }
        
        for (Map.Entry<String, Integer> e : wordsD.entrySet()) {            
            if (!wordsQ.containsKey(e.getKey())) {
                normD += Math.pow(e.getValue()*idf(e.getKey()),2);
            }            
        }
        
        sim = sim / (Math.sqrt(normQ)*Math.sqrt(normD));
        
        return sim.isNaN() ? 0.0 : sim;
    }
}
