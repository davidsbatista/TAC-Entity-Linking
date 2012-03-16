package tac.kbp.wikipedia.index;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import de.tudarmstadt.ukp.wikipedia.api.Category;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiTitleParsingException;

public class WikiPage {
	
	public String entity;
	public String plaintTitle;
	public String URL;
	public String wiki_text;
	public Set<Category> categories;
	
	
	public WikiPage() {
		super();
	}
	
	public WikiPage(String entity_, String plaintitle_, String URL_, String wiki_text_, Set<Category> categories_) {
		
		super();
		this.entity = entity_;
		this.plaintTitle = plaintitle_;
		this.URL = URL_;
		this.wiki_text = wiki_text_;
		this.categories = categories_;		
	}
	
	
	public Document luceneDoc() throws WikiTitleParsingException {
		
		Document doc = new Document();
		
		try {
			
			doc.add(new Field("entity", this.entity, Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("plaintTitle", this.plaintTitle, Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("URL", this.URL, Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("categories", this.getCategories(), Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
			doc.add(new Field("wiki_text", this.wiki_text, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return doc;
	}
	
	public String getCategories() throws WikiTitleParsingException {
		
		StringBuilder categories = new StringBuilder();
		
		for (Category c : this.categories) {
			String category = c.getTitle().getPlainTitle();
			if ((!category.startsWith("!"))) {
				categories.append(category);
				categories.append('\t');
			}
		}
		
		return categories.toString();
	}
}
