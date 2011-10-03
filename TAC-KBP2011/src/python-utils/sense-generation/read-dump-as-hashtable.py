#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import string as string
import sys
import re
import pickle
import MySQLdb


def read(file):
    pkl_file = open(file, 'rb')
    mappings = pickle.load(pkl_file)
    return mappings
    pkl_file.close()
    
    
def main():
    disambiguation = {}
    disambiguation = read("disambiguation_pages.pkl")
    
    f = open('disambiguation.file','w')
    f.write(str(disambiguation))
    f.close()
    disambiguation = {}
    
    
    anchors = {}
    anchors = read("anchors_to_title_page.pkl")
    
    f = open('anchors.file','w')
    f.write(str(anchors))
    f.close()
    anchors = {}
    
    
    normalized = {}
    normalized = read('normalized_articles_and_redirects.pkl')
    
    f = open('normalized.file','w')
    f.write(str(normalized))
    f.close()
    normalized = {}

    
if __name__ == "__main__":
    main()