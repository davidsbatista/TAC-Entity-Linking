#!/usr/bin/python

import re
import sys
import os

def readFile(file):
    
    hashtable = dict()
    
    for line in open(file):
        d = re.split("\s+", line.strip())
        query = d[0].upper().strip()
        entity = d[1].upper().strip()
        
        hashtable[query] = entity

    return hashtable

if __name__ == "__main__":
    
    system = readFile(sys.argv[1])
    goldStd = readFile(sys.argv[2])
    
    correct = 0
    
    for k in goldStd.keys():
        #print k, system[k], goldStd[k]
        if system[k] == goldStd[k]:
            correct += 1

    accuracy = float(correct) / float(len(goldStd.keys()))

    print "Total Correct: ", correct
    print "Total Queries: ", len(goldStd.keys())
    print "Accuracy: ", accuracy*100
