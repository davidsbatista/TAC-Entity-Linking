#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import fileinput
import sys,os
import operator

from collections import defaultdict

#gold_standard = dict()
answers = defaultdict(list)

def main():

    # load gold-standard
    for line in fileinput.input(sys.argv[2]):
        parts = line.split("\t")
        gold_standard[parts[0].strip()] = parts[1].strip()
    
    dirList=os.listdir(sys.argv[1])
    dirList.sort()
    for fname in dirList:
        if fname.endswith("no_topics"):
            #print fname.split("_")[0],
            results=os.listdir(sys.argv[1]+"/"+fname)
            if len(results)>0:
                for r in results:
                    #print "\t"+r.split("_")[1].split(".txt")[0]
                    answers[fname.split("_")[0]].append(r)
            else:
                answers[fname.split("_")[0]].append("NIL")
    
    answers_keys = answers.keys()
    
    #print "processed queries: ", len(answers_keys)
    
    correct = 0
    
    for a in answers_keys:
        if len(answers[a])>1:
            for file in answers[a]:
                kb_id = file.split("_")[1].split(".txt")[0]                
                if str(kb_id).strip() == str(gold_standard[a]).strip():
                    correct += 1
                    break;
 
    #print "nº queries with correct answer in the retrieved documents: ", correct
    
    """
    for fname in dirList:
        if fname.endswith("no_topics"):
            positions(sys.argv[1]+"/"+fname)
    """
    
def positions(path):
    
    scores = dict()
    dirList=os.listdir(path)
    for file in dirList:
        f = open(path+"/"+file)
        data = f.readlines() 
        score = data[0]
        kb_id = file.split("_")[1].split(".txt")[0]
        scores[kb_id] = float(score.strip()) 
        f.close()
    
    sorted_scores = sorted(scores.iteritems(), key=operator.itemgetter(1), reverse=True)
    query_id = path.split("_")[0].strip().split("/")[1]
    correct_kb_id = gold_standard[query_id]
    
    for t in sorted_scores:
        if t[0] == correct_kb_id: 
            print query_id, sorted_scores.index(t)  
            break;

if __name__ == "__main__":
    main()
    
    
    
    
    
    
    
    
    
    