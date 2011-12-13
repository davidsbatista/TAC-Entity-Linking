package tac.kbp.queries.xml;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tac.kbp.bin.Definitions;
import tac.kbp.queries.KBPQuery;

public class ParseQueriesXMLFile {
	
	public static List<KBPQuery> loadQueries(String filename) {
		
		List<KBPQuery> queries = new LinkedList<KBPQuery>();
		
		/*
		 * XML query structure:
		 * 
		 * <kbpentlink> 
		 * 	<query id="EL000014"> 
		 * 	   <name>AZ</name>
		 *	   <docid>eng-WL-11-174595-12967314</docid> 
		 * </query>
		 */

		try {

			File fXmlFile = new File(filename);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("query");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					String query_id = eElement.getAttribute("id");
					String name = getTagValue("name", eElement);
					String docid = getTagValue("docid", eElement);
					
					KBPQuery query = new KBPQuery(query_id, name, docid);
					query.answer_kb_id = Definitions.queriesGold.get(query_id).answer;
					queries.add(query);

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return queries;
	}

	public static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = (Node) nlList.item(0);

		return nValue.getNodeValue();
	}
}