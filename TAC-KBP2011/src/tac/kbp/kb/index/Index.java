/**
 * @author dsbatista
 * 
 */

package tac.kbp.kb.index;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import tac.kbp.kb.index.xml.Entity;
import tac.kbp.kb.index.xml.Fact;
import tac.kbp.kb.index.xml.FactLink;


public class Index {

	public static IndexWriter createIndex(String dir) throws CorruptIndexException, LockObtainFailedException, IOException {
		
		//Directory dir = new RAMDirectory();
		IndexWriter indexDir = new IndexWriter(FSDirectory.getDirectory(dir), new WhitespaceAnalyzer(),IndexWriter.MaxFieldLength.UNLIMITED);		
		CollectionSimilarityIndexer similarity = new CollectionSimilarityIndexer();
		indexDir.setSimilarity(similarity);
		
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
				facts.append("FACT:"+fact.name+"_"+fact.fact+"; ");
			}

			if (fact.factlink.size()>0) {
				for (Iterator<FactLink> iterator2 = fact.factlink.iterator(); iterator2.hasNext();) {
					FactLink factlink = (FactLink) iterator2.next();
						if (factlink.e_id!=null) {
							facts.append("FACT:"+fact.name+"_EID:"+factlink.e_id+"_LINK:"+factlink.link+"; ");
						}
						else {
							facts.append("FACT:"+fact.name+"_"+factlink.link+"; ");
						}
				}
			}
		}
		
		String factsString = new String(facts); 		
		doc.add(new Field("facts", factsString, Field.Store.YES, Field.Index.ANALYZED));
		index.addDocument(doc);
	}
}
