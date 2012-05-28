#!/usr/bin/python

import re
import sys
import os
import MySQLdb
import fileinput

freebase_date_of_birth = []
freebase_nationality = []
freebase_children = []
freebase_parents = []
freebase_profession = []
freebase_date_of_death = []    
freebase_place_of_death = []
freebase_place_lived = []
freebase_cause_of_death = []

def get_freebase_relations(table,field1,field2,cursor):
    
    SQL = """SELECT %s, %s FROM person WHERE %s!='' AND %s !='' ORDER BY name""" % (field1,field1,field2,field2)
    print SQL
    cursor.execute(SQL)
    rows = cursor.fetchall()
    
    print len(rows),field," relations found"
    relations = []
    
    for r in rows:
        r = (r[0].encode("utf8").lower().strip(),r[1].encode("utf8").lower().strip())
        relations.append(r)

    return relations

def find_relations_sentences(relation,table,pronouns,freebase_relations,stopwords,cursor):
    
    SQL = """SELECT id,normalized_arg1,normalized_arg2 FROM %s WHERE normalized_arg1!='' and normalized_arg2!=''""" % table
    cursor.execute(SQL)
    rows = cursor.fetchall()
    
    # buffer size to 0, that means unbuffered, so that results are immediately written to file, the process takes long hours to run
    # so every time a result is found we can see it by looking at the file while the program keeps running    
    bufsize = 0
    f = open(table+'_'+relation+'.txt', 'w',bufsize)
    
    print len(rows)," relations sentences found"    
    count = 0
    
    for reverb_relation in rows:
        
        #print "searching matches for reverb extraction: ",(r_arg1,r_arg2), "(",count,"/",len(rows),")"
        print "progress: ",count,"/",len(rows)
            
        # skip relations constructed where the args are personal pronouns
        if (reverb_relation[1] in pronouns) or (reverb_relation[2] in pronouns):
            continue
        
        else:
            r_arg1 = reverb_relation[1].encode("utf8").lower().strip()
            r_arg2 = reverb_relation[2].encode("utf8").lower().strip()
            
            for freebase_relation in freebase_relations:                
                f_arg1 = freebase_relation[0]
                f_arg2 = freebase_relation[1]
                
                if (r_arg1 == f_arg1 and r_arg2 == f_arg2) or ((r_arg1 == f_arg2 and r_arg2 == f_arg1)):
                    f.write(str(reverb_relation[0])+'\t'+reverb_relation[1].encode("utf8")+'\t'+reverb_relation[2].encode("utf8")+'\n');                
    
        count+=1
    f.close()

def find_relations_sentences_names(freebase_names,freebase_full_names,table,pronouns,cursor):
    
    SQL = """SELECT id,normalized_arg1,normalized_arg2 FROM %s WHERE normalized_arg1!='' and normalized_arg2!=''""" % table
    
    cursor.execute(SQL)
    rows = cursor.fetchall()
    
    # buffer size to 0, that means unbuffered, so that results are immediately written to file, the process takes long hours to run
    # so every time a result is found we can see it by looking at the file while the program keeps running    
    bufsize = 0
    f = open(table+'.txt', 'w',bufsize)
    
    print len(rows)," relations sentences found"    
    count = 0
    
    for reverb_relation in rows:
            
        # skip relations constructed where the args are personal pronouns
        if (reverb_relation[1] in pronouns) or (reverb_relation[2] in pronouns):
            count+=1
            continue
        
        else:
            
            r_arg1 = reverb_relation[1].encode("utf8").upper().strip()
            r_arg2 = reverb_relation[2].encode("utf8").upper().strip()
            
            print "searching matches for reverb extraction: ",(r_arg1,r_arg2), "(",count,"/",len(rows),")",
            
            if (r_arg1 in freebase_full_names or r_arg2 in freebase_full_names):
                print "\t\tMATCH"
                f.write(str(reverb_relation[0])+'\t'+reverb_relation[1].encode("utf8")+'\t'+reverb_relation[2].encode("utf8")+'\n');
            else:
                print "\n"
            count+=1
    
    f.close()           

def get_freebase_names(file):
    
    freebase_names = []
    
    for l in fileinput.input(file):
        freebase_names.append(l.strip().upper())
        
    return freebase_names

