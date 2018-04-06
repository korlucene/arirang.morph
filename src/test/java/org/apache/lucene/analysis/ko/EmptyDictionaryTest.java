package org.apache.lucene.analysis.ko;

import static org.apache.lucene.analysis.ko.MorphAnalyzerMultiThreadTest.analyze;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import org.apache.lucene.analysis.ko.morph.MorphException;
import org.apache.lucene.analysis.ko.utils.DictionaryUtil;
import org.junit.Assert;
import org.junit.Test;

public class EmptyDictionaryTest {

	private Logger log = Logger.getLogger(getClass().getName());

	@Test
	public void testAddWords() throws MorphException {
		DictionaryUtil.initDictionary();

		String TEST = "저작자는 저작물을 창작한 자를 말한다";
		String[] crumb = TEST.split("\\s+");

		Assert.assertNull(DictionaryUtil.getWord("저작자"));
		Assert.assertNull(DictionaryUtil.getWord("저작물"));
		Assert.assertNull(DictionaryUtil.getWord("창작"));
		Assert.assertNull(DictionaryUtil.getWord("자"));
		// log.info(analyze(crumb));
		Assert.assertEquals(
				"[저작자(N),는(j), 저작자는(N)][저작물(N),을(j), 저작물을(N)][창작(N),하(t),ㄴ(e), 창작한(N)][자(N),를(j), 자를(N)][말(N),하(t),ㄴ다(e), 말한다(N)]",
				analyze(crumb));

		DictionaryUtil.addWords(Collections.singletonList("자,110000000X"));
		Assert.assertNotNull(DictionaryUtil.getWord("자"));
		Assert.assertEquals(
				"[저작자(N),는(j), 저작자는(N)][저작물(N),을(j), 저작물을(N)][창작(N),하(t),ㄴ(e), 창작한(N)][자(V),를(e), 자(N),를(j)][말(N),하(t),ㄴ다(e), 말한다(N)]",
				analyze(crumb));

		DictionaryUtil.addWords(Arrays.asList("저작물,100000000X", "저작자,100000000X"));
		Assert.assertNotNull(DictionaryUtil.getWord("저작자"));
		Assert.assertNotNull(DictionaryUtil.getWord("저작물"));
		Assert.assertEquals(
				"[저작자(N),는(j)][저작물(N),을(j)][창작(N),하(t),ㄴ(e), 창작한(N)][자(V),를(e), 자(N),를(j)][말(N),하(t),ㄴ다(e), 말한다(N)]",
				analyze(crumb));
	}
}
