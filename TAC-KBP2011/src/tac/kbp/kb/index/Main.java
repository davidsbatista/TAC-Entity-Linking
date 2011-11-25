package tac.kbp.kb.index;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.xml.sax.SAXException;

import tac.kbp.kb.index.xml.Entity;
import tac.kbp.utils.OnlyExt;

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
		String fileList[] = dir.list(only);
		
		if (fileList.length == 0) {
			System.out.println("No XML files found");
			System.exit(0);
		}
		else {
			
			System.out.println(fileList.length + " files loaded");
			
			//creates a Lucene index
			IndexWriter indexDir = Index.createIndex(args[1]);
			
			EntityParser parser = new EntityParser();
			
			Arrays.sort(fileList);
			
			//starts indexing the parsed entities
			for (int i=0; i < fileList.length; i++) { 
				System.out.print("\nProcessing " + fileList[i]);
				for (Entity entity : parser.process(args[0]+fileList[i]).getEntities()) {
					Index.indexEntities(indexDir, entity);
				}
			}

			indexDir.close();
			
			long end = new Date().getTime();		
			float secs = (end - start) / 1000;
			
			System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
			
		}
		
	} 			
}

