#!/usr/bin/env python
# -*- coding: utf-8 -*-

import urllib
import httplib
import simplejson

import sys  


""" SASKI API
lg_value = "en"
ne_value = "teste"
saskia_key_value = "23256lk"
params = urllib.urlencode({"lg": lg_value, "ne": ne_value, "api_key": saskia_key_value})
print saskia.read()
"""

""" REMBRANDT API """
sentence = sys.stdin.read()
slg_value = "en"
lg_value = "en"
rembrandt_key_value = "db924ad035a9523bcf92358fcb2329dac923bf9c"
format = "dsb"


def main():

    params = urllib.urlencode({"db": sentence, "slg": slg_value, "lg": lg_value, "api_key": rembrandt_key_value, "f": format})
    headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain"}
    conn = httplib.HTTPConnection("agatha.inesc-id.pt:80")
    conn.request("POST", "/Rembrandt/api/rembrandt?", params, headers)
    
    print params
    
    response = conn.getresponse()
    print "\n"
    print response.msg
    print response.status 
    print response.reason
    data = response.read()
    conn.close()
    print "\n"
    print data
    JSONObject = simplejson.loads(data)
    
    print JSONObject["message"]["document"]["body"]
    
if __name__ == "__main__":
    main()