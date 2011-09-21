#!/opt/python2.6/bin/python2.6
# -*- coding: utf-8 -*-

import os
import sys
import fileinput
import math
import random

from subprocess import Popen, PIPE

n_topics = 200
num_entities = 818741

gold_standard = []


def copy_data():
    for line in fileinput.input(sys.argv[1]):
        parts = line.split("\t")
        query = parts[0].strip()
        correct_candidate = parts[1]        
        id = int(parts[1].split("E")[1].strip())

        """
        get the topics distribution for the correct_candiate:
        the line number in the file corresponds to the entity ID 
        """
        
        command = "head -n " + str(id-1) + " /collections/TAC-2011/trained_model_200_iterations/model-final.theta | tail -n 1"
        p = Popen(command,shell=True,stdout=PIPE,stderr=PIPE)
        output, stderr_output = p.communicate()
        print stderr_output
        
        print correct_candidate
        
        f = open("KB_entities/"+str(id)+".theta", 'w')
        f.write(output)
        f.close()
        
        """
        get the topics distributions for the query document: 
        (.theta file in the "/collections/TAC-2011/lda_distribution_queries/parts[0]" directory)
        """
        
        """
        dir = "/collections/TAC-2011/lda_distribution_queries/"+ query        
        command = "mkdir queries/" + query + ";cp " + dir + "/*.theta " + "queries/" + query + "/"
        p = Popen(command,shell=True,stdout=PIPE,stderr=PIPE)
        output, stderr_output = p.communicate()
        print stderr_output
        """


def load_gold_standard():
    for line in fileinput.input(sys.argv[1]):
        parts = line.split("\t")
        query = parts[0].strip()        
        id = parts[1].split("E")[1].strip()        
        gold_standard.append((query,id))


def calculate_divergence_gold_standard():
    
    for e in gold_standard:        
        query = e[0]        
        id = e[1]
        
        correct_entity = "/collections/TAC-2011/find-kl-divergence-with-correct-candidate/KB_entities/"+str(int(id))+".theta"
        document_dir = "/collections/TAC-2011/find-kl-divergence-with-correct-candidate/queries/"+query+"/"

        dirList = os.listdir(document_dir)
        document_file = document_dir + dirList[0]
        
        document = []
        correct = []
        
        read_distribution(document_file,document)    
        read_distribution(correct_entity,correct)

        divergence = kl_divergence(correct, document)
        
        print query, id, divergence


def calculate_divergence_random_entity():
    for e in gold_standard:
        """
        for each query, get the topics distribution for an entity from the KB which is not the correct one
        """
        query = e[0]
        random_entity = int(random.random()*num_entities)
        while random_entity == int(e[1]):
            random_entity = int(random.random()*num_entities)
                
        command = "head -n " + str(random_entity-1) + " /collections/TAC-2011/trained_model_200_iterations/model-final.theta | tail -n 1"
        p = Popen(command,shell=True,stdout=PIPE,stderr=PIPE)
        output, stderr_output = p.communicate()
        
        kb_entity = []        
 
        topics_disturbution =  output.rstrip().split(" ")
        for d in topics_disturbution:
            kb_entity.append(float(d))
                    
        """
        get the topics distribution for the query's document support
        """
        document_dir = "/collections/TAC-2011/find-kl-divergence-with-correct-candidate/queries/"+ query + "/"
        dirList = os.listdir(document_dir)
        document_file = document_dir + dirList[0]
        document = []
        
        read_distribution(document_file,document)
    
        """
        calculate the KL-Divergence
        """
        divergence = kl_divergence(document,kb_entity)
        print query, random_entity, divergence 
    

def read_distribution(data,list):
    i = 0
    for line in fileinput.input(data):
        topics_disturbution =  line.rstrip().split(" ")
        for d in topics_disturbution:
            list.append(float(d))


def kl_divergence(p,q):
    divergence = 0.0
    for i in range(int(n_topics)):
        divergence += p[i] * ( math.log10(p[i]) - math.log10(q[i]) )
    return divergence
 
       
def main():
    #copy_data()
    load_gold_standard()
    #calculate_divergence_gold_standard()
    calculate_divergence_random_entity()

if __name__ == "__main__":
    main()
    
