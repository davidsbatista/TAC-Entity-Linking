#!/usr/bin/env python
# -*- coding: utf-8 -*-

import fileinput
import sys
import math

"""
 #!/opt/python2.6/bin/python2.6
 it calculates the kullback-leibler-divergence between two topics distribution output from GibbsLDA++
"""

def main():
    data = sys.argv[1]
    read_distribution(data)

def read_distribution(data):
    list = []
    i = 0.0
    for line in fileinput.input(data):
        topics_disturbution = line.rstrip().split(" ")
        for d in topics_disturbution:
            list.append(float(d))
            i += float(d)
    
    print i

if __name__ == "__main__":
    main()