package org.unrulymedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class IndexBuilderTest {

	private BufferedReader reader = new BufferedReader(new InputStreamReader(
			System.in));

	@Test
	public void shouldDoSomeFunkyShit() throws Exception {
		RAMDirectory indexStore = new RAMDirectory();
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36,
				analyzer);
		IndexWriter writer = new IndexWriter(indexStore, config);

		List<String> cities = FileUtils.readLines(new File("cities15000.txt"));

		for (String cityLine : cities) {
			String[] data = cityLine.split("\t");
			Document city = new Document();
			city.add(new Field("name", data[1], Field.Store.YES,
					Field.Index.ANALYZED));
			city.add(new Field("ascii-name", data[2], Field.Store.YES,
					Field.Index.ANALYZED));
			city.add(new Field("alternate-name", data[3], Field.Store.YES,
					Field.Index.ANALYZED));
			city.add(new Field("latitude", data[4], Field.Store.YES,
					Field.Index.NO));
			city.add(new Field("longitude", data[5], Field.Store.YES,
					Field.Index.NO));

			writer.addDocument(city);
		}
		writer.close(true);

		IndexReader indexReader = IndexReader.open(indexStore);
		IndexSearcher searcher = new IndexSearcher(indexReader);

		System.err.println("Query:");
		String line;
		Term nameTerm = new Term("ascii-name");
		Term alternateTerm = new Term("alternate-name");
		while ((line = reader.readLine()) != null) {
			Query q = new FuzzyQuery(nameTerm.createTerm(line));

			search(searcher, q);
			q = new FuzzyQuery(alternateTerm.createTerm(line));
			search(searcher, q);
		}
	}

	private void search(IndexSearcher searcher, Query q) throws IOException,
			CorruptIndexException {
		TopDocs topDocs = searcher.search(q, 1, Sort.RELEVANCE);
		if (topDocs.totalHits > 0) {
			Document result = searcher.doc(topDocs.scoreDocs[0].doc);
			System.err.println(result);
		} else {
			System.err.println("No hits");
		}
	}
}
