package com.library.api.search.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TextFileSearcher {

    public static final int MAXIMUM_NUMBER_OF_HITS = 5;

    private static StandardAnalyzer analyzer = new StandardAnalyzer();
    private IndexReader reader;
    private IndexSearcher searcher;

    public TextFileSearcher(String indexDir) throws IOException {
        this(FSDirectory.open(new File(indexDir)));
    }

    public TextFileSearcher(BaseDirectory baseDirectory) throws IOException {
        this.reader = DirectoryReader.open(baseDirectory);
        this.searcher = new IndexSearcher(reader);
    }

    public void search(String searchQuery) {
        search(searchQuery, MAXIMUM_NUMBER_OF_HITS);
    }

    public void search(String searchQuery, int maxhits) {
        try {
            QueryParser parser = new QueryParser("contents", analyzer);
            Query query = parser.parse(searchQuery);

            TopScoreDocCollector collector = TopScoreDocCollector.create(maxhits, true);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            for (ScoreDoc scoredoc : hits) {
                //Retrieve the matched document and show relevant details
                Document doc = searcher.doc(scoredoc.doc);
                System.out.println(doc.get("path") + " score=" + scoredoc.score);
                System.out.println("\nSender: " + doc.getField("sender"));
                System.out.println("Subject: " + doc.getField("subject"));
                System.out.println("Email file location: " + doc.getField("emailDoc"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void highlightSearch(String searchQuery) {
        try {
            QueryParser parser = new QueryParser("ncontent", analyzer);
            Query query = parser.parse(searchQuery);

            TopDocs hits = searcher.search(query, reader.maxDoc());
            System.out.println(hits.totalHits);
            SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
            for (int i = 0; i < reader.maxDoc(); i++) {
                int id = hits.scoreDocs[i].doc;
                //Term vector
                fragmentHighlighter(id, highlighter, "ncontent");
                fragmentHighlighter(id, highlighter, "content");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addTestFiles() {
        try {
            TextFileIndexer indexer = new TextFileIndexer("C:/ZZZ/index");
            indexer.indexFileOrDirectory("C:/ZZZ/z/file1.txt");
            indexer.indexFileOrDirectory("C:/ZZZ/z/file2.txt");
            indexer.closeIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fragmentHighlighter(int hitId, Highlighter highlighter, String fieldname) throws IOException, InvalidTokenOffsetsException {
        Document doc = searcher.doc(hitId);
        String text = doc.get(fieldname);
        TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hitId, fieldname, analyzer);
        TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 4);
        for (int j = 0; j < frag.length; j++) {
            if ((frag[j] != null) && (frag[j].getScore() > 0)) {
                System.out.println((frag[j].toString()));
            }
        }
    }

    public void searchSpan(String text, int hits) throws IOException {
        // Do a search using SpanQuery
        SpanTermQuery spanQuery = new SpanTermQuery(new Term("content", text));
        TopDocs results = searcher.search(spanQuery, hits);
        for (int i = 0; i < results.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = results.scoreDocs[i];
            System.out.println("Score Doc: " + scoreDoc);
        }

        IndexReader reader = searcher.getIndexReader();
        //this is not the best way of doing this, but it works for the example.
        // See http://www.slideshare.net/lucenerevolution/is-your-index-reader-really-atomic-or-maybe-slow for higher performance approaches
        AtomicReader wrapper = SlowCompositeReaderWrapper.wrap(reader);
        Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
        Spans spans = spanQuery.getSpans(wrapper.getContext(), new Bits.MatchAllBits(reader.numDocs()), termContexts);
        int window = 2;//get the words within two of the match
        while (spans.next() == true) {
            Map<Integer, String> entries = new TreeMap<Integer, String>();
            System.out.println("Doc: " + spans.doc() + " Start: " + spans.start() + " End: " + spans.end());
            int start = spans.start() - window;
            int end = spans.end() + window;
            Terms content = reader.getTermVector(spans.doc(), "content");
            TermsEnum termsEnum = content.iterator(null);
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                //could store the BytesRef here, but String is easier for this example
                String s = new String(term.bytes, term.offset, term.length);
                DocsAndPositionsEnum positionsEnum = termsEnum.docsAndPositions(null, null);
                if (positionsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    int i = 0;
                    int position = -1;
                    while (i < positionsEnum.freq() && (position = positionsEnum.nextPosition()) != -1) {
                        if (position >= start && position <= end) {
                            entries.put(position, s);
                        }
                        i++;
                    }
                }
            }
            System.out.println("Entries:" + entries);
        }
    }
}
