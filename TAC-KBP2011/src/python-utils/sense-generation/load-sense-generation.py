#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import sys
import pickle
import redis
import fileinput

acronyms = dict()

r = redis.Redis(host='localhost', port=6379, db=0)

def read(file):
    pkl_file = open(file, 'rb')
    mappings = pickle.load(pkl_file)
    return mappings
    pkl_file.close()


def load_acronyms(file):
    
    for line in fileinput.input(file):
        
        parts = line.split("\t")        
        acronym = parts[0].strip()
        expansion = parts[-1].strip()
                
        try:
            acronyms[acronym].append(expansion)
              
        except:
            acronyms[acronym] = list()
            acronyms[acronym].append(expansion)

def load(dict):
    h_keys = dict.keys()
    
    """
    http://abaff.wordpress.com/2009/05/22/memcached-keys-cant-have-spaces-in-them/:
    keys cannot have spaces in them!
    """
    
    for k in h_keys:
        
        try:
            r.set(k, dict[k])
                      
        except Exception, e:
            print e

def main():
    
    print "loading disambiguation"
    disambiguation = pickle.load(open("disambiguation_pages.pkl"))
    print "nº disambiguation entries to load: ", len(disambiguation)
    load (disambiguation)
    print r.info
    
    print "loading normalized"
    normalized = pickle.load(open('normalized_articles_and_redirects.pkl'))
    print "nº normalized entries to load: ", len(normalized)
    load (normalized)
    print r.info    
    
    print "loading anchors"
    anchors = pickle.load(open("anchors_to_title_page.pkl"))
    print "nº anchors entries to load: ", len(normalized)
    load(anchors)
    print r.info
    
    print "loading anchors"
    load_acronyms(sys.argv[1])
    load(acronyms)
    
    
if __name__ == "__main__":
    main()