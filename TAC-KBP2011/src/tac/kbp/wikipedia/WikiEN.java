package tac.kbp.wikipedia;

import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiTitleParsingException;

public class WikiEN {
	
	public static DatabaseConfiguration dbConfig;
	public static Wikipedia wiki;
	
	public static void init() throws WikiApiException {
		
		// configure the database connection parameters
		dbConfig = new DatabaseConfiguration();
		dbConfig.setHost("");
		dbConfig.setDatabase("");
		dbConfig.setUser("");
		dbConfig.setPassword("");
		dbConfig.setLanguage(Language.english);

		// Create the Wikipedia object
		wiki = new Wikipedia(dbConfig);
		
		System.out.println(wiki.getWikipediaId());
	}
	
	public static void getAllPages() throws WikiApiException {
		
		for (Page page : wiki.getArticles()) {
		    getInfo(page.getTitle().getWikiStyleTitle());
		}		
	}
		
	public static void getInfo(String title) throws WikiApiException {

		Page page = wiki.getPage(title);
		        
		// the title of the page
		System.out.println("Queried string       : " + title);
		System.out.println("Title                : " + page.getTitle());

		// whether the page is a disambiguation page
		System.out.println("IsDisambiguationPage : " + page.isDisambiguation());
		        
		// whether the page is a redirect
		// If a page is a redirect, we can use it like a normal page.
		// The other infos in this example are transparently served by the page that the redirect points to. 
		System.out.println("redirect page query  : " + page.isRedirect());
		
		/*
		// the number of links pointing to this page
		System.out.println("# of ingoing links   : " + page.getNumberOfInlinks());
		*/
		        
		// the number of links in this page pointing to other pages
		System.out.println("# of outgoing links  : " + page.getNumberOfOutlinks());

		// the number of categories that are assigned to this page
		System.out.println("# of categories      : " + page.getNumberOfCategories());
		
		//System.out.println("text: " + "\n" + page.getPlainText());
		
		/*
		System.out.println("inLinkAnchors");
		Set<String> inLinkAnchors = page.getInlinkAnchors();
		
		for (String string : inLinkAnchors) {
			System.out.println(string);
		}
		*/
		
		Set<String> getRedirects = page.getRedirects();
		
		System.out.println("Redirects");
		for (String string : getRedirects) {
			System.out.println(string);
		}

		/*
		System.out.println("Outlinks");
		Map<String,Set<String>> outlinkanchors = page.getOutlinkAnchors();
		Set<Page> inlinks = page.getInlinks();
		}
		*/
	}
	
	public static void main(String args[]) throws WikiApiException {
		
		init();
		getAllPages();		
	}
}
