#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
adapted from: 
http://stackoverflow.com/questions/120656/directory-listing-in-python
"""

import os,sys

def main():
    
    for dirname, dirnames, filenames in os.walk(sys.argv[1]):
            
        for filename in filenames:
            
            #print only  the .sgm from filename            
            filename_parts = filename.split(".")
            
            if filename_parts[-1] == 'sgm':            
                print filename, dirname
        
if __name__ == "__main__":
    main()