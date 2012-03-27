package tac.kbp.collection.index;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

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
	
	public static IndexWriter createIndex(String index) throws IOException {
		
		System.out.println("Writing to index: " + index);
		
		IndexWriterConfig indexCfg = new IndexWriterConfig(Version.LUCENE_35, new WhitespaceAnalyzer(Version.LUCENE_35));
		Directory indexDirectory = FSDirectory.open(new File(index));
		return new IndexWriter(indexDirectory,indexCfg);
	}
	
	public static void main(String[] args) throws Exception{
				
		long start = new Date().getTime();

		//loads document collections files location
		System.out.println("loading document collection files from: " + args[0]);
		loadDocsLocations(args[0]);
		System.out.println(docslocations.size() + " files loaded");
		
		System.out.println("Lucene index will be written to: " + args[1]);
			
		//creates a Lucene index
		IndexWriter indexW = createIndex(args[1]);
						
		//starts indexing the documents
		Set<String> keys = docslocations.keySet();
		
		for (String file : keys) {
			System.out.print("\nProcessing " + file);
			Article a = new Article(file);
			indexW.addDocument(a.luceneDoc());
			}
		
		indexW.commit();
		indexW.close();
			
		long end = new Date().getTime();		
		float secs = (end - start) / 1000;
			
		System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
	} 			
}
