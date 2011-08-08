#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.dom.minidom
import sys,re
import fileinput
import string
import nltk

stopwords = []


def load_stopwords(file):
    for line in fileinput.input(file):
        stopwords.append(line.strip())


def remove_tags(data):
    p = re.compile(r'<.*?>|&quot;')
    return p.sub('', data)


def parse_doc(document):
    xmldoc = xml.dom.minidom.parse(document)
    items = xmldoc.getElementsByTagName('BODY')
    text = remove_tags(str(items[0].toxml()))    
    p = re.compile(r'\s+')
    return p.sub(' ',text)
    

def remove_stopwords(document):
    tokens = nltk.word_tokenize(document)
    tokens_no_stopwords = []
    
    for token in tokens:
        if token not in stopwords:
            tokens_no_stopwords.append(token)
            
    return tokens_no_stopwords


def main():
    load_stopwords(sys.argv[1])
    text_doc = parse_doc(sys.argv[2])
    text_doc_no_stopwords = remove_stopwords(text_doc)
    print string.join(text_doc_no_stopwords, " ")
    
    
if __name__ == "__main__":
    main()