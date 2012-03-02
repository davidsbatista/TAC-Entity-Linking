#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys

queries = []

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
                    query.append(int(line_parts[i].split(':')[1]))
                elif i==26:
                    continue
                else:
                    query.append(float(line_parts[i].split(':')[1]))
                
            queries.append(query)            

def find_min_max():
    for q in queries:
        for i in range(0, len(q)):
            
            if q[i]>max_values[i]:
                max_values[i]=q[i]
            
            if q[i]<min_values[i]:
                min_values[i]=q[i]
    
    """
    print "\nmax values:"
    for i in range(0, len(max_values)):
        print max_values[i],
         
    print "\nmin values:"
    for i in range(0, len(min_values)):
        print min_values[i],
    """    

def normalize():
    """
    Rescaling:
        
        The simplest method is rescaling the range of features to make the features independent of each other and aims to scale the range in [0, 1] 
        or [-1, 1]. Selecting the target range depends on the nature of the data. The general formula is given as:
        
        x' = (x - min) / (max - min)
        #q[i] = (q[i] - min_values[i]) / (max_values[i] - min_values[i])
    """
    for q in queries:
        
        for i in range(2, 26):
            
            if q[i] == 0 or max_values[i]== 0:
                continue
            else:
                q[i] = q[i] / max_values[i]

def output():
    for q in queries:
        for i in range(0, 26):
            if i==0:
                print q[i],
            elif i==1:
                print 'qid:'+str(q[i]),
            elif i==25:
                print str(i-1)+':'+str(q[i])
            else:
                print str(i-1)+':'+str(q[i]),
            
    #0 qid:5 1:0.062410748386342904 2:0.0 3:0.0 4:0.0 5:0.0 6:2.0202872418794926 7:0.0 8:0.33104298008569477 9:0.0 10:0.0 11:0.0 12:0.0 13:0.0 14:0.0 15:0.0 16:0.0 17:0.0 18:0.0 19:0.0 20:
#0.0 21:0.0 22:0.0 23:8398.0 24:0.0

def main():
    for i in range(0, 26):
        global max_values
        max_values.append(int(0))
        global min_values
        min_values.append(int(10000))
    
    load(sys.argv[1])
    find_min_max()
    normalize()
    output()
   
if __name__ == "__main__":
    main()