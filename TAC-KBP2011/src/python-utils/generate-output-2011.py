#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import os,sys

def main():    
    dirList=os.listdir(sys.argv[1])
    dirList.sort()
    for fname in dirList:
        if fname.endswith("no_topics"):
            results=os.listdir(sys.argv[1]+"/"+fname)
            if len(results)>0:
                for r in results:
                    print fname.split("_no_topics")[0], r.split("_")[1].split(".txt")[0]
            else:
                print fname.split("_no_topics")[0],"NIL0000"

if __name__ == "__main__":
    main()
    
    
    
    
    
    
    
    
    
    