def load_stopwords(file):
    
    stopwords = []
    
    for l in fileinput.input(file):
        stopwords.append(l.upper().strip())
        
    return stopwords

def load_pronouns(file):
    
    pronouns = []
    
    for l in fileinput.input(file):
        pronouns.append(l.upper().strip())
        
    return pronouns

def freebase_relations_tokenized(freebase_relations,stopwords):
    
    freebase_relations_tokenized = []
    
    for freebase_relation in freebase_relations:
        
        f_arg1 = freebase_relation[0].encode("utf8").lower().strip()
        f_arg2 = freebase_relation[1].encode("utf8").lower().strip()
                
        # tokenize relation and remove stopwords
        f_arg1_tokens = set(f_arg1.split(" "))
        f_arg2_tokens = set(f_arg2.split(" "))
                
        f_arg1_tokens = f_arg1_tokens.difference(stopwords)
        f_arg2_tokens = f_arg2_tokens.difference(stopwords)
                
        freebase_relations_tokenized.append((f_arg1_tokens,f_arg2_tokens))

    return freebase_relations_tokenized

def main():
    
    conn = MySQLdb.connect (host = "borat", user = "root", passwd = "07dqeuedm", db = "reverb-extractions", use_unicode="True", charset="utf8")
    cursor = conn.cursor()
    
    pronouns = load_pronouns("list_of_personal_pronouns.txt")
    stopwords = load_stopwords("stopwords.txt")
    
    #table = sys.argv[1]
    #relation = sys.argv[2]
    
    # table: 'person'
    table = 'person'
    global freebase_place_of_birth
    freebase_place_of_birth = get_freebase_relations(table,"name","place_of_birth",cursor)    
    
    global freebase_date_of_birth
    freebase_date_of_birth = get_freebase_relations(table,"name","date_of_birth",cursor)
    
    global freebase_nationality
    freebase_nationality = get_freebase_relations(table,"name","nationality",cursor)
    
    global freebase_children
    freebase_children = get_freebase_relations(table,"name","religion",cursor)
    
    global freebase_parents
    freebase_parents = get_freebase_relations(table,"name","parents",cursor)
    
    global freebase_profession 
    freebase_profession = get_freebase_relations(table,"name","profession",cursor)
    
    
    # table: 'deceased_person'
    table = 'deceased_person'
    global freebase_date_of_death
    freebase_date_of_death = get_freebase_relations(table,"name","date_of_death",cursor)
    
    global freebase_place_of_death
    freebase_place_of_death = get_freebase_relations(table,"name","place_of_death",cursor)
        
    # table: place_lived
    table = 'place_lived'
    global freebase_place_lived
    freebase_place_lived = get_freebase_relations(table,"name","location",cursor)
    
    # table: cause_of_death
    table = 'cause_of_death'
    global freebase_cause_of_death
    freebase_cause_of_death = get_freebase_relations(table,"name","cause_of_death",cursor)
    
    # table: appointment
    table = 'appointment'
    global freebase_appointment
    freebase_appointment = get_freebase_relations(table,"appointee","appointed_role",cursor)
    
    # table: profession
    table = 'profession'
    global freebase_profession
    freebase_profession = get_freebase_relations(table,"name","profession",cursor)
    
    # table: academic_post
    table = 'academic_post'
    global freebase_academic_post
    freebase_academic_post = get_freebase_relations(table,"person","position_or_title",cursor)
    
    # table: government_position_held
    table = 'government_position_held'
    global freebase_government_position_held
    freebase_government_position_held = get_freebase_relations(table,"name","office_position_or_title",cursor)
    
    # table: employment_tenure
    table = 'employment_tenure'
    global freebase_employment_tenure
    freebase_employment_tenure = get_freebase_relations(table,"person","company",cursor)
    
    # table: organization_founder
    # needs parsing
    table = 'organization_founder'
    global freebase_employment_tenure
    freebase_employment_tenure = get_freebase_relations(table,"name","organizations_founded",cursor)
    
    
       
    
    # table: marriage
    # table: siblings
    
    
    #find_relations_sentences(relation,table,pronouns,freebase_relations,set(stopwords),cursor)
    
    conn.close()

if __name__ == "__main__":
    main()