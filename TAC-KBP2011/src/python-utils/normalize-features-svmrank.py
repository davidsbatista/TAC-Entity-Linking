#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys

queries_train = []

max_values = []
min_values = []

def load(file):
    for line in fileinput.input():
        if line.startswith('#'):
            # get the query id (EL_XXXX) and the answer (NILXXX or EXXXX)
            """
            line_parts = line.split(' ')
            query = line_parts[0]
            answer = line_parts[1]            
            print query,answer,
            """
            continue
        
        else:
            """
            get:
                - all the features
                - candiadte entity
            """
            line_parts = line.split(' ')            
            query = []
            
            for i in range(0, len(line_parts)):
                if i==0:
                    # True or False
                    query.append(line_parts[i])
                elif i==1:
                    # Query ID
                    query.append(line_parts[i].split(':')[1])
                elif i==26:
                    continue
                else:
                    query.append(line_parts[i].split(':')[1])
                
            queries_train.append(query)            
    
    for q in queries_train:
        for i in range(0, len(q)):
            
            if q[i]>max_values[i]:
                max_values[i]=q[i]
            
            if q[i]<min_values[i]:
                print "true"
                min_values[i]=q[i]

    print "\nmax values:"
    for i in range(0, len(max_values)):
        print max_values[i],
         
    print "\nmin values:"
    for i in range(0, len(min_values)):
        print min_values[i],    



def main():
    for i in range(0, 26):
        global max_values
        max_values.append(0)
        global min_values
        min_values.append(10000)
    
    load(sys.argv[1])
   
if __name__ == "__main__":
    main()