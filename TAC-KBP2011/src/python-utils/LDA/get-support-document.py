#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import re
import string

import xml.dom.minidom
import fileinput

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
        self.alternative_names = []

def main():
    load_docs_locations(sys.argv[1])
    parse_queries(sys.argv[2])
    load_stopwords(sys.argv[3])
    text = ''
    
    for q in queries:
        text+= parse_doc(docs_locations[q.doc_id]+"/"+q.doc_id+".sgm") 
        text+= '\n'

    f = open("queries_one_file_documents.txt","wb")
    f.write(text.encode("utf8"))
    f.close()

def parse_doc(document):
    xmldoc = xml.dom.minidom.parse(document)
    items = xmldoc.getElementsByTagName('BODY')
    text = remove_all_tags(items[0].toxml().encode("utf8"))
    text_no_stopwords = remove_stopwords(text)
    p = re.compile(r'\s+')
    return p.sub(' ',text_no_stopwords)

def remove_stopwords(text):
    parts = text.split(" ")
    no_stopwords = []
    for w in parts:
        if w.encode("utf8") in stopwords:
            continue
        else:
         no_stopwords.append(w)
         
    return string.join(no_stopwords, " ")

def remove_all_tags(data):
    return ''.join(BeautifulSoup(data).findAll(text=True))

def load_stopwords(file):
    for line in fileinput.input(file):
        stopwords.append(line.strip())

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
    
    except Exception, e:
        print "Error parsing queries"
        print e

if __name__ == "__main__":
    main()
