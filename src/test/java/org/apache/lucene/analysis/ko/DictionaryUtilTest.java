package org.apache.lucene.analysis.ko;

import org.apache.lucene.analysis.ko.morph.MorphException;
import org.apache.lucene.analysis.ko.utils.DictionaryUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.lucene.analysis.ko.MorphAnalyzerMultiThreadTest.analyze;

import java.util.Collections;
import java.util.logging.Logger;

public class DictionaryUtilTest {

	private Logger log = Logger.getLogger(getClass().getName());

	@Test
	public void testAddWords() throws MorphException {
		DictionaryUtil.loadDictionary(false);

		String TEST = "저작자는 저작물을 창작한 자를 말한다";
		String[] crumb = TEST.split("\\s+");

		Assert.assertNull(DictionaryUtil.getWord("말한다"));
		Assert.assertEquals(
				"[저작자(N),는(j)][저작물(N),을(j)][창작(N),하(t),ㄴ(e)][자(V),를(e), 자르(V),ㄹ(e), 자(N),를(j)][말(N),하(t),ㄴ다(e)]",
				analyze(crumb));

		DictionaryUtil.addWords(Collections.singletonList("말한다,100000000X"));
		Assert.assertNotNull(DictionaryUtil.getWord("말한다"));
		Assert.assertEquals(
				"[저작자(N),는(j)][저작물(N),을(j)][창작(N),하(t),ㄴ(e)][자(V),를(e), 자르(V),ㄹ(e), 자(N),를(j)][말한다(N), 말(N),하(t),ㄴ다(e)]",
				analyze(crumb));
		
		DictionaryUtil.clearWords();
		Assert.assertNull(DictionaryUtil.getWord("저작자"));
		Assert.assertNull(DictionaryUtil.getWord("저작물"));
		Assert.assertNull(DictionaryUtil.getWord("창작"));
		Assert.assertNull(DictionaryUtil.getWord("자"));
		Assert.assertNull(DictionaryUtil.getWord("말한다"));
		//log.info(analyze(crumb));
		Assert.assertEquals(
				"[저작자(N),는(j), 저작자는(N)][저작물(N),을(j), 저작물을(N)][창작(N),하(t),ㄴ(e), 창작한(N)][자(N),를(j), 자를(N)][말(N),하(t),ㄴ다(e), 말한다(N)]",
				analyze(crumb));
	}
}
