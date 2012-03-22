package tac.kbp.slotfilling.queries;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class LoadQueries{

    public static Map<String, SFQuery> loadXML(String file) {
    	
    	try {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (new File(file));
            
            NodeList queries = doc.getElementsByTagName("query");
            
            Map<String,SFQuery> squeries = new HashMap<String,SFQuery>(); 
            
            for (int s=0; s < queries.getLength(); s++){
            	Node query = queries.item(s);
    			
                if (query.getNodeType() == Node.ELEMENT_NODE) {
                	Element eElement = (Element) query;
    					
    				String query_id = eElement.getAttribute("id");
    				String name = getTagValue("name", eElement);
    				String docid = getTagValue("docid", eElement);
    				String etype = getTagValue("enttype", eElement);
                	String nodeid = getTagValue("nodeid", eElement);
                	String ignore = null;
                	
                	if (eElement.getElementsByTagName("ignore").getLength()>0)
                		ignore = getTagValue("ignore", eElement);
                	
                	System.out.println("query_id: " + query_id);
                	System.out.println("name: " + name);
                	System.out.println("docid: " + docid);
                	System.out.println("etype: " + etype);
                	System.out.println("nodeid: " + nodeid);
                	System.out.println("ignore: " + ignore);
                	System.out.println();
                	
                	SFQuery q = new SFQuery(query_id, name, docid, etype, nodeid);
                	if (ignore!=null)
                		q.ignore = ignore;
                	squeries.put(query_id,q);                	
                }                
            }
            
            System.out.println(squeries.size() + " loaded");            
            return squeries;

        } catch (SAXParseException err) {
	        System.out.println ("** Parsing error" + ", line " + err.getLineNumber () + ", uri " + err.getSystemId ());
	        System.out.println(" " + err.getMessage ());

        } catch (SAXException e) {
	        Exception x = e.getException ();
	        ((x == null) ? e : x).printStackTrace ();

        } catch (Throwable t) {
        	t.printStackTrace ();
        }
        
		return null;
    }
    
    public static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = (Node) nlList.item(0);

		return nValue.getNodeValue();
	}
}