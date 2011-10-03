#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys
import re
import MySQLdb
import pickle
import string as string

mappings = {}

def query():
    conn = MySQLdb.connect(host="agatha.inesc-id.pt", user="publico", passwd="publ1c0", db="wikipedia", charset="utf8", use_unicode=True)
    
    SQL = """SELECT page_title, rd_title FROM en_redirect, en_page WHERE en_redirect.rd_from = en_page.page_id"""
    
    cursor = conn.cursor()    
    status = cursor.execute(SQL)
    rows = cursor.fetchall()
    
    for r in rows:        
        normalize(r[0],r[1])
        
    conn.close()

def normalize(title,redirect):
    
    title_under_scores_removed = re.sub(r'_|\s+',' ', title.lower())
    title_normalized = re.sub(r'\(.*\)', '', title_under_scores_removed)
    
    redirect_under_scores_removed = re.sub(r'_|\s+',' ', redirect.lower())
    redirect_normalized = re.sub(r'\(.*\)', '', redirect_under_scores_removed)
    

    try:
        mappings[title_normalized].add(title)
        
    except:
        mappings[title_normalized] = set()
        mappings[title_normalized].add(title)


    try:
        mappings[redirect_normalized].add(redirect)
        
    except:
        mappings[redirect_normalized] = set()
        mappings[redirect_normalized].add(redirect)


def dump():
    output = open('normalized_articles_and_redirects.pkl', 'wb')
    pickle.dump(mappings, output)
    output.close()

def read():
    pkl_file = open('normalized_articles_and_redirects.pkl', 'rb')
    mappings = pickle.load(pkl_file)
    return mappings
    pkl_file.close()


def main():
    query()
    dump()
    
    """
    mappings = read()
    for m in mappings:
        if len(mappings[m])>1:
            print m, mappings[m]
    """

if __name__ == "__main__":
    main()