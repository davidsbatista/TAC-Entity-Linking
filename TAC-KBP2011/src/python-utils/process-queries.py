#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os,sys
import re
import fileinput
import string
import StringIO

import nltk
import MySQLdb

import xml.dom.minidom
import httplib
import urllib
import simplejson

from BeautifulSoup import BeautifulSoup
from subprocess import Popen, PIPE

docs_locations = dict()
queries = []
wordmaps_lda = dict()

conn = MySQLdb.connect(host="agatha.inesc-id.pt", user="publico", passwd="publ1c0", db="wikipedia", charset="utf8", use_unicode=True)

lda_models = "/collections/TAC-2011/lda_models/"

class Query:
    
    id = None
    string_name = ''
    doc_id = None
    entity_kb_id = None
    entity_type = None
    
    alternative_names = None
    support_doc_persons = None
    support_doc_organizations = None
    support_doc_places = None
    support_doc_context_occurences = None

    def __init__(self,identifier,string,document):
        self.id = identifier
        self.string_name = string
        self.doc_id = document
        self.support_doc_persons = xml.dom.minidom.NodeList
        self.support_doc_organizations = xml.dom.minidom.NodeList
        self.support_doc_places = xml.dom.minidom.NodeList
        self.support_doc_context_occurences = []
        self.alternative_names = []

def remove_tags(data):
    p = re.compile(r'<.*?>|&quot;')
    return p.sub('', data)

def parse_doc(document):
    xmldoc = xml.dom.minidom.parse(document)
    items = xmldoc.getElementsByTagName('BODY')
    text = remove_tags(str(items[0].toxml()))    
    p = re.compile(r'\s+')
    return p.sub(' ',text)

def load_docs_locations(file):
    
    for line in fileinput.input(file):
        parts = line.split(".sgm");
        docs_locations[parts[0].strip()] = parts[1].strip()

def load_lda_wordmaps(file):
    
    for line in fileinput.input(file):
        parts = line.split(" ");
        if len(parts)>1:
            wordmaps_lda[parts[1].strip()] = parts[0].strip()

def parse_queries(file):
    
    try:
        xmldoc = xml.dom.minidom.parse(file)
        items = xmldoc.getElementsByTagName('query')
                    
        if len(items)>0:
            for item in items:                
                
                id = item.getAttribute("id")
                name =  item.childNodes[1].childNodes[0].nodeValue
                docid = item.childNodes[3].childNodes[0].nodeValue
                
                query = Query(id,name,docid)
                queries.append(query)
    
    except Exception, e:
        print "Error parsing queries"
        print e

def call_rembrandt(document):

    """ REMBRANDT API """
    slg_value = "en"
    lg_value = "en"
    rembrandt_key_value = "db924ad035a9523bcf92358fcb2329dac923bf9c"
    format = "dsb"

    params = urllib.urlencode({"db": document, "slg": slg_value, "lg": lg_value, "api_key": rembrandt_key_value, "f": format})
    headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain"}
    conn = httplib.HTTPConnection("agatha.inesc-id.pt:80")
    conn.request("POST", "/Rembrandt/api/rembrandt?", params, headers)
    
    response = conn.getresponse()
    
    """
    DEBUG
    print "\n"
    print response.msg
    print response.status 
    print response.reason
    """
        
    data = response.read()
    conn.close()
    
    try:
        JSONObject = simplejson.loads(data)
        return JSONObject["message"]["document"]["body"]
    
    except Exception, e:
        print e

