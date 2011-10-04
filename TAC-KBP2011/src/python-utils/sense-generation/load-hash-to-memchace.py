#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import pickle
import memcache

mc = memcache.Client(['127.0.0.1:11211'], debug=1)


def read(file):
    pkl_file = open(file, 'rb')
    mappings = pickle.load(pkl_file)
    return mappings
    pkl_file.close()


def load(dict):
    h_keys = dict.keys()
    
    """
    http://abaff.wordpress.com/2009/05/22/memcached-keys-cant-have-spaces-in-them/:
    keys cannot have spaces in them!
    """
    
    for k in h_keys:
        
        try:
            mc.set(k.strip().replace(" ","_").encode("utf8"),dict[k])
                      
        except Exception, e:
            print e
        


def main():
    
    disambiguation = pickle.load(open("disambiguation_pages.pkl"))    
    print "nº disambiguation entries to load: ", len(disambiguation)
    load (disambiguation)
    print mc.stats
    
    normalized = pickle.load(open('normalized_articles_and_redirects.pkl'))
    print "nº normalized entries to load: ", len(normalized)
    load (normalized)
    print mc.stats    
    
    anchors = pickle.load(open("anchors_to_title_page.pkl"))
    print "nº anchors entries to load: ", len(normalized)
    load(anchors)
    print mc.stats
    
if __name__ == "__main__":
    main()