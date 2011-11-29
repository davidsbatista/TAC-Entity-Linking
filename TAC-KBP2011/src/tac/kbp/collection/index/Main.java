package tac.kbp.collection.index;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.lucene.index.IndexWriter;

import tac.kbp.utils.misc.BigFile;

/**
 * @author dsbatista
 *
 */

public class Main {
	
	public static HashMap<String, String> docslocations = new HashMap<String, String>();

	public static void loadDocsLocations(String filename) throws Exception {
		
		BigFile file = new BigFile(filename);
		String[] parts;
		
		for (String line : file) {		
			parts = line.split(".sgm");			
			docslocations.put(parts[0], parts[1]);
		}
		
	}
	
	public static void main(String[] args) throws Exception{
				
		long start = new Date().getTime();

		//loads document collections files location
		System.out.println("loading document collection files from: " + args[0]);
		loadDocsLocations(args[0]);
		System.out.println(docslocations.size() + " files loaded");
		
		System.out.println("Lucene index will be written to: " + args[1]);
			
		//creates a Lucene index
		IndexWriter indexDir = Index.createIndex(args[1]);
						
		//starts indexing the documents
		Set<String> keys = docslocations.keySet();
		
		for (String file : keys) {
			System.out.print("\nProcessing " + file);
			Article a = new Article(file);
			Index.indexEntities(indexDir,a);
			}
		
		indexDir.close();
			
		long end = new Date().getTime();		
		float secs = (end - start) / 1000;
			
		System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
	} 			
}
