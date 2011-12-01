package tac.kbp.kb.ivo_spellchecker;


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *  SuggestWord, used in suggestSimilar method in SpellChecker class.
 * 
 *
 */
public final class SuggestWord implements Comparable<SuggestWord> {
    /**
     * the external id for this word
     */
    public String eid;
    /**
     * the score of the word
     */
    public float score;

    /**
     * the suggested word
     */
    public String string;

    
    
    public final int compareTo(SuggestWord a) {
        if (score < a.score) {
            return 1;
        }
        if (score > a.score) {
            return -1;
        }
        
        
        return this.eid.compareTo(a.eid);
    }


    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( !(obj instanceof SuggestWord) ) return false;
              
        return this.eid.equals(((SuggestWord) obj).eid) && this.score == ((SuggestWord) obj).score;
    }
    
    @Override
    public int hashCode() { // as seen in Effective Java 2nd ed.
        int result = 17;
        
        long l = Double.doubleToLongBits(this.score);
        int c = (int)(l ^ (l >>> 32));
        
        result = 31 * result + c;
        result = 31 * result + this.eid.hashCode();
        
        return result;
    }
}