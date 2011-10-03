#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import string as string
import sys
import re
import pickle
import MySQLdb

mappings = {}

def query():
    conn = MySQLdb.connect(host="agatha.inesc-id.pt", user="publico", passwd="publ1c0", db="wikipedia", charset="utf8", use_unicode="True")
    
    SQL = """SELECT page_title, CONVERT( en_text.old_text USING utf8) FROM en_page, en_revision, en_text WHERE page_title LIKE '%(disambiguation)%' AND en_page.page_id = en_revision.rev_page AND en_text.old_id = en_revision.rev_text_id"""
    
    try:        
        cursor = conn.cursor()    
        status = cursor.execute(SQL)
        rows = cursor.fetchall()
    
        for r in rows:
            redirects_title_pages = extract_hypertext_anchors(r[1])
            for t in redirects_title_pages:
                
                """
                TODO:
                    redirect can also be anchor text: 
                    "USS Birmingham (CL-2)|USS ''Birmingham'' (CL-2)"
                """
                normalize(r[0],t)
        
        conn.close()
    
    except Exception, e:
        print e
        conn.close()


def extract_hypertext_anchors(wiki_text):
    
    title_pages = []
        
    lines = wiki_text.split("\n")
    
    for l in lines:        
        if l.startswith("==See also=="):
            """
            After this the other links are to further disambiguation pages, not relevent at this point
            """
            break
        
        else:
        
            regex = re.compile("(?<=(\[\[)).*?(?=\]\])")
            r = regex.search(l)
            if r is not None:
                if not re.match("[a-z][a-z][a-z]?|(Portal)|(Category):", r.group()):
                    title_pages.append(r.group())
    
    return title_pages


def normalize(disambiguation,anchor):
    
    disambiguation_under_scores_removed = re.sub(r'_|\s+',' ', disambiguation.lower())
    disambiguation_normalized = re.sub(r'\(.*\)', '', disambiguation_under_scores_removed)

    try:
        mappings[disambiguation_normalized].add(anchor)
        
    except:
        mappings[disambiguation_normalized] = set()
        mappings[disambiguation_normalized].add(anchor)


def dump():
    output = open('disambiguation_pages.pkl', 'wb')
    pickle.dump(mappings, output)
    output.close()


def read():
    pkl_file = open('disambiguation_pages.pkl', 'rb')
    mappings = pickle.load(pkl_file)
    return mappings
    pkl_file.close()


def main():
    query()
    dump()
    print len(mappings)
    
       
if __name__ == "__main__":
    main()