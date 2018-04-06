package org.apache.lucene.analysis.ko;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.lucene.analysis.ko.morph.AnalysisOutput;
import org.apache.lucene.analysis.ko.morph.MorphAnalyzer;
import org.apache.lucene.analysis.ko.morph.MorphException;
import org.junit.Assert;
import org.junit.Test;

public class MorphAnalyzerMultiThreadTest {

	private Logger log = Logger.getLogger(getClass().getName());

	private static final String TEST = "저작자는 저작물을 창작한 자를 말한다";
	private static final String[] crumb = TEST.split("\\s+");
	private static final String EXPECT = "[저작자(N),는(j)][저작물(N),을(j)][창작(N),하(t),ㄴ(e)][자(V),를(e), 자르(V),ㄹ(e), 자(N),를(j)][말(N),하(t),ㄴ다(e)]";

	@Test
	public void test() throws Exception {
		testMultiThread(8);
	}

	protected void testMultiThread(int nThreads) throws InterruptedException, ExecutionException {
		ExecutorService pool = Executors.newFixedThreadPool(nThreads);
		// 워밍업이 필요한가?
		for (int n = 0; n < nThreads; n++) {
			pool.submit(new Runnable() {
				public void run() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						log.warning(e.getMessage());
					}
				}
			});
		}

		List<Callable<String>> tasks = new ArrayList<Callable<String>>();
		for (int n = 0; n < nThreads; n++) {
			tasks.add(new Callable<String>() {
				public String call() {
					// DictionaryUtil.loadDictionary(false);
					return exec();
				}
			});
		}
		List<Future<String>> results = pool.invokeAll(tasks);
		for (Future<String> result : results) {
			Assert.assertEquals(EXPECT, result.get());
		}

		pool.shutdown();
	}

	protected String exec() {
		try {
			log.info(Thread.currentThread().getName() + "analyze 시작");
			String result = analyze(crumb);
			log.info(Thread.currentThread().getName() + "analyze 종료");
			return result;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	protected static String analyze(String[] crumb) throws MorphException {
		MorphAnalyzer analyzer = new MorphAnalyzer();
		StringBuilder buf = new StringBuilder();
		
		for (int ii = 0; ii < crumb.length; ii++) {
			List<AnalysisOutput> output = analyzer.analyze(crumb[ii]);
			buf.append(output.toString());
		}
		return buf.toString();
	}
}
