#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.dom.minidom
import sys,os
import codecs
import fileinput
import re
import nltk

from subprocess import Popen, PIPE

kb_lda_models = "kb_lda_models"
stopwords = []

def remove_stopwords(document):
    tokens_no_stopwords = []
    
    for token in document:
        if token.encode("utf8") not in stopwords:
            tokens_no_stopwords.append(token)
            
    return tokens_no_stopwords


def load_stopwords(file):
    for line in fileinput.input(file):
        stopwords.append(line.strip())

def get_topics(id,wiki_text):
    
    """ first get the document and transform it for GibbsLDA format:
        - all words in one line
        - remove stopwords
        - add 1 to the first line
    """
    
    doc_text_no_stopwords = remove_stopwords(wiki_text.split())
    
    """
    directory will be kb_lda_models/ELXXXXX/
    """
    os.mkdir(kb_lda_models+"/"+id)
    
    wiki_text_lda_format = codecs.open(kb_lda_models+'/'+id+'/wiki_text_lda_format', "w", "utf-8")
    text_string = "1\n"+" ".join(doc_text_no_stopwords)    
    wiki_text_lda_format.write(text_string)
    wiki_text_lda_format.close()
    
    command = "/home/dsbatista/GibbsLDA++-0.2/src/lda"
    args = " -inf -dir /collections/TAC-2011/" + kb_lda_models + " -model model-final -niters 20 -dfile " + id + "/" + 'wiki_text_lda_format'
    
    print "Calling GibbsLDA++... ", command + args                                         
    p = Popen(command+args,shell=True,stdout=PIPE,stderr=PIPE)
    output, stderr_output = p.communicate()
    print stderr_output     
    print output


def parse_doc(document):
    xmldoc = xml.dom.minidom.parse(document)
    items = xmldoc.getElementsByTagName('entity')
    
    for i in items:
        id = i.getAttribute("id")
        wiki_text = i.getElementsByTagName("wiki_text")
        text = wiki_text[0].firstChild.wholeText
        get_topics(id,text)


def main():
    
    load_stopwords(sys.argv[1])
    parse_doc(sys.argv[2])

if __name__ == "__main__":
    main()
    
    
