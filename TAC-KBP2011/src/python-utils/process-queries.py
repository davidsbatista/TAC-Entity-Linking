#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys

import nltk

import fileinput

import xml.dom.minidom
import urllib
import httplib
import simplejson
import tidy  

docs_locations = dict()
queries = []

class Query:
    
    id = None
    string_name = ''
    doc_id = None
    entity_kb_id = None
    entity_type = None
    
    support_doc_persons = None
    support_doc_organizations = None
    support_doc_places = None

    def __init__(self,identifier,string,document):
        self.id = identifier
        self.string_name = string
        self.doc_id = document
        self.support_doc_persons = xml.dom.minidom.NodeList
        self.support_doc_organizations = xml.dom.minidom.NodeList
        self.support_doc_places = xml.dom.minidom.NodeList


def load_docs_locations(file):
    
    for line in fileinput.input(file):
        parts = line.split(".sgm");
        docs_locations[parts[0].strip()] = parts[1].strip()


def parse_queries(file):
    
    try:
        xmldoc = xml.dom.minidom.parse(file)
        items = xmldoc.getElementsByTagName('query')
                    
        if len(items)>0:
            for item in items:                
                
                id = item.getAttribute("id")
                name =  item.childNodes[1].childNodes[0].nodeValue
                docid = item.childNodes[3].childNodes[0].nodeValue
                
                query = Query(id,name,docid)
                queries.append(query)
    
    except:
        print "Error parsing queries"


def call_rembrandt(document):

    """ REMBRANDT API """
    slg_value = "en"
    lg_value = "en"
    rembrandt_key_value = "db924ad035a9523bcf92358fcb2329dac923bf9c"
    format = "dsb"

    params = urllib.urlencode({"db": document, "slg": slg_value, "lg": lg_value, "api_key": rembrandt_key_value, "f": format})
    headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain"}
    conn = httplib.HTTPConnection("agatha.inesc-id.pt:80")
    conn.request("POST", "/Rembrandt/api/rembrandt?", params, headers)
    
    response = conn.getresponse()
    
    """
    DEBUG
    print "\n"
    print response.msg
    print response.status 
    print response.reason
    """
        
    data = response.read()
    conn.close()
    JSONObject = simplejson.loads(data)

    return JSONObject["message"]["document"]["body"]


def get_entities(query):
    
    """ call REMBRANDT over the support document """
    
    # let's get the document location
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    document = file.read()

    # do NER on the document
    rembrandted_document = call_rembrandt(document)
    
    # tidy the xml output by REMBRANDT to make it parsable by xml.dom.minidom.parseString
    options = { 'output_xhtml' : 1, 'add_xml_decl' : 1, 'indent' : 1, 'input-encoding' : 'utf8', 'output-encoding' : 'utf8', 'tidy_mark' : 0}    
    xml_tidydoc = tidy.parseString(rembrandted_document.encode("utf8"),**options)
    
    #try:
    print xml_tidydoc
    
    xmldoc = xml.dom.minidom.parseString(str(xml_tidydoc))
    
    persons = None
    organizations = None
    places = None
                
    persons = xmldoc.getElementsByTagName('PERSON')
    organizations = xmldoc.getElementsByTagName('ORGANIZATION')
    places = xmldoc.getElementsByTagName('PLACE')
        
    query.support_doc_persons = persons;
    query.support_doc_organizations = organizations;
    query.support_doc_places = places;

    #except:
        #print "Error parsing XML returned by REMBRANDT"
        #print str(xml_tidydoc)


def concordance(doc_id):
    tokens = nltk.word_tokenize(document)
    text = nltk.Text(tokens)
    concordance = nltk.ConcordanceIndex(text)
    print "context for: " + '"'+entity+'"'
    offsets = concordance.offsets(entity)   
    concordance.print_concordance(entity);


def analyze_support_document(query):
    get_entities(query)
    print query.support_doc_persons
    print query.support_doc_organizations
    print query.support_doc_places


def get_alternative_names(query_string):
    """ get alternative names """




def start():    
    for q in queries:
        print "Processing query: " + q.id +' "'+ q.string_name +'"' 
        print 
        
        """
        # get_alternative_names();
        # query_lucene ();
        """
        
        analyze_support_document(q)
        


def help():
    print "Usage:"
    print "\t process-queries support-documents-locations-file queries-file"




def main():
    if len(sys.argv) < 3 or len(sys.argv) > 3:
        help()
    else:  
        load_docs_locations(sys.argv[1])
        parse_queries(sys.argv[2])
        print len(docs_locations), "support documents loaded"
        print len(queries), "queries loaded"
        start()



if __name__ == "__main__":
    main()
    
    

