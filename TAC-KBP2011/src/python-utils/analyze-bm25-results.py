#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.dom.minidom

import sys
import fileinput
import time

from datetime import datetime


class Results:
    
    K1 = None
    B = None
    total = 0.0

    def __init__(self,K1,B):
        self.K1 = K1
        self.B = B



def parse_results(file):
    
    overall_score = 0.0
    
    for line in fileinput.input(file):
        
        if len(line) == 1:
            continue
               
        try:
            parameter,value = line.split(":")
            
            if parameter=="K1":
                K1 = value.strip("\n")
                
            elif parameter=="B":
                B = value.strip("\n")
            
        except:
            query_id,score = line.split("\t")
            overall_score += float(score)

    print "K1: "+str(K1)+"\tB: "+str(B)+"\tscore: "+str(overall_score)    
 
    
def main():
    parse_results(sys.argv[1])

        
if __name__ == "__main__":
    main()