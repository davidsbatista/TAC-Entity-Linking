#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import nltk

def main():
    f = sys.argv[1]
    entity = sys.argv[2]
    
    file = open(f)
    document = file.read()
    
    tokens = nltk.word_tokenize(document)
    text = nltk.Text(tokens)
    
    concordance = nltk.ConcordanceIndex(text)
    
    print "context for: " + '"'+entity+'"'
    
    offsets = concordance.offsets(entity)
    
    #print offsets
    
    concordance.print_concordance(entity);


if __name__ == "__main__":
    main()