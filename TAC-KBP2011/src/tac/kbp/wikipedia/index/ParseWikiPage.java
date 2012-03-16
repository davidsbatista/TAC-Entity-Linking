package tac.kbp.wikipedia.index;

import java.util.Set;

import de.tudarmstadt.ukp.wikipedia.api.Category;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.Title;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;

public class ParseWikiPage {
	
	public static WikiPage getInfo(Page page) throws WikiApiException {
		
		ParsedPage pp = Main.parser.parse(page.getText());
		WikiPage wikiPage = null;
		
		if (!page.isDisambiguation() && !page.isRedirect()) {
			
			Title titleT = page.getTitle();		
			String entity = titleT.getEntity();					// without disambiguation String
			String plaintTitle = titleT.getPlainTitle(); 		// without wikistyle underscores replacing spaces 
			String URL = titleT.getWikiStyleTitle();			// wiki title to be append to "http://pt.wikipedia.org/wiki/"
			
			System.out.println("URL: " + URL );
			
			Set<Category> categories = page.getCategories();	// categories
			String wiki_text = null;
			
			try {
				wiki_text = pp.getText();
				if (wiki_text.startsWith("REDIRECT"))
					return wikiPage;

				else wikiPage = new WikiPage(entity, plaintTitle, URL, wiki_text, categories);
							
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}	
		return wikiPage;
	}	
}