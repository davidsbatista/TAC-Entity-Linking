#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys
import math

"""
 #!/opt/python2.6/bin/python2.6
 it calculates the kullback-leibler-divergence between two topics distribution output from GibbsLDA++
"""

n_topics = sys.argv[1]
n_candidates = sys.argv[2]


def main():
    candidates_doc = [[None] * int(n_topics) for i in range(int(n_candidates))]    
    document = []

    read_distribution(sys.argv[3],document,False)
    
    print document
    print len(document)
    
    read_distribution(sys.argv[4],candidates_doc,True)
    
    print "candidates: ", len(candidates_doc)
    print "n_topics: ", len(document)
    
    kl_divergence(document, candidates_doc)



def read_distribution(file,list,candidates):
    i = 0
    for line in fileinput.input(file):
        topics_disturbution =  line.rstrip().split(" ")
        
        if not candidates:
            for d in topics_disturbution:
                list.append(float(d))

        else:
            z = 0
            for d in topics_disturbution: 
                list[i][z] = (float(d))
                z+=1                
            i+=1



def kl_divergence(p,q):
    lowest = 1
    topic = -1
    for z in range(int(n_candidates)):
        sum = 0
        for i in range(int(n_topics)):
            sum += p[i] * ( math.log10(p[i]) - math.log10(q[z][i]) )
            print p[i], q[z][i], sum
        
        if sum <= lowest:
            lowest = sum
            topic = z    
    
    """
    normalization: 1−exp(−Divergence K-L)
    normalized = 1 - math.exp(lowest-lowest*2)
    """
    
    print "divergence: ", lowest 


if __name__ == "__main__":
    main()