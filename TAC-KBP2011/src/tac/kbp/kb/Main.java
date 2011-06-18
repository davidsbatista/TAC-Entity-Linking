package tac.kbp.kb;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.apache.lucene.index.IndexWriter;
import org.xml.sax.SAXException;

import tac.kbp.kb.EntityParser.Entity;
import tac.kbp.kb.utils.OnlyExt;

/**
 * @author dsbatista
 *
 */

public class Main {
	
	public static void main(String[] args) throws IOException, SAXException{
		
		//loads Knowledge Base files
		File dir = new File(args[0]);
		FilenameFilter only = new OnlyExt("xml"); 
		String s[] = dir.list(only);
		System.out.println(s.length + " files loaded");
		
		//creates a Lucene index
		IndexWriter indexDir = Index.createIndex();
		
		//starts indexing the parsed entities
		for (int i=0; i < s.length; i++) { 
			System.out.println("Processing " + s[i]);
			Vector<Entity> entities = EntityParser.process(args[0]+s[i]);
			
			for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext();) {
				Entity entity = (Entity) iterator.next();
				Index.indexEntities(indexDir, entity);
			}
		} 
	} 			
}


