#!/usr/bin/python

import re
import sys
import os
import MySQLdb
import fileinput

def find_relations_based_on_args(cursor,arg1,arg2,output):
       
    SQL = """
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction1 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction2 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction3 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction4 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction5 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction6 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction7 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction8 
            WHERE (arg1 = '%s' AND arg2 = '%s')
            GROUP BY arg1,rel,arg2 ORDER BY occurrences DESC;
          """ % (arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,arg1,arg2,)
    
    cursor.execute(SQL)
    rows = cursor.fetchall()
    
    f = open(output,'a')
    
    for r in rows:
        if r[3] == 0:
            continue 
        f.write(r[0].encode("utf8")+'\t'+r[1].encode("utf8")+'\t'+r[2].encode("utf8")+'\t'+str(r[3])+'\n')
    
    f.close();


def load_pronouns(file):
    
    pronouns = []
    
    for l in fileinput.input(file):
        pronouns.append(l.upper().strip())
        
    return pronouns


def find_relations_seed(cursor,seed,output):
        
    SQL = """
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction1 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')            
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction2 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction3 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction4 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction5 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction6 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences
            FROM extraction7 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            UNION
            SELECT arg1, rel, arg2, count(*) AS occurrences 
            FROM extraction8 
            WHERE (rel LIKE '%s' AND arg1 REGEXP BINARY '^[A-Z][a-z]+$' AND arg2 REGEXP BINARY '^[A-Z][a-z]+$')
            GROUP BY arg1, rel, arg2
            ORDER by occurrences DESC;
           """ % (seed,seed,seed,seed,seed,seed,seed,seed)
    
    cursor.execute(SQL)
    rows = cursor.fetchall()
    
    f = open(output,'w')
    
    results = []
    
    f.write("relation:\t"+seed+'\n')
    
    for r in rows:
        f.write(r[0].encode("utf8")+'\t'+r[1].encode("utf8")+'\t'+r[2].encode("utf8")+'\t'+str(r[3])+'\n')
        results.append((r[0],r[1],r[2]))
    
    f.close();

    return results;


def find_relations(file):
    for line in fileinput.input(file):
        parts = line.strip('\t')
        find_relations_based_on_args()
        


def main():
    
    conn = MySQLdb.connect (host = "borat", user = "root", passwd = "07dqeuedm", db = "reverb-extractions", use_unicode="True", charset="utf8")
    cursor = conn.cursor()
    
    seed = sys.argv[1]
    output_args = sys.argv[2]
    output_rel = sys.argv[3]    
    pronouns = load_pronouns("list_of_personal_pronouns.txt")
    
        
    print "looking for relations based on: ", seed
    results = find_relations_seed(cursor,seed,output_args)
    
    print len(results), "found"

    for r in results:
        
        #if it is a personal pronoun, skip it
        if r[0].encode("utf8").upper() in pronouns or r[1].encode("utf8").upper() in pronouns:
            continue 
        
        else:
            print "looking for relations matching: ", r[0],"?",r[2]
            find_relations_based_on_args(cursor,r[0],r[2],output_rel)
    
    conn.close()


if __name__ == "__main__":
    main()