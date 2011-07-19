package tac.kbp.queries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.ninit.models.bm25.BM25BooleanQuery;
import org.ninit.models.bm25.BM25Parameters;
import org.ninit.models.bm25f.BM25FParameters;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;

import tac.kbp.utils.BigFile;

public class ProcessQuery {

	static String docs = "documents-locations.txt";
	static String fields_size = "fields_avg_size.txt";
	static HashMap<String, String> docslocations = new HashMap<String, String>();
	static IndexSearcher searcher = null; 
	static List<Query> queries = null;
	
	public static void main(String[] args) throws Exception {
		
		/* Load the queries file */
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(args);
		System.err.println(queries.size() + " queries loaded");
		
		/* Load the text file with location of support documents */
		loadDocsLocations();		
		System.out.println(docslocations.size() + " documents locations loaded");
		
		/* Lucene Index */
		searcher = new IndexSearcher(FSDirectory.getDirectory(args[1]));
		
		//Load fields average length
		BM25Parameters.load(fields_size);
		
		//Set explicit the k1 and B parameter
		BM25FParameters.setK1(0.1f);
		BM25FParameters.setB(1.0f);
		
		for (Iterator<Query> iterator = queries.iterator(); iterator.hasNext();) {
			Query query = (Query) iterator.next();
			System.out.println("Processing query: " + query.query_id);
			processQuery(query);
		}
		
		generateOutput(args[2]);
	
	}

	
	private static JSONObject callRembrandt(String text) {
		
		HttpClient httpclient = new HttpClient();
		
		PostMethod postMethod = new PostMethod("http://agatha.inesc-id.pt:80/Rembrandt/api/rembrandt?");
		
		NameValuePair slg = new NameValuePair("slg", "en");
		NameValuePair lg = new NameValuePair("lg", "en");
		NameValuePair format = new NameValuePair("f", "dsb");
		NameValuePair key = new NameValuePair("api_key","db924ad035a9523bcf92358fcb2329dac923bf9c");
		NameValuePair sentence = new NameValuePair("db",text);
		
		postMethod.addParameter(slg);
		postMethod.addParameter(lg);
		postMethod.addParameter(format);
		postMethod.addParameter(key);
		postMethod.addParameter(sentence);
		
		BufferedReader br = null;
		JSONObject jsonObj = null;
		
		try{			
			int returnCode = httpclient.executeMethod(postMethod);
			
			if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {		    	  
				System.err.println("The Post method is not implemented by this URI");
				// still consume the response body
				postMethod.getResponseBodyAsString();
			}
			
			else {
		        String response = postMethod.getResponseBodyAsString();
		        jsonObj = new JSONObject(response);
		      }

			
		} catch (Exception e) {
			
		      System.err.println(e);
		      
		    } finally {
		    	
		      postMethod.releaseConnection();
		      
		      if (br != null)
		    	  
		    	  try { 
		    		  br.close();
		    	  } 
		      	  catch (Exception fe) {}
		    }
	
	        return jsonObj;
		    
	}
	
