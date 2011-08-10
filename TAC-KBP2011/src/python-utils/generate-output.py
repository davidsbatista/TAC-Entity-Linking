#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys,os

def main():
    """
    for line in fileinput.input():
        if line.startswith("EL"):
            print line.split("_")[0],
        if line.startswith("lucene"):
            print "\t"+line.split("_")[1].split(".txt")[0]
        else:
            print "\tNIL\n"

    for dirname, dirnames, filenames in os.walk(sys.argv[1]):
            
        for filename in filenames:            
            filename_parts = filename.split(".")
            print filename_parts
    """
        
    dirList=os.listdir(sys.argv[1])
    for fname in dirList:
        if fname.endswith("no_topics"):
            print fname.split("_")[0],
            result=os.listdir(sys.argv[1]+"/"+fname)
            if len(result)>0:
                print "\t"+result[0].split("_")[1].split(".txt")[0]
            else:
                print "\tNIL"

        
if __name__ == "__main__":
    main()