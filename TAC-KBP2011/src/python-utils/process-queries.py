#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys

import fileinput

import nltk

import xml.dom.minidom
import httplib
import urllib
import simplejson

from BeautifulSoup import BeautifulSoup

docs_locations = dict()
queries = []


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
        self.alternative_names = []


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
            
        query.support_doc_persons = persons;
        query.support_doc_organizations = organizations;
        query.support_doc_places = places;

    except Exception, e:
        print "Error parsing XML returned by REMBRANDT"
        print e
        print rembrandted_document.encode("utf8")


def get_context(query):
    
    # let's get the document location
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    document = file.read()
    
    tokens = nltk.word_tokenize(document)
    text = nltk.Text(tokens)
    concordance = nltk.ConcordanceIndex(text)
    
    """ adapted from nltk.text.ConcordanceIndex.print_concordance """
    offsets = concordance.offsets(query.string_name)
    occurrences = []
    lines=25
    width=75
    half_width = (width - len(query.string_name) - 2) / 2 
    context = width/4 # approx number of words of context
    
    if offsets: 
        lines = min(lines, len(offsets))  
        for i in offsets: 
            if lines <= 0: 
                break 
            left = (' ' * half_width + ' '.join(tokens[i-context:i])) 
            right = ' '.join(tokens[i+1:i+context]) 
            left = left[-half_width:] 
            right = right[:half_width]                          
            occurrences.append(left + " " + tokens[i] + " " + right)
            lines -= 1

        query.support_doc_context_occurences = occurrences


def remove_stop_words(sentence):
    word_list = nltk.word_tokenize(sentence)
    for word in word_list: # iterate over word_list
        if word in nltk.corpus.stopwords.words('english'): 
            word_list.remove(word) # remove word from filtered_word_list if it is a stopword
    return word_list


def analyze_support_document(query):
    
    print "Suppor document: ", docs_locations[query.doc_id]+"/"+query.doc_id+".sgm"
    
    get_entities(query)
    print "PERSONS", len(query.support_doc_persons)
    print "ORGANIZATIONS", len(query.support_doc_organizations)
    print "PLACES", len(query.support_doc_places)
    
    get_context(query)
    print "Number context occurence sentences", len(query.support_doc_context_occurences)
    

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
        #query_lucene
        #topics similarity
        


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
    
    

