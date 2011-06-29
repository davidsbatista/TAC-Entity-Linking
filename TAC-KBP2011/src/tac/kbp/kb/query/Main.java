package tac.kbp.kb.query;

import java.io.IOException;
import java.util.Date;

import org.apache.lucene.queryParser.ParseException;

/**
 * @author dsbatista
 *
 */

public class Main {
	
	public static void main(String[] args) throws IOException, ParseException{
		
		long start = new Date().getTime();
		
		TuneBM25 tune = new TuneBM25(args[0], args[1], args[2]);
		
		long end = new Date().getTime();		
		float secs = (end - start) / 1000;	
		
		System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
		
		}		
}

