# TAC-Entity-Linking

This repo contains the code for an entity linking prototype, trained and tested with the datasets from the TAC Entity-Linking sub-task.
The task consists in linking name-entity mentions in a document collection to entities in a Knowledge Base. 


Data
====
- Knowledge Base
- Source Document Collection


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


Candidates Ranking
==================
- Pairwise learning to rank: SVMRank
- Graph Analysis


NIL Detection
=============
