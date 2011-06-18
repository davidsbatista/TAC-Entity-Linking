/**
 * @author dsbatista
 * 
 */

package tac.kbp.kb;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import tac.kbp.kb.EntityParser.Entity;
import tac.kbp.kb.EntityParser.Fact;
import tac.kbp.kb.EntityParser.FactLink;


public class Index {

	public static IndexWriter createIndex() throws CorruptIndexException, LockObtainFailedException, IOException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_32);
		IndexWriterConfig indexconfig = new IndexWriterConfig(Version.LUCENE_32, analyzer);
		Directory dir = new RAMDirectory();
		
		IndexWriter indexDir = new IndexWriter(dir, indexconfig);
		
		return indexDir;
	}

	
	public static void indexEntities(IndexWriter index, Entity entity){
			
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
		doc.add(new Field("infobox", entity.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
				
		//wiki_text
		doc.add(new Field("infobox", entity.getName(), Field.Store.YES, Field.Index.ANALYZED));
		
		//facts		
		StringBuilder facts = new StringBuilder();
		
		for (Iterator<Fact> iterator = entity.getFacts().iterator(); iterator.hasNext();) {
			
			Fact fact = (Fact) iterator.next();
			
			System.out.println(fact.fact+":"+fact.fact.length());
						
			if (fact.fact!=null) {
				facts.append(fact.name+" "+fact.fact+"\n");
			}

			if (fact.factlink.size()>0) {
				for (Iterator<FactLink> iterator2 = fact.factlink.iterator(); iterator2.hasNext();) {
					FactLink factlink = (FactLink) iterator2.next();
						if (factlink.e_id!=null) {
							facts.append(fact.name+" "+factlink.e_id+" "+factlink.link+"\n");
						}
						else {
							facts.append(fact.name+" "+factlink.link+"\n");
						}
				}
			}
		}
		
		System.out.println(facts);
		
		String factsString = new String(facts); 		
		doc.add(new Field("facts", factsString, Field.Store.YES, Field.Index.ANALYZED));
		
	}	
}
