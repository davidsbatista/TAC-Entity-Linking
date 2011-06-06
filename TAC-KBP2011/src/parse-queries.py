#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.dom.minidom
import simplejson
import urllib
import httplib

import sys
import os
import re
import time
import fileinput

import MySQLdb
import StringIO 

from datetime import datetime
from BeautifulSoup import BeautifulSoup,NavigableString

queries = {}


class Query:
    
    id = None
    string_name = ''
    doc_id = None
    entity_kb_id = None
    entity_type = None
    

    def __init__(self,identifier,string,document):
        self.id = identifier
        self.string_name = string
        self.doc_id = document
    


def parse_queries_types(file):
    
    for line in fileinput.input(file):
        query_id,kb_id,entity_type = line.split("\t")
        
        try:
            queries[query_id].entity_kb_id = kb_id 
            queries[query_id].entity_type = entity_type
            
        except:
            print "Error! query: ", query_id, "not parsed"
    

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
                queries[id] = query
    
    except:
        print "Error parsing queries"



def main():

    """    
    if os.path.isdir(sys.argv[1]):
        
        dir_list = os.listdir(sys.argv[1])
        
        for file in sorted_dir_list:
            
            if file.rsplit(".")[1] == 'xml':
    """
    parse_queries(sys.argv[1])
    parse_queries_types(sys.argv[2])

   
    for q in queries:
        print queries[q].id, queries[q].string_name, queries[q].doc_id, queries[q].entity_kb_id, queries[q].entity_type, 
        
if __name__ == "__main__":
    main()
    