def get_entities(query):
    
    """ call REMBRANDT over the support document """
    
    # let's get the document location
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    #file = open(docs_location+query.doc_id+".sgm")
    document = file.read()

    # do NER on the document
    rembrandted_document = call_rembrandt(document)
    
    if rembrandted_document:
          
        rembrandted_document_valid_xml = BeautifulSoup(rembrandted_document)
        
        doc = unicode(rembrandted_document_valid_xml)
    
        try:
            xmldoc = xml.dom.minidom.parseString(doc.encode("utf-8"))
            
            persons = None
            organizations = None
            places = None
                        
            persons = xmldoc.getElementsByTagName('person')
            organizations = xmldoc.getElementsByTagName('organization')
            places = xmldoc.getElementsByTagName('place')
            
            query.support_doc_persons = getDictWithKeysFromList(persons)
            query.support_doc_organizations = getDictWithKeysFromList(organizations)
            query.support_doc_places = getDictWithKeysFromList(places)
    
        except Exception, e:
            print "Error parsing XML returned by REMBRANDT"
            print e
            print rembrandted_document.encode("utf8")
        
def getDictWithKeysFromList(lst):
    
    d = {}
    
    for l in lst:
        
        try:
            d[l.firstChild.data.strip().rstrip()] = ""
        
        except Exception, e:
            print e
    
    return d

def getConcordance(text,word,offset):
    
    tokens = re.findall(u'\w+',text,re.U)
    listOfIndexes  = list(find_all_in_list( tokens,word))
    
    listOfConcordance = []
    
    for i in listOfIndexes:
        
        start = i - offset
        
        if start < 0:
            start = 0
        
        end = i + offset + 1
         
        if end > len(text):
            end = len(text)
        
        listOfConcordance.append(tokens[start:end])
    
    return listOfConcordance

def find_all_in_list(a_str, sub):
    
    start = 0
    
    while True:
         
        try:
            start = start + a_str[start:].index(sub)
        except ValueError:
            return
        yield start
        
        start+=1

def get_context(query):
    
    # let's get the document location
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    document = file.read()
    occurrences = []
    query.support_doc_context_occurences = getConcordance(document,query.string_name,10)

def remove_stop_words(sentence):
    word_list = nltk.word_tokenize(sentence)
    for word in word_list: # iterate over word_list
        if word in nltk.corpus.stopwords.words('english'): 
            word_list.remove(word) # remove word from filtered_word_list if it is a stopword
    return word_list

def get_alternative_names(q):
    
    #TODO: Problem with strings such as "Champions' League"
    
    try:
        SQL = """SELECT en_page.page_title FROM en_redirect, en_page WHERE en_redirect.rd_title = '%s' AND en_redirect.rd_from = en_page.page_id """ % (q.string_name.replace("'",""))
        cursor = conn.cursor()    
        status = cursor.execute(SQL)
        rows = cursor.fetchall()
                    
        redirects = []    
    
        for r in rows:        
            redirects.append(r[0])
        
        return redirects
    
    except Execption, e:
        print e
        print SQL

def get_topics(query):
    
    
    """ first get the document and transform it for GibbsLDA format:
        - all words in one line
        - remove stopwords
    """
    
    file = open(docs_locations[query.doc_id]+"/"+query.doc_id+".sgm")
    
    xmldoc = xml.dom.minidom.parse(file)
    items = xmldoc.getElementsByTagName('BODY')
    text = remove_tags((items[0].toxml()).encode("utf8"))    
    p = re.compile(r'\s+')
    doc_text = p.sub(' ',text)
    
    doc_text_no_stopwords = remove_stop_words(doc_text)
    
    doc_lda_format = open(lda_models+"/"+query.id+"/"+query.doc_id+'_lda_format', 'w')
    doc_lda_format.write("1\n"+" ".join(doc_text_no_stopwords))
    doc_lda_format.close()
    
    command = "/home/dsbatista/GibbsLDA++-0.2/src/lda"
    args = " -inf -dir /collections/TAC-2011/lda_models/ -model model-01000 -dfile " + query.id+"/"+query.doc_id+'_lda_format'
    
    print "Calling GibbsLDA++... ", command + args                                         
    p = Popen(command+args,shell=True,stdout=PIPE,stderr=PIPE)
    output, stderr_output = p.communicate()
    print stderr_output     
    print output
    
