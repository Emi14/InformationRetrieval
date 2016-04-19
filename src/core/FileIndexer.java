package core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

public class FileIndexer implements AutoCloseable {
	
	private File _indexDirectory = null;
	private IndexWriter _indexWriter = null;
	private IndexReader _indexReader = null;
	
	private static final String IndexerPath = "E:\\Cucu";
	
	public FileIndexer() throws IOException {
		this.createIndexWriter();
	}
	
	private void createIndexWriter() throws IOException {
		_indexDirectory = new File(IndexerPath);
		if (!_indexDirectory.exists()) {
			_indexDirectory.mkdir();
		}
		FSDirectory dir = FSDirectory.open(_indexDirectory.toPath());
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
		_indexWriter = new IndexWriter(dir, writerConfig);
		_indexReader = DirectoryReader.open(dir);
	}
	
	public void addFileToIndex(File file) throws CorruptIndexException, IOException {
		Document doc = new Document();
		doc.add(new TextField("content", new FileReader(file)));
		doc.add(new StringField("filename", file.getName(), Field.Store.YES));
		doc.add(new StringField("fullpath", file.getAbsolutePath(),  Field.Store.YES));
		_indexWriter.addDocument(doc);
		_indexWriter.commit();
	}
	
	public void search() throws ParseException, IOException {
		String queryString = "elevi";
		Query query = new QueryParser("content", _indexWriter.getAnalyzer()).parse(queryString);
		int hitsPerPage = 10;
	    IndexSearcher searcher = new IndexSearcher(_indexReader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	    searcher.search(query, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    for (int i = 0; i < hits.length; i++) {
	    	int documentId = hits[i].doc;
	    	Document document = searcher.doc(documentId);
	    	System.out.println("Found in: " + document.get("fullpath"));
	    }
	}
	
	@Override
	public void close() throws IOException {
		_indexWriter.close();
		_indexReader.close();
    }
}
