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
		
		Query q = new Query(args[0],args[1]);
				
		long start = new Date().getTime();
		
		System.out.println("Loading: " + args[0]);
		System.out.println("Loading: " + args[1]);
		System.out.println("Querying for " + args[2]);
		
		q.BM25(args[2]);
		q.BM25F(args[2]);
		
		long end = new Date().getTime();		
		float secs = (end - start) / 1000;	
		
		System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
		
		}		
}

