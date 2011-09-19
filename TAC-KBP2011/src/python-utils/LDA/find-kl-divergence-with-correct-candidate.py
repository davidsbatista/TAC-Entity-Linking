#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import os
import sys
import fileinput

def main():
    for line in fileinput.input(sys.argv[1]):
        parts = line.split("\t")
        query = parts[0]
        correct_candidate = parts[1]
        
        
        """
         get the topics distribution for the correct_candiate:
         it's the big file: KB_one_file_documents_rare_words_removed.txt.theta
         
         get the line number:
         head -n parts[1] filename.php | tail -n 1
                
         get the topics distributions for the query document: 
         (.theta file in the "/collections/TAC-2011/lda_distribution_queries/parts[0]" directory)
         
         compare both with the kullback-leiber-divergence.py
        """        

if __name__ == "__main__":
    main()