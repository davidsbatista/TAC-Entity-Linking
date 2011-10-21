#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import sys
import redis
import fileinput

big_dict = dict()
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
            acronyms[acronym] = []
            acronyms[acronym].append(expansion)

def load(dict):
    h_keys = dict.keys()
    
    for k in h_keys:        

        try:
            for el in dict[k]:
                big_dict[k].append(el)
                                  
        except:
            big_dict[k] = []
            for el in dict[k]:
                big_dict[k].append(el)

def load_to_cache():
    h_keys = big_dict.keys()
    
    for k in h_keys:
        r.set(k,big_dict[k])


def main():

    print "loading disambiguation"
    disambiguation = eval(open("disambiguation.file").read())
    print "nº disambiguation entries to load: ", len(disambiguation)
    load(disambiguation)
    disambiguation = dict()
    
    print "loading normalized"
    normalized = eval(open("normalized.file").read())
    print "nº normalized entries to load: ", len(normalized)
    load(normalized)    
    normalized = dict() 
    
    print "loading anchors"
    anchors = eval(open("anchors.file").read())
    print "nº anchors entries to load: ", len(anchors)
    load(anchors)
    anchors = dict()
    
    print "load acronyms"
    load_acronyms(sys.argv[1])
    #print "nº acronyms entries to load: ", len(acronyms)
    load(acronyms)
    acronyms = dict()
    
    print "writing to file"
    f = open("all.file","w")
    f.write(str(big_dict))
    f.close()
    
    print "loading to redis server"
    load_to_cache()
    
if __name__ == "__main__":
    main()