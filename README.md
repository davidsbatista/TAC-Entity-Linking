# TAC-Entity-Linking

The entity-linking task consists in linking name-entity mentions in a document collection to the correct entity in a Knowledge Base, dealing with disambiguation. For instance, the string-name `Washingon` in a document might represent:

- George Washington (1732–1799), first president of the United States
- Washington (state), United States
- Washington, D.C., the capital of the United States
- NIL in case the `Washington` which the document is refering to does not exist in the KB

This repository contains the code for an entity linking prototype, trained and tested with the datasets from the TAC Entity-Linking sub-task. A detaied description of the prototype system can be found in this [report](dsbatista-projecto_RI.pdf)


Data
====
- Knowledge Base: [LDC2009E58 TAC 2009 KBP Evaluation Reference Knowledge Base](https://catalog.ldc.upenn.edu/docs/LDC2014T16/README.txt)
- Support Documents Collection: LDC2010E12 TAC 2010 KBP Source Data V1.1
- Training Data/Queries: [TAC-KBP 2013 data](http://tac.nist.gov/2013/KBP/data.html)



Training Data
=============

Each training query consists of a:
- `name_string`: string representing the named-entity, of 3 possible types (i.e.: GPE, PER, ORG)
- `doc_id`: the id of a support document where the name-entity occurs


Pre-Processing
==============
- Create a dictionary of alternative names based on:
  - Acronyms expansion
  - Wikipedia redirect pages
- The dictionary is kept in a REDIS instance
- Create 3 Lucene Indexes
 - KB/Wikipedia article names
 - KB/Wikipedia full-text
 - Source Document Collection


Query Expansion
===============
- Get all possible alternative names/senses for a given query string
- Extract the top-k articles, for each sense/name, from the KB/Wikipedia using the Lucene Index


Candidates Generation
=====================
- Extract features for each candidate instance retrieved from the KB
  - Topic Similarities (LDA)
  - String-Name Similarities
  - Textual Similarities
  - Graph Structure


Candidates Ranking
==================
- Pairwise learning to rank: [SVMRank](https://www.cs.cornell.edu/people/tj/svm_light/svm_rank.html)
 - Correct answer is ranked as first
 - All others candidates are ranked as second
- [A Graph-based Method for Entity Linking](http://www.aclweb.org/anthology/I11-1113) which exploits the graph structure of Wikipedia to perform named entity disambiguation based on two measures
    
  - Out-Degree: nodes in the graph consist of named entities present in the support document which also correspond to entities in the KB, and the text articles of the candidates. There is a directed edge from an article node to a name node when the name is mentioned in the article.

  - In-Degree: nodes are the name string of the candidates entities and the text articles of named entities, which are also entities in the KB, present in the support document. There is a directed edge that links an article to a candidate name string when the article of a context named entity contains that name.


NIL Detection
=============
- An SVM to distiguinch between a correct candidate and a NIL (i.e., there is no KB representation)
- Features
 - Score
 - Mean Score
 - Difference to Mean Score
 - Standard Deviation
 - Dixion's Q Test for Outliers
 - Grubb's Test for Outliers
