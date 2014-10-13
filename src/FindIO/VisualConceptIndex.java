package FindIO;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Beyond on 10/13/2014 0013.
 */
public class VisualConceptIndex extends Index{
    private File indexFile;
    private MMapDirectory MMapDir;
    private IndexWriterConfig config;

    private String fieldname1 = "concept";
    private String fieldname2 = "img_score";

    private Field concept_field;
    private Field img_field;

    // Maximum Buffer Size
    private int MAX_BUFF = 48;

    // to create the TextField for vector insertion
    private StringBuffer strbuf;

    public VisualConceptIndex() {
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

        // use Memory Map to store the index
        MMwriter = new IndexWriter(MMapDir, config);

        img_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }


    public void buildIndex(String dataFile) throws Throwable{


        BufferedReader reader = new BufferedReader(new FileReader(dataFile));
        HashMap<Integer, ArrayList<ImagePair>> conceptImgMap = new HashMap<Integer, ArrayList<ImagePair>>();
        String line = null;

        //add the image score pair to the visual concept posting list
        while ((line = reader.readLine()) != null) {
            String[] img_scores = line.split(" ");
            String imgID = img_scores[0];
            for(int i = 1; i < img_scores.length; i++) {
                int concept = i-1;
                float score = Float.valueOf(img_scores[i]);

                if(score<0){ //never include the negative scored images in the index
                    continue;
                }
                ImagePair image_freq = new ImagePair(imgID, score);


                if(!conceptImgMap.containsKey(concept)){
                    ArrayList<ImagePair> imgPairList = new ArrayList<ImagePair>();
                    imgPairList.add(image_freq);
                    conceptImgMap.put(concept, imgPairList);
                } else {
                    ArrayList<ImagePair> imgPairList = conceptImgMap.get(concept);
                    imgPairList.add(image_freq);
                    conceptImgMap.put(concept, imgPairList);
                }
            }
        }

        for(int concept : conceptImgMap.keySet()){
            ArrayList<ImagePair> imgPairList = conceptImgMap.get(concept);
            addDoc(concept, imgPairList);
            index_count++;
        }
        closeWriter();
    }

    /**
     * Add a document. The document contains two fields: one is the element id,
     * the other is the values on each dimension
     *
     * @param concept: concept as the key of inverted index
     * @param imgPairList: the posting list containing image pairs
     * */
    public void addDoc(int concept, ArrayList<ImagePair> imgPairList) {

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
        this.concept_field.setIntValue(concept);
        this.img_field.setStringValue(strbuf.toString());
        doc.add(concept_field);
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



    public void searchVisualConcept(int concept) throws Throwable{
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        Query query = NumericRangeQuery.newIntRange(this.fieldname1, 1, concept, concept, true, true);
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

        //print out the top hits documents
        for(ScoreDoc hit : hits){
            Document doc = searcher.doc(hit.doc);
            System.out.println(doc.get(fieldname1)+" "+doc.get(fieldname2));
        }

        reader.close();
    }

    public static void main(String[] args){
        VisualConceptIndex vcIndex = new VisualConceptIndex();
        vcIndex.setIndexfile("./src/FindIO/index/vcIndex");
        try{
            vcIndex.initBuilding();
            vcIndex.buildIndex("./src/FindIO/Datasets/train/image_concepts.txt");
        } catch(Throwable e) {
            System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
            if(test)
                e.printStackTrace();
        }

//        try{
//            vcIndex.searchText("china bear");
//        }catch(Throwable e) {
//            System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
//            if(test)
//                e.printStackTrace();
//        }
    }
}
