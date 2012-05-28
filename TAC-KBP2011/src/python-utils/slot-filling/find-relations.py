#!/usr/bin/python

import re
import sys
import os
import MySQLdb
import fileinput

def find_relations_sentences(file,args):
    
    for l in fileinput.input(file):
        parts = l.split()
        id = parts[0]
        arg1 = parts[1].encode("utf8").upper().strip()
        arg2 = parts[2].encode("utf8").upper().strip()
    
        for arg in args:            
            if (arg == arg1) or (arg == arg2):
                print l.strip()

def load_args(file):
    
    args = []
    
    for l in fileinput.input(file):
        args.append(l.strip().upper())
        
    return args


def main():
        
    args = load_args(sys.argv[2])
    find_relations_sentences(sys.argv[1],args)

    conn.close()

if __name__ == "__main__":
    main()