def analyze_support_document(query):
    
    print "Support document: ", docs_locations[query.doc_id]+"/"+query.doc_id+".sgm"
    
    get_entities(query)
    get_context(query)
    
    """ remove stop words from context occurrences """
    occurences_no_stopwords = []
    
    for occ in query.support_doc_context_occurences:
        occurences_no_stopwords.append(remove_stop_words(" ".join(occ)))
    
    query.support_doc_context_occurences = occurences_no_stopwords
    
    print "Number context occurence sentences", len(query.support_doc_context_occurences)
    
def query_lucene(q):
    
    """
    print "entity string:", q.string_name
    print "persons: ", len(q.support_doc_persons)
    print "organizations: ", len(q.support_doc_organizations)
    print "places: ", len(q.support_doc_places)
    print "alternative names: ", q.alternative_names
    print "context occurences: ", q.support_doc_context_occurences 
    """
    
    args = StringIO.StringIO()
    args.write(' '+q.string_name.encode("utf8")+' ')
    
    
    try:
        #persons = open(lda_models+"/"+q.id+"/"+'persons.txt', 'w')
        for p in q.support_doc_persons:
            #persons.write(p.encode("utf8")+"\n")
            args.write(p.replace("'","").encode("utf8")+" ") 
        #persons.close()
        
        #organizations = open(lda_models+"/"+q.id+"/"+'organizations.txt', 'w')
        for o in q.support_doc_organizations:
            #organizations.write(o.encode("utf8")+"\n")
            args.write(o.replace("'","").encode("utf8")+" ")
        #organizations.close()
        
        #places = open(lda_models+"/"+q.id+"/"+'places.txt', 'w')
        for p in q.support_doc_places:
            #places.write(p.encode("utf8")+"\n")
            args.write(p.replace("'","").encode("utf8")+" ")
        #places.close()
            
        #ocurrences = open(lda_models+"/"+q.id+"/"+'occurrences.txt', 'w')
        for o in q.support_doc_context_occurences:
            #ocurrences.write((" ".join(o)).encode("utf8")+"\n")
            for w in o:
                w.replace("'","")
                args.write(w.encode("utf8")+" ")
        #ocurrences.close()
        
        for a in q.alternative_names:
            if a.endswith("_(disambiguation)"):
                args.write(a.split("_(")[0].replace("'","").encode("utf8")+" ")
            else:
                args.write(a.replace("'","").encode("utf8")+" ")
        
    except Exception, e:
        print "error in ", q.id
        print e
    

    command = "java -jar /collections/TAC-2011/TACKBP.jar"
    outputdir = " /collections/TAC-2011/lda_models/" + q.id + "_no_topics/"
    args_string = args.getvalue()
    
    full_comand = command + outputdir + args_string.decode("utf8")
    
    print "Querying Lucene...", full_comand
                                            
    p = Popen(full_comand,shell=True,stdout=PIPE,stderr=PIPE)
    output, stderr_output = p.communicate()
    
    print stderr_output     
    print output


def generate_output():
    print ""


def start():
    for q in queries:
        print "Processing query: " + q.id +' "'+ q.string_name +'"'
        
        """ create directory with query_id to store information regarding the query """
        os.mkdir(lda_models+q.id+"_no_topics/")
        
        print "Looking for alternative names"
        q.alternative_names =  get_alternative_names(q);
        analyze_support_document(q)
        
        #get_topics(q)        
        query_lucene(q)
        
        #topics similarity
        #generate_output(q)
        
              
def help():
    print "Usage:"
    print "\t process-queries support-documents-locations-file queries-file"

def main():
    if len(sys.argv) < 4 or len(sys.argv) > 4:
        help()
    else:
        print "loading docs locations"
        load_docs_locations(sys.argv[1])
        print len(docs_locations), "docs loaded"
        
        print "loading LDA wordmaps"
        load_lda_wordmaps(lda_models+"wordmap.txt")
        print len(wordmaps_lda), "wordmaps loaded"
        
        print "loading queries"
        parse_queries(sys.argv[3])        
        print len(queries), "queries loaded"
        print "\n"
        start()


if __name__ == "__main__":
    main()
    
    

