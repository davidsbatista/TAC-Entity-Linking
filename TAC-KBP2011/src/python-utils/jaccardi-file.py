#!/usr/bin/env python
# -*- coding: utf-8 -*-

import simplejson as json
import os,sys
import fileinput

from operator import itemgetter

entities = list()
wordEC = dict()

class Entity:
    
    kb_id = ''
    name = ''
    type = ''
    information_content = 0.0
    
    def __init__(self,kb_id_,name_,type_,information_content_):
        self.kb_id = kb_id_
        self.name = name_
        self.type = type_
        self.information_content = information_content_

def get_entities(file):
    
    for line in fileinput.input(file):
        entity =  line.rstrip().split('\t')
                
        kb_id = entity[0].strip()
        name = entity[1].lower().strip()    
        type = entity[2].strip()
        information_content = float(entity[3]) 
        
        entity = Entity(kb_id,name,type,information_content)
        
        entities.append(entity)

def matches(query):
    
    matches = list()    
    query_set = set(query.split())
        
    for e in entities:
        entity_set = set(e.name.split())
        if len (query_set.intersection((entity_set))) == 0:
            continue
        else:
            matches.append(e)
            
    return matches


def load_wordEC(file):
    for line in fileinput.input(file):
        entry =  line.rstrip().split('\t')    
        wordEC[entry[0]] = entry[1]


def nameEC(words):    
    ec = float(0)
    
    for w in words:
        ec += float(wordEC[w])

    return ec

          
def relatedness(query,entity):
    
    if query == entity.name:
        return float(1.0)
        
    else:  
        query_set = set(query.split())
        entity_set = set(entity.name.split())
        
        intersection = query_set.intersection(entity_set)       
        union = query_set.union(entity_set)
        
        inter_val = nameEC(intersection)
        union_val = nameEC(union)
        
        return float(inter_val/union_val)       


def main():
            
    get_entities(sys.argv[1])
    load_wordEC(sys.argv[2])
    query = sys.argv[3]
    
    print "entities: ", len(entities)
    print "query: ", query
    print "word content: ", len(wordEC)
    
    mappings = matches(query)
    mappings_scores = list()
    
    print "mappings :", len(mappings)
    
    for entity in mappings:
        mappings_scores.append((entity.kb_id, entity.name, entity.type, relatedness(query,entity)))
        
    mappings_scores_sorted = sorted(mappings_scores, key=itemgetter(3), reverse=True)
    
    last_score = 1 
    matchings = list()
        
    """
    for el in mappings_scores_sorted:        
        last_score = el[2]
        if last_score < 0.25:
            break
        else:
            match = dict()
            match["full_name"] = el[0].encode("utf-8")
            match["uri"] =  el[1]
            match["simPM"] = el[2]
            matchings.append(match)
    """
    
    for m in mappings_scores_sorted:
        print m


if __name__ == "__main__":
    main()