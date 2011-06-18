/**
 * @author dsbatista
 * 
 */

package tac.kbp.kb;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;

import tac.kbp.kb.EntityParser.Entity;
import tac.kbp.kb.EntityParser.Fact;
import tac.kbp.kb.EntityParser.FactLink;


public class Index {

	public static IndexWriter createIndex() throws CorruptIndexException, LockObtainFailedException, IOException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		
		//Directory dir = new RAMDirectory();
		Directory dir = new SimpleFSDirectory(new File("/tmp/KB"));
		
		IndexWriter indexDir = new IndexWriter(dir, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
		
		return indexDir;
	}

	
	public static void indexEntities(IndexWriter index, Entity entity) throws CorruptIndexException, IOException{
			
		Document doc = new Document();
		
		//wiki_title
		doc.add(new Field("wiki_title", entity.getWiki_title(), Field.Store.YES, Field.Index.NOT_ANALYZED));

		//type
		doc.add(new Field("type", entity.getType(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//id
		doc.add(new Field("id", entity.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//name
		doc.add(new Field("name", entity.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//infobox_class
		doc.add(new Field("infobox", entity.getInfobox_class(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//wiki_text
		doc.add(new Field("wiki_text", entity.getWiki_text(), Field.Store.YES, Field.Index.ANALYZED));
		
		//facts		
		StringBuilder facts = new StringBuilder();
		
		for (Iterator<Fact> iterator = entity.getFacts().iterator(); iterator.hasNext();) {
			
			Fact fact = (Fact) iterator.next();

			if (fact.fact.length()>0) {
				facts.append(fact.name+"_FACT_"+fact.fact+"\n");
			}

			if (fact.factlink.size()>0) {
				for (Iterator<FactLink> iterator2 = fact.factlink.iterator(); iterator2.hasNext();) {
					FactLink factlink = (FactLink) iterator2.next();
						if (factlink.e_id!=null) {
							facts.append(fact.name+"_EID_"+factlink.e_id+"_LINK_"+factlink.link+"\n");
						}
						else {
							facts.append(fact.name+"_FACT_"+factlink.link+"\n");
						}
				}
			}
		}
		
		
		String factsString = new String(facts); 		
		doc.add(new Field("facts", factsString, Field.Store.YES, Field.Index.NOT_ANALYZED));
				
		index.addDocument(doc);
	}
}
