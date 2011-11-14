#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
#!/opt/python2.6/bin/python2.6
"""

import sys
import re

import xml.dom.minidom
import fileinput

import urllib
import httplib
import simplejson

import string

from BeautifulSoup import BeautifulSoup

docs_locations = dict()
queries = []
stopwords = []

class Query:
    
    id = None
    string_name = ''
    doc_id = None
    entity_kb_id = None
    entity_type = None
    
    alternative_names = None
    support_doc_persons = None
    support_doc_organizations = None
    support_doc_places = None
    support_doc_context_occurences = None

    def __init__(self,identifier,string,document):
        self.id = identifier
        self.string_name = string
        self.doc_id = document
        self.support_doc_persons = xml.dom.minidom.NodeList
        self.support_doc_organizations = xml.dom.minidom.NodeList
        self.support_doc_places = xml.dom.minidom.NodeList
        self.support_doc_context_occurences = []
        self.alternative_names = set()
   
def parse_doc(document):
    xmldoc = xml.dom.minidom.parse(document)
    items = xmldoc.getElementsByTagName('BODY')
    text = remove_all_tags(items[0].toxml().encode("utf8"))    
    p = re.compile(r'\s+')
    return p.sub(' ',text)

def load_docs_locations(file):
    
    for line in fileinput.input(file):
        parts = line.split(".sgm");
        docs_locations[parts[0].strip()] = parts[1].strip()

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

    try:
        JSONObject = simplejson.loads(data)
        return JSONObject["message"]["document"]["body"]
    
    except Exception, e:
        print e

def get_entities(query):
    
    """ call REMBRANDT over the support document """
    
    # let's get the document location
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    document = file.read()

    # do NER on the document
    rembrandted_document = call_rembrandt(document)
    
    if rembrandted_document:
          
        rembrandted_document_valid_xml = BeautifulSoup(rembrandted_document)
        
        doc = unicode(rembrandted_document_valid_xml)
        
        try:
            xmldoc = xml.dom.minidom.parseString(doc.encode("utf-8"))
            
            persons = None
            organizations = None
            places = None
                        
            persons = xmldoc.getElementsByTagName('person')
            organizations = xmldoc.getElementsByTagName('organization')
            places = xmldoc.getElementsByTagName('place')
            
            query.support_doc_persons = persons
            query.support_doc_organizations = organizations
            query.support_doc_places = places
    
        except Exception, e:
            print "Error parsing XML returned by REMBRANDT"
            print e
            print rembrandted_document.encode("utf8")

def get_context(query):
    
    """ remove stop words from document """
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    document = file.read()
    
    p = re.compile(r'<.*?>|&quot;|&amp;')
    doc_no_tags = p.sub('', document)
    
    document_no_stop_words = []

    for i in doc_no_tags.split():
        if i in string.punctuation or i in stopwords:
            continue
        else:
            document_no_stop_words.append(i)
    
    concordances = []
    
    for alternative in query.alternative_names:
        [ concordances.extend(el) for el in getConcordance(" ".join(document_no_stop_words),query.string_name,10)]
        
    query.support_doc_context_occurences = concordances

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
    
    except Exception, e:
        print "Error parsing queries"
        print e

def analyze_support_document(query):
    
    print "Support document: ", docs_locations[query.doc_id]+"/"+query.doc_id+".sgm"
    
    get_entities(query)
    #get_context(query)

def load_stopwords(file):
    for line in fileinput.input(file):
        stopwords.append(line.strip())

def get_alternative_names(query):
        
    alternative_names = r.get(query.string_name.lower())
    acronyms = r.get(query.string_name)
        
    senses = set()
    
    if alternative_names:
        for el in eval(alternative_names):
            if el.lower() != query.string_name.lower():
                senses.add(el)
    
    if acronyms:
        for el in eval(acronyms):
            if el.lower() != query.string_name.lower():
                senses.add(el)
    
    query.alternative_names = senses

def getConcordance(text,word,offset):
    
    tokens = re.findall(u'\w+',text,re.U)
    listOfIndexes  = list(find_all_in_list( tokens,word))
    
    listOfConcordance = []
    
    for i in listOfIndexes:
        
        start = i - offset
        
        if start < 0:
            start = 0
        
        end = i + offset + 1
         
        if end > len(text):
            end = len(text)
        
        listOfConcordance.append(tokens[start:end])
    
    return listOfConcordance

def find_all_in_list(a_str, sub):
    
    start = 0
    
    while True:
         
        try:
            start = start + a_str[start:].index(sub)
        except ValueError:
            return
        yield start
        
        start+=1

def main():
    load_docs_locations(sys.argv[1])
    parse_queries(sys.argv[2])
    load_stopwords(sys.argv[3])
    
    for q in queries:
        #get_alternative_names(q)
        analyze_support_document(q)
    
    
    for q in queries:
        
        f_entities = open(q.id+'-named-entities.txt','w+')
        
        f_entities.write("PERSONS:\n")         
        for e in q.support_doc_persons:
            f_entities.write(e.firstChild.toxml()+"\n")
        
        f_entities.write("\n")
        f_entities.write("PLACES:\n")
        
        for e in q.support_doc_places:
            f_entities.write(e.firstChild.toxml()+"\n")
        
        f_entities.write("\n")
        f_entities.write("ORGANIZATIONS:\n")
        
        for e in q.support_doc_organizations: 
            f_entities.write(e.firstChild.toxml()+"\n")

        f_entities.close()

        """
        q.support_doc_organizations = xml.dom.minidom.NodeList
        q.support_doc_places = xml.dom.minidom.NodeList
        """
    
if __name__ == "__main__":
    main()
