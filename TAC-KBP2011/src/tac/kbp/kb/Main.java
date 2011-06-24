package tac.kbp.kb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.xml.sax.SAXException;

import tac.kbp.kb.EntityParser.Entity;
import tac.kbp.kb.utils.CollectionSimilarityIndexer;
import tac.kbp.kb.utils.OnlyExt;

/**
 * @author dsbatista
 *
 */

public class Main {
	
	public static void main(String[] args) throws IOException, SAXException, ParseException{
				
		long start = new Date().getTime();
		
		//loads Knowledge Base files
		System.out.println("looking for files in " + args[0]);
		System.out.println("Lucene index will be written to: " + args[1]);
		
		File dir = new File(args[0]);
		FilenameFilter only = new OnlyExt("xml"); 
		String s[] = dir.list(only);
		
		if (s.length == 0) {
			System.out.println("No XML files found");
			System.exit(0);
		}
		else {
			
			System.out.println(s.length + " files loaded");
			
			//creates a Lucene index
			IndexWriter indexDir = Index.createIndex(args[1]);
			
			//starts indexing the parsed entities
			for (int i=0; i < s.length; i++) { 
				System.out.println("\nProcessing " + s[i]);
				Vector<Entity> entities = EntityParser.process(args[0]+s[i]);
				
				for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext();) {
					Entity entity = (Entity) iterator.next();
					Index.indexEntities(indexDir, entity);
				}
			}
			
			// caculates each field average lenght, to be used in BM25
			CollectionSimilarityIndexer similarity = (CollectionSimilarityIndexer) indexDir.getSimilarity();
					
			Float numDocs = new Float(indexDir.numDocs());
			
			Float wiki_title_length = similarity.getLength("wiki_title") / numDocs; 
			Float type_length = similarity.getLength("type") / numDocs;
			Float id_length = similarity.getLength("id") / numDocs;;
			Float name_length = similarity.getLength("name") / numDocs;;
			Float infobox_length = similarity.getLength("infobox") / numDocs;;
			Float wiki_text_length = similarity.getLength("wiki_text") / numDocs;;
			Float facts_length = similarity.getLength("facts") / numDocs;;
			
			// write everything to file to be loaded at query time
			try{
				  // Create file 
				  FileWriter fstream = new FileWriter("fields_avg_size.txt");
				  BufferedWriter out = new BufferedWriter(fstream);
	
				  out.write("wiki_title\n");
				  out.write(Float.toString(wiki_title_length)+"\n");
				  
				  out.write("type\n");
				  out.write(Float.toString(type_length)+"\n");
				  
				  out.write("id\n");
				  out.write(Float.toString(id_length)+"\n");
				  
				  out.write("name\n");
				  out.write(Float.toString(name_length)+"\n");
				  
				  out.write("infobox\n");
				  out.write(Float.toString(infobox_length)+"\n");
				  
				  out.write("wiki_text\n");
				  out.write(Float.toString(wiki_text_length)+"\n");
				  
				  out.write("facts\n");
				  out.write(Float.toString(facts_length)+"\n");
				  
				  //Close the output stream
				  out.close();
				  
			}	catch (Exception e) {
					//Catch exception if any
					System.err.println("Error: " + e.getMessage());
				}
			
			indexDir.close();
			
			long end = new Date().getTime();		
			float secs = (end - start) / 1000;
			
			System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
			
		}
		
	} 			
}


