#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.dom.minidom
import MySQLdb

import sys,os
import fileinput

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
    
    insertBD(cursor,entities)
        
def insertBD(cursor,entities):
    
    sql = "INSERT INTO entity (kb_id, type, name) VALUES (%s,%s,%s)"    
    status = cursor.executemany(sql,entities)
    print status



def calculate(cursor):
    sql = "SELECT * FROM entity"
    status = cursor.execute(sql)
    print status
         
    rows = cursor.fetchall()

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
        words_list.append((word.decode("utf-8"),words_frequency[word],
                           math.log(1/(float(words_frequency[word])/float(maxFreq)))))
            
    sql = "INSERT INTO word (word,frequency,wordEvidenceContent) VALUES (%s,%s,%s)"
    insertBD(cursor,sql,words_list)





def main():
    conn = MySQLdb.connect (host = "agatha.inesc-id.pt", user = "publico", passwd = "publ1c0", db = "TAC-KB", use_unicode="True", charset="utf8")
    cursor = conn.cursor()
    
    filelist = os.listdir(sys.argv[1])
    filelist.sort()
    for f in filelist:
        if os.path.isdir(f) != True and f.split(".")[1] == 'xml':
            parse_doc(f,cursor)
        
if __name__ == "__main__":
    main()
    