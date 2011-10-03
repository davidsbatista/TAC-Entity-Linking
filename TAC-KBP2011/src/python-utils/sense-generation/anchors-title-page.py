#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys
import re
import pickle
import MySQLdb
import string as string

mappings = {}

def read_anchors():
    for line in fileinput.input():

        anchors_parts = line.split("|")
        
        title_page = anchors_parts[0].strip("[[").strip("\n")
        anchor_link = anchors_parts[1].strip("]]\n")
        
        normalize(title_page,anchor_link)


def normalize(title,redirect):
    
    title_under_scores_removed = re.sub(r'_|\s+',' ', title.lower())
    title_normalized = re.sub(r'\(.*\)', '', title_under_scores_removed)
    
    redirect_under_scores_removed = re.sub(r'_|\s+',' ', redirect.lower())
    redirect_normalized = re.sub(r'\(.*\)', '', redirect_under_scores_removed)

    try:
        mappings[redirect_normalized].add(title)
        
    except:
        mappings[redirect_normalized] = set()
        mappings[redirect_normalized].add(title)


def dump():
    output = open('anchors_to_title_page.pkl', 'wb')
    pickle.dump(mappings, output)
    output.close()

def read():
    pkl_file = open('anchors_to_title_page.pkl', 'rb')
    mappings = pickle.load(pkl_file)
    return mappings
    pkl_file.close()

def main():
    read_anchors()
    dump()
   
if __name__ == "__main__":
    main()