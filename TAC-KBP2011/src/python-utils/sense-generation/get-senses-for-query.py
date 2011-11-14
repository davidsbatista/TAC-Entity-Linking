#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import sys
import redis
import fileinput
import xml.dom.minidom

r = redis.Redis(host='localhost', port=6379, db=0)

queries = []

class Query:
    
    id = None
    string_name = ''
    doc_id = None
    entity_kb_id = None
    entity_type = None
    
    alternative_names = None

    def __init__(self,identifier,string,document):
        self.id = identifier
        self.string_name = string
        self.doc_id = document
        self.alternative_names = []

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

def main():
    parse_queries(sys.argv[1])
    
    for q in queries:
        get_alternative_names(q)
        
        if len(q.alternative_names)>0:
            f_senses = open(q.id+'-alternative-senses.txt','w+')
            
            for s in q.alternative_names:
                f_senses.write(s+"\n")
            
            f_senses.close()

if __name__ == "__main__":
    main()