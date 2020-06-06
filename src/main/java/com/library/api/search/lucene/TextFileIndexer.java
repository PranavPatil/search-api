package com.library.api.search.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds files into this index
 * based on the input of the user.
 */
public class TextFileIndexer {

    private static StandardAnalyzer analyzer = new StandardAnalyzer();
    private IndexWriter writer;
    private ArrayList<File> queue = new ArrayList<File>();
    private BaseDirectory baseDirectory;

    public TextFileIndexer() throws IOException {
        this(new RAMDirectory());
    }

    /**
     * Constructor
     *
     * @param indexDir the name of the folder in which the index should be created
     * @throws java.io.IOException when exception creating index.
     */
    public TextFileIndexer(String indexDir) throws IOException {
        // the boolean true parameter means to create a new index everytime,
        // potentially overwriting any existing files there.
        this(FSDirectory.open(new File(indexDir)));
    }

    public TextFileIndexer(BaseDirectory baseDirectory) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        this.writer = new IndexWriter(baseDirectory, config);
        this.baseDirectory = baseDirectory;
    }

    /**
     * Indexes a file or directory
     *
     * @param fileName the name of a text file or a folder we wish to add to the index
     * @throws java.io.IOException when exception
     */
    public void indexFileOrDirectory(String fileName) throws IOException {
        //===================================================
        //gets the list of files in a folder (if user has submitted
        //the name of a folder) or gets a single file name (is user
        //has submitted only the file name)
        //===================================================
        addFiles(new File(fileName));

        int originalNumDocs = writer.numDocs();

        for (File f : queue) {
            FileReader fr = null;
            try {
                Document doc = new Document();

                // add contents of file
                fr = new FileReader(f);
                //doc.add(new TextField("contents", fr));
                doc.add(createContentField(fr));
                doc.add(new StringField("path", f.getPath(), Field.Store.YES));
                doc.add(new StringField("filename", f.getName(), Field.Store.YES));
                writer.addDocument(doc);
                System.out.println("Added: " + f);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not add: " + f);
            } finally {
                fr.close();
            }
        }

        int newNumDocs = writer.numDocs();
        System.out.println("");
        System.out.println("************************");
        System.out.println((newNumDocs - originalNumDocs) + " documents added.");
        System.out.println("************************");

        queue.clear();
    }

    private Field createContentField(FileReader reader) throws IOException {

        FieldType fieldType = new FieldType();
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setIndexed(true);
        fieldType.setTokenized(true);
        fieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS);
        fieldType.setStored(true);
        fieldType.setOmitNorms(false);
        fieldType.setStoreTermVectorPayloads(false);
        return new Field("content", reader, fieldType);
    }

    private void addFiles(File file) {

        if (!file.exists()) {
            System.err.println(file + " does not exist.");
        }
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                addFiles(f);
            }
        } else {
            String filename = file.getName().toLowerCase();

            // Only index text files
            if (filename.endsWith(".htm") || filename.endsWith(".html") ||
                    filename.endsWith(".xml") || filename.endsWith(".txt")) {
                queue.add(file);
            } else {
                System.out.println("Skipped " + filename);
            }
        }
    }

    /**
     * Close the index.
     *
     * @throws java.io.IOException when exception closing
     */
    public void closeIndex() throws IOException {
        writer.close();
    }

    private static BooleanQuery getBooleanQuery(String... queries) {
        BooleanQuery booleanQuery = new BooleanQuery();

        for (String queryString : queries) {
            Query query = new TermQuery(new Term("bodytext", queryString));
            booleanQuery.add(query, BooleanClause.Occur.SHOULD);
        }
        return booleanQuery;
    }

    private byte[] getFileBytes(String filename) throws IOException {
        Path path = Paths.get(filename);
        return Files.readAllBytes(path);
    }

    public BaseDirectory getBaseDirectory() {
        return this.baseDirectory;
    }
}
