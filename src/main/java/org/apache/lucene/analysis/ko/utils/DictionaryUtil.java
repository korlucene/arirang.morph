package org.apache.lucene.analysis.ko.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/*
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

import org.apache.lucene.analysis.ko.morph.CompoundEntry;
import org.apache.lucene.analysis.ko.morph.MorphException;
import org.apache.lucene.analysis.ko.morph.WordEntry;

public class DictionaryUtil {
  
  private static Trie<String,WordEntry> dictionary;
  
  private static HashMap<String, String> josas;
  
  private static HashMap<String, String> eomis;
  
  private static HashMap<String, String> prefixs;
  
  private static HashMap<String, String> suffixs;
  
  private static HashMap<String,WordEntry> uncompounds;
  
  private static HashMap<String, String> cjwords;
  
  private static HashMap<String, String> abbreviations;
  
  private static List<char[]> syllables;  // 음절특성 정보
  
  private static AtomicBoolean initialized = new AtomicBoolean(); // 조사,어미 등..
  private static AtomicBoolean prepared = new AtomicBoolean(); // 단어사전(dictionary)이 준비되었는가?
  
  /**
   * 기존 로드 방식을 유지하기 위해서, 
   * Elasticsearch 플러그인의 reload를 위해서 단어는 무조건 리로드
   * @throws MorphException
   */
  public static void loadDictionary() throws MorphException {
	if (!initialized.get()) initialize();
	loadWordsAndCompounds(true); // 
  }
  
  public static void loadDictionary(boolean reload) throws MorphException {
	if (!initialized.get()) initialize();
	loadWordsAndCompounds(reload); 
  }
  
  /**
   * 단어 사전을 로드하지 않는다.
   * 
   * @throws MorphException
   */
  public static void initDictionary() throws MorphException {
	if (!initialized.get()) initialize();  
	synchronized(prepared) {
		DictionaryUtil.dictionary = new Trie<String, WordEntry>(true);
	    prepared.set(true);
	}
  }
  
  /**
   * 사전이 준비되었는지 확인
   */
  private static void checkDictionary() throws MorphException {
	if(!initialized.get()) initialize();
	if(!prepared.get()) loadWordsAndCompounds(false);
  }
  
  private static void loadWordsAndCompounds(boolean reload) throws MorphException {
	synchronized(prepared) {
	  if (!reload && prepared.get())
		return;
	  
	  long start = System.currentTimeMillis();
	  try {
	    Trie<String, WordEntry> dictionary = new Trie<String, WordEntry>(true);
	    List<String> strList = null;
	    List<String> compounds = null;
	    strList = FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_DICTIONARY),"UTF-8");
	    strList.addAll(FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_EXTENSION),"UTF-8"));
	    compounds = FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_COMPOUNDS),"UTF-8"); 
	       
	    loadWords(dictionary, strList);
	    loadCompounds(dictionary, compounds);
	
	    DictionaryUtil.dictionary = dictionary;
	    prepared.set(true);
	    Logger.getLogger("org.apache.lucene.analysis.ko.Dictionary")
	      .info("단어사전 로드 " + (System.currentTimeMillis() - start) + "ms");
	  } catch (IOException e) {      
	    throw new MorphException(e.getMessage(),e);
	  } catch (Exception e) {
	    throw new MorphException(e.getMessage(),e);
	  }
	}
  }

  private static void loadWords(Trie<String, WordEntry> dictionary, List<String> strList) {
	for(String str:strList) {
      String[] infos = str.split("[,]+");
      if(infos.length!=2) continue;
      infos[1] = infos[1].trim();
      if(infos[1].length()==6) infos[1] = infos[1].substring(0,5)+"000"+infos[1].substring(5);
      
      WordEntry entry = new WordEntry(infos[0].trim(),infos[1].trim().toCharArray());
      dictionary.add(entry.getWord(), entry);
    }
  }
  
  private static void loadCompounds(Trie<String, WordEntry> dictionary, List<String> compounds) {
	for(String compound: compounds) 
    {    
      String[] infos = compound.split("[:]+");
      if(infos.length!=3&&infos.length!=2) continue;
      
      WordEntry entry = null;
      if(infos.length==2) 
        entry = new WordEntry(infos[0].trim(),"200000000X".toCharArray());
      else 
        entry = new WordEntry(infos[0].trim(),("200"+infos[2]+"00X").toCharArray());
      
      entry.setCompounds(compoundArrayToList(infos[1], infos[1].split("[,]+")));
      dictionary.add(entry.getWord(), entry);
    }
  }
  
  /**
   * 단어 사전을 제외하고 기본 사전을 로드. 한번만 로드한다.
   * @throws MorphException
   */
  private static void initialize() throws MorphException {
	synchronized (initialized) {
	  if (initialized.get())
	    return;
	  
	  HashMap<String, String> abbreviations;
	  HashMap<String, String> josas;
	  HashMap<String, String> eomis;
	  HashMap<String, String> prefixs;
	  HashMap<String, String> suffixs;
	  HashMap<String,WordEntry> uncompounds;
	  HashMap<String, String> cjwords;
	  
	  long start = System.currentTimeMillis();
	  try {
		List<String> abbrevs = FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_ABBREV),"UTF-8");
		   
		abbreviations = new HashMap<String, String>();
		for(String abbrev: abbrevs) 
		{    
		  String[] infos = abbrev.split("[:]+");
		  if(infos.length!=2) continue;      
		  abbreviations.put(infos[0].trim(), infos[1].trim());
		}
		  
	      
	    josas = new HashMap<String, String>();
	    readFile(josas,KoreanEnv.FILE_JOSA);
	
	    eomis = new HashMap<String, String>();
	    readFile(eomis,KoreanEnv.FILE_EOMI);
	    
	    prefixs = new HashMap<String, String>();
	    readFile(prefixs,KoreanEnv.FILE_PREFIX);
	    
	    suffixs = new HashMap<String, String>();
	    readFile(suffixs,KoreanEnv.FILE_SUFFIX);
	    
	    cjwords = new HashMap<String, String>();
        List<String> lines = FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_CJ),"UTF-8");  
        for(String cj: lines) {    
          String[] infos = cj.split("[:]+");
          if(infos.length!=2) continue;
          cjwords.put(infos[0], infos[1]);
        }      
        
        uncompounds = new HashMap<String,WordEntry>();
        List<String> lines2 = FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_UNCOMPOUNDS),"UTF-8");  
        for(String compound: lines2) {    
          String[] infos = compound.split("[:]+");
          if(infos.length!=2) continue;
          WordEntry entry = new WordEntry(infos[0].trim(),"90000X".toCharArray());
          entry.setCompounds(compoundArrayToList(infos[1], infos[1].split("[,]+")));
          uncompounds.put(entry.getWord(), entry);
        }
        
    	//음절정보특성 SyllableUtil
        ArrayList<char[]> syllables = new ArrayList<char[]>();

        List<String> line = FileUtil.readLines(KoreanEnv.getInstance().getValue(KoreanEnv.FILE_SYLLABLE_FEATURE),"UTF-8");  
        for(int i=0;i<line.size();i++) {        
          if(i!=0)
        	syllables.add(line.get(i).toCharArray());
        }
        
	    DictionaryUtil.abbreviations = abbreviations;
	    DictionaryUtil.josas = josas;
	    DictionaryUtil.eomis = eomis;
	    DictionaryUtil.prefixs = prefixs;
	    DictionaryUtil.suffixs = suffixs;
	    DictionaryUtil.uncompounds = uncompounds;
	    DictionaryUtil.cjwords = cjwords;
	    DictionaryUtil.syllables = syllables;
	      
	    initialized.set(true);
	    
	    Logger.getLogger("org.apache.lucene.analysis.ko.Dictionary")
	      .info("기본사전 로드 " + (System.currentTimeMillis() - start) + "ms");
	  } catch (IOException e) {
	    throw new MorphException(e);
	  }
	}
  }
  
  @SuppressWarnings("unchecked")
  public static Iterator<WordEntry> findWithPrefix(String prefix) throws MorphException {
	checkDictionary();
    return dictionary.getPrefixedBy(prefix);
  }

  public static WordEntry getWord(String key)  {    
   
	try {
		checkDictionary();
	    if(key.length()==0) return null;
	    
	    return (WordEntry)dictionary.get(key);
	} catch (MorphException e) {
		throw new RuntimeException(e);
	}

  }
  
  public static void addEntry(WordEntry entry) {
      try {
    	  checkDictionary();
          dictionary.add(entry.getWord(), entry);
      } catch (MorphException e) {
          throw new RuntimeException(e);
      }
  }
  
  /**
   * Thread Safe 하지 않음
   * @Experimental 
   */
  public static void clearWords() {
	  if (dictionary != null) {
		  dictionary.clear();
	  }
  }
  
  /**
   * Thread Safe 하지 않음
   * @Experimental 
   */
  public static void addWord(WordEntry entry) {
	  synchronized(prepared) {
		  if (dictionary == null) {
			 dictionary = new Trie<String, WordEntry>(true);
		  }
		  dictionary.add(entry.getWord(), entry);
		  prepared.set(true);
	  }
  }
  
  /**
   * Thread Safe 하지 않음 
   * @Experimental
   */
  public static void addWords(List<String> lines) {
	  synchronized(prepared) {
		  if (dictionary == null) {
			 dictionary = new Trie<String, WordEntry>(true);
		  }
		  loadWords(dictionary, lines);
		  prepared.set(true);
	  }
  }
  
  /**
   * Thread Safe 하지 않음 
   * @Experimental
   */
  public static void addCompounds(List<String> lines) {
	  synchronized(prepared) {
		  if (dictionary == null) {
			 dictionary = new Trie<String, WordEntry>(true);
		  }
		  loadCompounds(dictionary, lines);
		  prepared.set(true);
	  }
  }

  public static WordEntry getWordExceptVerb(String key) throws MorphException {    
    WordEntry entry = getWord(key);    
    if(entry==null) return null;
    
    if(entry.getFeature(WordEntry.IDX_NOUN)=='1'||
        entry.getFeature(WordEntry.IDX_NOUN)=='2'||
        entry.getFeature(WordEntry.IDX_BUSA)=='1')
      return entry;
    
    return null;
  }
  
  public static WordEntry getNoun(String key) throws MorphException {  

    WordEntry entry = getWord(key);
    if(entry==null) return null;
    
    if(entry.getFeature(WordEntry.IDX_NOUN)=='1') return entry;
    return null;
  }
  
  /**
   * 
   * return all noun including compound noun
   * @param key the lookup key text
   * @return  WordEntry
   * @throws MorphException throw exception
   */
  public static WordEntry getAllNoun(String key) throws MorphException {  

    WordEntry entry = getWord(key);
    if(entry==null) return null;

    if(entry.getFeature(WordEntry.IDX_NOUN)=='1' || entry.getFeature(WordEntry.IDX_NOUN)=='2') return entry;
    return null;
  }
  
  public static WordEntry getVerb(String key) throws MorphException {
    
    WordEntry entry = getWord(key);  
    if(entry==null) return null;

    if(entry.getFeature(WordEntry.IDX_VERB)=='1') {
      return entry;
    }
    return null;
  }
  
  @Deprecated
  public static WordEntry getAdverb(String key) throws MorphException {
    WordEntry entry = getWord(key);
    if(entry==null) return null;

    if(entry.getFeature(WordEntry.IDX_BUSA)=='1') return entry;
    return null;
  }
  
  @Deprecated
  public static WordEntry getBusa(String key) throws MorphException {
    WordEntry entry = getWord(key);
    if(entry==null) return null;

    if(entry.getFeature(WordEntry.IDX_BUSA)=='1') return entry;
    return null;
  }
  
  @Deprecated
  public static WordEntry getIrrVerb(String key, char irrType) throws MorphException {
    WordEntry entry = getWord(key);
    if(entry==null) return null;

    if(entry.getFeature(WordEntry.IDX_VERB)=='1'&&
        entry.getFeature(WordEntry.IDX_REGURA)==irrType) return entry;
    return null;
  }
  
  @Deprecated
  public static WordEntry getBeVerb(String key) throws MorphException {
    WordEntry entry = getWord(key);
    if(entry==null) return null;
    
    if(entry.getFeature(WordEntry.IDX_BEV)=='1') return entry;
    return null;
  }
  
  @Deprecated
  public static WordEntry getDoVerb(String key) throws MorphException {
    WordEntry entry = getWord(key);
    if(entry==null) return null;
    
    if(entry.getFeature(WordEntry.IDX_DOV)=='1') return entry;
    return null;
  }
  
  public static String getAbbrevMorph(String key) throws MorphException {
	if(!initialized.get()) initialize();
	  
    return abbreviations.get(key);
  }
  
  public static WordEntry getUncompound(String key) throws MorphException {
	if(!initialized.get()) initialize();
	
    return uncompounds.get(key);
  }
  
  public static String getCJWord(String key) throws MorphException {
	if(!initialized.get()) initialize();
	
    return cjwords.get(key);
  }
  
  public static boolean existJosa(String str) throws MorphException {
	if(!initialized.get()) initialize();
	  
    return josas.containsKey(str);
  }
  
  public static boolean existEomi(String str)  throws MorphException {
	if(!initialized.get()) initialize();

    return eomis.containsKey(str);
  }
  
  public static String getJosa(String str) throws MorphException {
	if(!initialized.get()) initialize();
	
    return josas.get(str);
  }
  
  public static String getEomi(String str)  throws MorphException {
	if(!initialized.get()) initialize();

    return eomis.get(str);
  }
	  
  public static boolean existPrefix(String str)  throws MorphException {
	if(!initialized.get()) initialize();
    
    return prefixs.get(str) != null;
  }
  
  public static boolean existSuffix(String str)  throws MorphException {
    if(!initialized.get()) initialize();

    return suffixs.containsKey(str);
  }
  
  /**
   * ㄴ,ㄹ,ㅁ,ㅂ과 eomi 가 결합하여 어미가 될 수 있는지 점검한다.
   */
  public static String combineAndEomiCheck(char s, String eomi) throws MorphException {
  
    if(eomi==null) eomi="";

    if(s=='ㄴ') eomi = "은"+eomi;
    else if(s=='ㄹ') eomi = "을"+eomi;
    else if(s=='ㅁ') eomi = "음"+eomi;
    else if(s=='ㅂ') eomi = "습"+eomi;
    else eomi = s+eomi;

    if(existEomi(eomi)) return eomi;    

    return null;
    
  }
  
  /**
   * modified at 2017-09-19 by smlee
   * @param map map
   * @param dic  1: josa, 2: eomi
   * @throws MorphException excepton
   */
  private static void readFile(HashMap<String, String> map, String dic) throws MorphException {    
    
    String path = KoreanEnv.getInstance().getValue(dic);

    try{
      List<String> lines = FileUtil.readLines(path,"UTF-8");
      
      for(int i=1;i<lines.size();i++) {
    	String word_string = lines.get(i).trim();
    	String[] fields = word_string.split(",");

    	if(fields.length==2) {
    		map.put(fields[0].trim(), fields[1].trim());
    	} else {
    		map.put(word_string, word_string);
    	}
      }
    }catch(IOException e) {
      throw new MorphException(e.getMessage(),e);
    } catch (Exception e) {
      throw new MorphException(e.getMessage(),e);
    }
  }
  
  private static List<CompoundEntry> compoundArrayToList(String source, String[] arr) {
    List<CompoundEntry> list = new ArrayList<CompoundEntry>();
    for(String str: arr) {
      CompoundEntry ce = new CompoundEntry(str);
      ce.setOffset(source.indexOf(str));
      list.add(ce);
    }
    return list;
  }

/**
   * 인덱스 값에 해당하는 음절의 특성을 반환한다.
   * 영자 또는 숫자일 경우는 모두 해당이 안되므로 가장 마지막 글자인 '힣' 의 음절특성을 반환한다.
   * 
   * @param idx '가'(0xAC00)이 0부터 유니코드에 의해 한글음절을 순차적으로 나열한 값
   * @throws MorphException throw exceptioin
   */
  public static char[] getFeature(int idx)  throws MorphException {
	if(!initialized.get()) initialize();
	
    if(idx<0||idx>=syllables.size()) 
      return syllables.get(syllables.size()-1);
    else 
      return syllables.get(idx);
    
  }
}
