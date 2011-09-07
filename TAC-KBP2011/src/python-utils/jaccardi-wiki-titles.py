#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.dom.minidom
import MySQLdb
import math
import sys,os
import fileinput

from operator import itemgetter

frequencies = dict()


def parse_doc(filename,cursor):
    entities = []
    print filename
    xmldoc = xml.dom.minidom.parse(sys.argv[1]+"/"+filename)
    items = xmldoc.getElementsByTagName('entity')
        
    for i in items:
        id = i.getAttribute("id")
        type = i.getAttribute("type")
        name = i.getAttribute("name")
        
        e = (id, type, name)
        entities.append(e)
    
    sql = "INSERT INTO entity (kb_id, type, name) VALUES (%s,%s,%s)"
    insertBD(cursor,sql,entities)
        
def insertBD(cursor,sql,list):
        
    status = cursor.executemany(sql,list)
    print status

def calculate(cursor):
    
    sql = "SELECT * FROM entity"
    status = cursor.execute(sql)
    print status
         
    rows = cursor.fetchall()
    words_frequency = dict()

    for row in rows:
        name_parts = row[3].encode("utf-8").split()
        for word in name_parts:
            try:
                words_frequency[word.lower()] += 1
            except:
                words_frequency[word.lower()] = 1


    size = math.log(len(words_frequency))
    words_list = list()
            
    sorted_words_frequency = sorted(words_frequency.items(), key=itemgetter(1), reverse=True)
    maxFreq = sorted_words_frequency[0][1]
    
    wordEC = dict()
            
    for word in words_frequency:
        wordEC[word] = math.log(1/(float(words_frequency[word])/float(maxFreq)))
        words_list.append((word.decode("utf-8"),words_frequency[word],math.log(1/(float(words_frequency[word])/float(maxFreq)))))
   
    sql = "INSERT INTO word (word,frequency,evidence_content) VALUES (%s,%s,%s)"
    insertBD(cursor,sql,words_list)


def calculateIC(cursor):
    sql = "SELECT * FROM entity"
    status = cursor.execute(sql)
    print status
    rows = cursor.fetchall()
    
    sql = "SELECT word,evidence_content FROM word"
    status = cursor.execute(sql)
    print status
    wordEC = cursor.fetchall()
    
    wordECdict = dict(wordEC)
    
    keys = wordECdict.keys()
    keys.sort()
            
    """
    w = "Əsədabad"
    print wordECdict[w.decode("utf8")]
    """
    
    namesEC = list()
    
    for row in rows:
        name_parts = row[3].split()
        nameEC = 0
        for word in name_parts:
            try:
                #print type(word)
                nameEC += wordECdict[word.lower()]
            except:
                print word
        
        namesEC.append((row[0],nameEC))
        
    sql = "INSERT INTO entity_Ec (kb_id,nameEC) VALUES (%s,%s)" 
    print "status: ", insertBD(cursor,sql,namesEC)


def load(cursor):
    filelist = os.listdir(sys.argv[1])
    filelist.sort()
    for f in filelist:
        if os.path.isdir(f) != True and f.split(".")[1] == 'xml':
            parse_doc(f,cursor)


def main():
    conn = MySQLdb.connect (host = "agatha.inesc-id.pt", user = "publico", passwd = "publ1c0", db = "TAC-KB", use_unicode="True", charset="utf8")
    cursor = conn.cursor()
    
    #load(cursor)
    #calculateWC(cursor)
    calculateIC(cursor)
    
    conn.close()

    
if __name__ == "__main__":
    main()
    