package FindIO;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class VisualWordIndex extends Index {
    private File indexFile;
    private MMapDirectory MMapDir;
    private IndexWriterConfig config;
    private IndexReader indexReader;
    private AtomicReader areader;
    private String fieldname1 = "imageID";
    private String fieldname2 = "words";

    private Field vw_field;
    private Field img_field;

    // Maximum Buffer Size
    private int MAX_BUFF = 48;

    // to create the TextField for vector insertion
    private StringBuffer strbuf;
    // to create the data_field
    private StringBuffer databuf;

    private FileInputStream binIn;

    public VisualWordIndex() {
        setIndexfile("./src/FindIO/index/vwIndex");

    }

    public void setIndexfile(String indexfilename) {
        this.indexFile = new File(indexfilename);
        if(test){
            System.out.println("The Index File is set: " + indexfilename);
        }
    }

    /**
     * initialization for building the index
     *
     * @throws Throwable
     * */
    public void initBuilding() throws Throwable {
        startbuilding_time = System.currentTimeMillis();

        // PayloadAnalyzer to map the Lucene id and Doc id
        Analyzer analyzer = new StandardAnalyzer();
        // MMap
        MMapDir = new MMapDirectory(indexFile);
        // set the configuration of index writer
        config = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);
        config.setRAMBufferSizeMB(MAX_BUFF);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        // the index configuration
        if (test) {
            System.out.println("Max Docs Num:\t" + config.getMaxBufferedDocs());
            System.out.println("RAM Buffer Size:\t"
                    + config.getRAMBufferSizeMB());
            System.out.println("Max Merge Policy:\t" + config.getMergePolicy());
        }
        // use Memory Map to store the index
        MMwriter = new IndexWriter(MMapDir, config);

        img_field = new TextField(this.fieldname1, "-1", Field.Store.YES);
        vw_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        databuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }

    private void walk(String path){
        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk(f.getAbsolutePath());
            }
            else {
                String fileName = f.getName();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                    String line;
                    while((line = br.readLine()) != null){
                        String[] freqs = line.trim().split("\\s+");
                        double[] words = new double[Common.NUM_VISUAL_WORDS];
                        for(int i = 0; i < freqs.length; i++){
                            words[i] = Double.parseDouble(freqs[i]);
                        }
                        addDoc(fileName, words);
                        index_count++;
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    System.out.println("Result file is not created");
                } catch (IOException e) {
                    System.out.println("Cannot read line from results");
                }
            }
        }
    }

    public void buildIndex() throws Throwable{

        //VisualWordExtraction.createVisualWordsForDirectory("C:\\Users\\Nhan\\Documents\\FindIO\\src\\FindIO\\Datasets\\test\\query", true, "cacheSiftPooling");

        walk("src/FindIO/Features/Visual Word/ScSPM/indexSiftPooling");
        System.out.println("Number of index: " + index_count);
        closeWriter();
    }

    /**
     * Add a document. The document contains two fields: one is the element id,
     * the other is the values on each dimension
     *
     * @param imageID: tag as the key of inverted index
     * @param words: the posting list containing image pairs
     * */
    public void addDoc(String imageID, double[] words) {

        Document doc = new Document();
        // clear the StringBuffer
        strbuf.setLength(0);
        // set new Text for payload analyzer
        long start = System.currentTimeMillis();
        for (int i = 0; i < words.length; i++) {
            double wordValue = words[i];
            if(wordValue > 0){
                strbuf.append(i + " " + wordValue + " ");
            }
        }
        strbuf_time += (System.currentTimeMillis() - start);

        // set fields for document
        this.img_field.setStringValue(imageID);
        this.vw_field.setStringValue(strbuf.toString().trim());
        doc.add(img_field);
        doc.add(vw_field);

        try {
            MMwriter.addDocument(doc);
            System.out.println(Common.MESSAGE_FILE_INDEX_SUCCESS + imageID);
        } catch (IOException e) {
            System.err.println(Common.MESSAGE_HIST_INDEX_ERROR);
            if (test){
                e.printStackTrace();
            }
        }
    }

    public Map<String, double[]> scanVisualWords() throws Exception {

        Map<String, double[]> mapResults = new HashMap<String, double[]>();

        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));

        Bits liveDocs = MultiFields.getLiveDocs(reader);
        for (int i=0; i<reader.maxDoc(); i++) {
            if (liveDocs != null && !liveDocs.get(i))
                continue;

            Document doc = reader.document(i);
            String imageName = doc.get(fieldname1);
            String[] wordsString = doc.get(fieldname2).split(" ");
            double[] words = new double[Common.NUM_VISUAL_WORDS];
            for(int j = 0; j < wordsString.length - 1; j += 2){
                words[Integer.parseInt(wordsString[j])] = Double.parseDouble(wordsString[j + 1]);
            }
            mapResults.put(imageName, words);
        }

        return mapResults;
    }

    public static void main(String[] args){
        VisualWordIndex vwIndex = new VisualWordIndex();
        try{
            vwIndex.initBuilding();
            vwIndex.buildIndex();
        } catch(Throwable e) {
            e.printStackTrace();
            System.out.println(Common.MESSAGE_TEXT_INDEX_ERROR);
            if(test){
                e.printStackTrace();
            }
        }
    }
}
