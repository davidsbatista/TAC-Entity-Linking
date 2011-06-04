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

import MySQLdb
import StringIO 

from datetime import datetime
from BeautifulSoup import BeautifulSoup,NavigableString

class Query:
    
    id = None
    string_name = ''
    doc_id = None
    
    def __init__(self,identifier,string,document):
        self.id = identifier
        self.string_name = string
        self.doc_id = document
    

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
    
    except:
        print "Error parsing queries"


def main():
    
    parse_queries(sys.argv[1])
    
if __name__ == "__main__":
    main()