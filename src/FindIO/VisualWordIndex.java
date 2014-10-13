package FindIO;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nhan on 10/13/14.
 */
public class VisualWordIndex extends Index {

    private File indexFile;
    private MMapDirectory MMapDir;
    private IndexWriterConfig config;
    private IndexReader indexReader;
    private AtomicReader areader;
    private String fieldname1 = "vw";
    private String fieldname2 = "img_freq";

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

    }

    public void setIndexfile(String indexfilename) {
        this.indexFile = new File(indexfilename);
        System.out.println("The Index File is set: " + indexfilename);
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

        vw_field = new TextField(this.fieldname1, "-1", Field.Store.YES);
        img_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        databuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }

    // TODO: reimplement this method
    public void buildIndex(String dataFile) throws Throwable{

        BufferedReader reader = new BufferedReader(new FileReader(dataFile));
        HashMap<String, ArrayList<ImagePair>> tagImgMap = new HashMap<String, ArrayList<ImagePair>>();
        String line = null;

        //add the image frequency pair to the tag posting list
        while ((line = reader.readLine()) != null) {
            String[] img_tags = line.split(" ");
            String imgID = img_tags[0];
            for(int i = 1; i < img_tags.length; i ++) {
                String tag = img_tags[i];
                ImagePair image_freq = new ImagePair(imgID, 1);

                if(!tagImgMap.containsKey(tag)){
                    ArrayList<ImagePair> imgPairList = new ArrayList<ImagePair>();
                    imgPairList.add(image_freq);
                    tagImgMap.put(tag, imgPairList);
                } else {
                    ArrayList<ImagePair> imgPairList = tagImgMap.get(tag);
                    imgPairList.add(image_freq);
                    tagImgMap.put(tag, imgPairList);
                }
            }
        }

        for(String tag : tagImgMap.keySet()){
            ArrayList<ImagePair> imgPairList = tagImgMap.get(tag);
            addDoc(tag, imgPairList);
            index_count++;
        }
        closeWriter();
    }

    /**
     * Add a document. The document contains two fields: one is the element id,
     * the other is the values on each dimension
     *
     * @param tag: tag as the key of inverted index
     * @param imgPairList: the posting list containing image pairs
     * */
    public void addDoc(String tag, ArrayList<ImagePair> imgPairList) {

        Document doc = new Document();
        // clear the StringBuffer
        strbuf.setLength(0);
        // set new Text for payload analyzer
        long start = System.currentTimeMillis();
        for (int i = 0; i < imgPairList.size(); i++) {
            ImagePair imgPair = imgPairList.get(i);
            strbuf.append(imgPair.getImageID() + " "+imgPair.getValue()+",");
        }
        strbuf_time += (System.currentTimeMillis() - start);

        // set fields for document
        this.vw_field.setStringValue(tag);
        this.img_field.setStringValue(strbuf.toString());
        doc.add(vw_field);
        doc.add(img_field);

        try {
            MMwriter.addDocument(doc);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.err.println("index writer error");
            if (test)
                e.printStackTrace();
        }
    }

    public Map<String, Map<String, Double>> searchText(String visualWords) throws Throwable{
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        Query query = parser.parse(visualWords);
        System.out.println("Searching for: " + query.toString(fieldname1));

        TopDocs topDocs;
        if (test) {                           // repeat & time as benchmark
            long start = System.currentTimeMillis();
            topDocs = searcher.search(query, null, Common.topK);
            long end =  System.currentTimeMillis();
            System.out.println("Time: "+(end - start)+" ms");
        } else {
            topDocs = searcher.search(query, null, Common.topK);
        }

        ScoreDoc[] hits = topDocs.scoreDocs;

        Map<String, Map<String, Double>> mapResults = new HashMap<String, Map<String, Double>>();
        //print out the top hits documents
        for(ScoreDoc hit : hits){
            Document doc = searcher.doc(hit.doc);
            String visualWord = doc.get(fieldname1);
            String[] images = doc.get(fieldname2).split(",");
            for(String image : images){
                String[] infos = image.trim().split("\\s+");
                String imageName = infos[0];
                String frequency = infos[1];
                if(mapResults.get(imageName) == null){
                    mapResults.put(imageName, new HashMap<String, Double>());
                }
                Map<String, Double> imageVisualWords = mapResults.get(imageName);
                imageVisualWords.put(visualWord, Double.parseDouble(frequency));
            }
        }

        reader.close();
        return mapResults;
    }

    public static void main(String[] args){
        TextIndex textIndex = new TextIndex();
        textIndex.setIndexfile("./src/FindIO/index/textIndex");
//        try{
//            textIndex.initBuilding();
//            textIndex.buildIndex("./src/FindIO/Datasets/train/image_tags.txt");
//        } catch(Throwable e) {
//            System.out.println(MESSAGE_TEXT_INDEX_ERROR);
//            if(test)
//                e.printStackTrace();
//        }

        try{
            textIndex.searchText("china bear");
        }catch(Throwable e) {
            System.out.println(Common.MESSAGE_TEXT_INDEX_ERROR);
            if(test)
                e.printStackTrace();
        }
    }

}
