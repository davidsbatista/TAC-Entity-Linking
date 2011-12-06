package tac.kbp.ner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import tac.kbp.queries.KBPQuery;
import tac.kbp.utils.misc.BigFile;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class TagSupportDocs {

	static List<KBPQuery> queries = null;
	static HashMap<String, String> docslocations = new HashMap<String, String>();

	private static void loadDocsLocations(String filename) throws Exception {

		BigFile file = new BigFile(filename);
		String[] parts;

		for (String line : file) {
			parts = line.split(".sgm");
			docslocations.put(parts[0], parts[1]);
		}
	}

	private static String getSupportDocument(KBPQuery q) {

		StringBuilder contents = new StringBuilder();

		try {

			// use buffering, reading one line at a time
			// FileReader always assumes default encoding is OK!

			String file = docslocations.get(q.docid).trim() + "/" + q.docid
					+ ".sgm";

			BufferedReader input = new BufferedReader(new FileReader(file));

			try {
				String line = null; // not declared within while loop
				/*
				 * readLine is a bit quirky : it returns the content of a line
				 * MINUS the newline. it returns null only for the END of the
				 * stream. it returns an empty String if two newlines appear in
				 * a row.
				 */
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return contents.toString();
	}

	public static void main(String[] args) throws Exception {

		String serializedClassifier = args[2];

		/* Load the queries file */
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(args[0]);
		System.err.println(queries.size() + " queries loaded");

		/* Load the text file with location of support documents */
		loadDocsLocations(args[1]);
		System.out
				.println(docslocations.size() + " documents locations loaded");

		AbstractSequenceClassifier classifier = CRFClassifier
				.getClassifierNoExceptions(serializedClassifier);

		for (KBPQuery query : queries) {

			String doc = getSupportDocument(query);
			System.out.println("Tagging: " + query.query_id + ": "
					+ docslocations.get(query.docid).trim() + "/" + query.docid
					+ ".sgm");
			String XMLString = classifier.classifyWithInlineXML(doc);
			
			writeFile(query.query_id+"-CRF-named-entities.xml", XMLString);
			
			/*
			
			Document taggedDocXML = loadXMLFromString(XMLString); 
			 
			NodeList persons = taggedDocXML.getElementsByTagName("PERSON");
			NodeList organization = taggedDocXML
					.getElementsByTagName("ORGANIZATION");
			NodeList location = taggedDocXML.getElementsByTagName("LOCATION");

			for (int i = 0; i < persons.getLength(); i++) {
				System.out.println(persons.item(i).toString());
				System.out.println(persons.item(i).getNodeValue());
			}
			*/
		}
	}

	public static void writeFile(String filename, String contents) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(filename);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(contents);
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static Document loadXMLFromString(String xml) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));

		return builder.parse(is);

	}
}