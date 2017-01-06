# TAC-Entity-Linking

The entity-linking task consists in linking name-entity mentions in a document collection to the correct entity in a Knowledge Base, dealing with disambiguation. For instance, the string-name `Washingon` in a document might represent:

- George Washington (1732–1799), first president of the United States
- Washington (state), United States
- Washington, D.C., the capital of the United States

This repository contains the code for an entity linking prototype, trained and tested with the datasets from the TAC Entity-Linking sub-task. A detaied report of the system can be found [here](dsbatista-projecto_RI.pdf)


Data
====
- Knowledge Base: [LDC2009E58 TAC 2009 KBP Evaluation Reference Knowledge Base](https://catalog.ldc.upenn.edu/docs/LDC2014T16/README.txt)
- Support Documents Collection: LDC2010E12: TAC 2010 KBP Source Data V1.1
- Training Data/Queries: [TAC-KBP 2013 data](http://tac.nist.gov/2013/KBP/data.html)



Training Data
=============

Each training query consists of a:
- name_string: string representing the named-entity, i.e.: GPE, PER, ORG
- doc_id: the id of a support document where the name-entity occurs


Pre-Processing
==============


Query Expansion
===============



Candidates Generation
=====================
- Features Extraction
  - Topic Similarities (LDA)
  - String-Name Similarities
  - Textual Similarities
  - Graph Structure


Candidates Ranking
==================
- Pairwise learning to rank: SVMRank
- Graph Analysis


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