	private static void processQuery(Query q) throws Exception {
		String supportDoc = getSupportDocument(q);
		Document doc = queryLucene(q);
		
		System.out.println("Calling REMBRANDT...");
		JSONObject jsonObj = callRembrandt(supportDoc);
		
		if (jsonObj!=null) {
			
			String documentString = (jsonObj.getJSONObject("message").getJSONObject("document").getString("body"));
			String cleanedDocumentString = documentString.
			replaceAll("&amp\\s;", "&amp;").
			replaceAll("&lt\\s;", "&lt;").
			replaceAll("&gt\\s;", "&gt;").
	        replaceAll("&quot\\s;", "&quot;").
	        replaceAll("&nbsp\\s;", "&nbsp;").
			replaceAll("&iexcl\\s;", "&iexcl;").
			replaceAll("&cent\\s;", "&cent;").
			replaceAll("&&pound\\s;","&pound;").
			replaceAll("&curren\\s;", "&curren;").
			replaceAll("&yen\\s;", "&yens;");
			/*			
			&brvbar;
			&sect;
			&uml;
			&copy;
			&ordf;
			&laquo;
			&not;
			&shy;
			&reg;
			&macr;
			&deg;
			&plusmn;
			&sup2;
			&sup3;
			&acute;
			&micro;
			&para;
			&middot;
			&cedil;
			&sup1;
			&ordm;
			&raquo;
			&frac14;
			&frac12;
			&frac34;
			&iquest;
			&Agrave;
			&Aacute;
			&Acirc;
			&Atilde;
			&Auml;
			&Aring;
			&AElig;
			&Ccedil;
			&Egrave;
			&Eacute;
			&Ecirc;
			&Euml;
			&Igrave;
			&Iacute;
			&Icirc;
			&Iuml;
			&ETH;
			&Ntilde;
			&Ograve;
			&Oacute;
			&Ocirc;
			&Otilde;
			&Ouml;
			&times;
			&Oslash;
			&Ugrave;
			&Uacute;
			&Ucirc;
			&Uuml;
			&Yacute;
			&THORN;
			&szlig;
			&agrave;
			&aacute;
			&acirc;
			&atilde;
			&auml;
			&aring;
			&aelig;
			&ccedil;
			&egrave;
			&eacute;
			&ecirc;
			&euml;
			&igrave;
			&iacute;
			&icirc;
			&iuml;
			&eth;
			&ntilde;
			&ograve;
			&oacute;
			&ocirc;
			&otilde;
			&ouml;
			&divide;
			&oslash;
			&ugrave;
			&uacute;
			&ucirc;
			&uuml;
			&yacute;
			&thorn;
			&yuml;
			*/
			
	        System.out.println(cleanedDocumentString);
	        
	        InputSource is = new InputSource();
	        is.setCharacterStream(new StringReader(cleanedDocumentString));
	        
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();		
	        factory.setValidating(false);
	        factory.setNamespaceAware(false);
	        DocumentBuilder loader = factory.newDocumentBuilder();
	        
	        org.w3c.dom.Document XMLDoc =  loader.parse(is);
	        
	        NodeList persons = XMLDoc.getElementsByTagName("PERSON");
	        NodeList organizations = XMLDoc.getElementsByTagName("ORGANIZATION");
	        NodeList places = XMLDoc.getElementsByTagName("PLACE");
	        
	        //TODO: trim() the the text that represents the Entity
	        
	        System.out.println("= PERSONS =");
	        for (int i = 0; i < persons.getLength(); i++) {
	            Element element = (Element) persons.item(i);
	            System.out.println(element.getTextContent());
	        }
	        
	        System.out.println("= ORGANIZATIONS =");
	        for (int i = 0; i < organizations.getLength(); i++) {
	            Element element = (Element) organizations.item(i);
	            System.out.println(element.getTextContent());
	        }
	        
	        System.out.println("= PLACES =");
	        for (int i = 0; i < places.getLength(); i++) {
	            Element element = (Element) places.item(i);
	            System.out.println(element.getTextContent());
	        }

	        
	        
	        
	        
		}
		
		
		if (doc!=null) {		
			String id = doc.getField("id").stringValue();
			q.answer_kb_id = id;
		}
		
		else {
			q.answer_kb_id = "NIL";
		}

	}
	
	private static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (Iterator<Query> iterator = queries.iterator(); iterator.hasNext();) {
			Query q = (Query) iterator.next();
			out.println(q.query_id.trim()+"\t"+q.answer_kb_id.trim());
		}
		out.close();		
	}
	
	private static String getSupportDocument(Query q) {
		
		StringBuilder contents = new StringBuilder();
	    
	    try {
	    	
	    	//use buffering, reading one line at a time
	    	//FileReader always assumes default encoding is OK!
	    	
	    	String file = docslocations.get(q.docid).trim()+"/"+q.docid+".sgm";
	    	
	    	BufferedReader input =  new BufferedReader(new FileReader(file));
	    	
	    	try {
	    		String line = null; //not declared within while loop
		        /*
		        * readLine is a bit quirky :
		        * it returns the content of a line MINUS the newline.
		        * it returns null only for the END of the stream.
		        * it returns an empty String if two newlines appear in a row.
		        */
		        while (( line = input.readLine()) != null){
		          contents.append(line);
		          contents.append(System.getProperty("line.separator"));
		        }
	      }
	      finally {
	        input.close();
	      }
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
	    
	    return contents.toString();		
	}
	
	private static Document queryLucene(Query q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		Document doc = null;
		
		//String[] fields = {"wiki_title","type","id","name","infobox_class","wiki_text","facts"};
		String[] fields = {"name","wiki_text","facts"};
		float[] boosts =  {1f,1f,1f};
		float[] bParams =  {1.0f,1.0f,1.0f};
		
		BM25BooleanQuery query = new BM25BooleanQuery(q.name, fields, analyzer, boosts, bParams);
		
		//Using boost and b defaults parameters	
		TopDocs top = searcher.search(query, null, 100);				
		ScoreDoc[] docs = top.scoreDocs;
		
		if (top.scoreDocs.length>0) {
			doc = searcher.doc(docs[0].doc);
		}

		return doc;
		
		/*
		for (int i = 0; i < top.scoreDocs.length; i++) {
			Document doc = searcher.doc(docs[i].doc);
			String id = doc.getField("id").stringValue();
			String wiki_title = doc.getField("wiki_title").stringValue();
			String type = doc.getField("type").stringValue();
			System.out.println(id+": "+wiki_title+" "+type);
		}
		*/
	}
	
	private static void loadDocsLocations() throws Exception {
		
		BigFile file = new BigFile(docs);
		String[] parts;
		
		for (String line : file) {		
			parts = line.split(".sgm");			
			docslocations.put(parts[0], parts[1]);
		}
		
	}
}
