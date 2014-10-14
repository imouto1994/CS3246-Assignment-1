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
import java.util.Map;

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

    public static void main(String[] args){
        VisualConceptIndex vcIndex = new VisualConceptIndex();
        vcIndex.setIndexfile("./src/FindIO/index/vcIndex");
//        try{
//            vcIndex.initBuilding();
//            vcIndex.buildIndex("./src/FindIO/Datasets/train/image_gt.txt");
//        } catch(Throwable e) {
//            System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
//            if(test)
//                e.printStackTrace();
//        }

        try{
            vcIndex.searchVisualConcept("0");
        }catch(Throwable e) {
            System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
            if(test)
                e.printStackTrace();
        }
    }

    public VisualConceptIndex(){
        setIndexfile("./src/FindIO/index/vcIndex");
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

        concept_field = new IntField(this.fieldname1, -1, Field.Store.YES);
        img_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }


    public void buildIndex(String gtFile) throws Throwable{

        BufferedReader reader = new BufferedReader(new FileReader(gtFile));
        HashMap<Integer, ArrayList<FindIOPair>> conceptImgMap = new HashMap<Integer, ArrayList<FindIOPair>>();
        String line = null;

        //add the vc image pair to the hashmap
        while ((line = reader.readLine()) != null) {
            String[] img_folders = line.split("\\s+");
            String imageID = img_folders[0];
            String vcTxtName = imageID.substring(0, imageID.lastIndexOf('.'))+".txt";
            String folderName = img_folders[1];
            String vcFilePath = "./src/FindIO/Datasets/train/data/"+folderName+"/"+vcTxtName;
            try {
                readVcFile(Common.removeExtension(imageID), vcFilePath, conceptImgMap);
            } catch(FileNotFoundException e) {
                System.out.println(Common.MESSAGE_FILE_NOTEXIST+": "+vcFilePath);
                e.printStackTrace();
            } catch(IOException e){
                System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
                e.printStackTrace();
            }
            index_count++;
        }

        //Add concept image pair to the index
        for(int concept : conceptImgMap.keySet()){
            ArrayList<FindIOPair> imgPairList = conceptImgMap.get(concept);
            addDoc(concept, imgPairList);
        }
        reader.close();
        closeWriter();
    }


    public void readVcFile(String imgID, String vcFilePath, HashMap<Integer, ArrayList<FindIOPair>> conceptImgMap)throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(vcFilePath));
        String line = null;

        //add the image score pair to the visual concept posting list
        while ((line = reader.readLine()) != null) {
            String[] img_scores = line.split(" ");
            for(int concept = 0; concept < img_scores.length; concept++) {
                float score = Float.valueOf(img_scores[concept]);

                if(score<=0){ //never include the negative scored images in the index
                    continue;
                }
                FindIOPair image_freq = new FindIOPair(imgID, score);


                if(!conceptImgMap.containsKey(concept)){
                    ArrayList<FindIOPair> imgPairList = new ArrayList<FindIOPair>();
                    imgPairList.add(image_freq);
                    conceptImgMap.put(concept, imgPairList);
                } else {
                    ArrayList<FindIOPair> imgPairList = conceptImgMap.get(concept);
                    imgPairList.add(image_freq);
                    conceptImgMap.put(concept, imgPairList);
                }
            }
        }
    }

    /**
     * Add a document. The document contains two fields: one is the element id,
     * the other is the values on each dimension
     *
     * @param concept: concept as the key of inverted index
     * @param imgPairList: the posting list containing image pairs
     * */
    public void addDoc(int concept, ArrayList<FindIOPair> imgPairList) {

        Document doc = new Document();
        // clear the StringBuffer
        strbuf.setLength(0);
        // set new Text for payload analyzer
        long start = System.currentTimeMillis();
        for (int i = 0; i < imgPairList.size(); i++) {
            FindIOPair imgPair = imgPairList.get(i);
            strbuf.append(imgPair.getID() + " "+imgPair.getValue()+",");
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

    public Map<String, double[]> searchVisualConcept(String visualConcepts) throws Exception {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        Query query = parser.parse(visualConcepts);
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

        Map<String, double[]> mapResults = new HashMap<String, double[]>();
        //print out the top hits documents
        for(ScoreDoc hit : hits){
            Document doc = searcher.doc(hit.doc);
            String visualConcept =  doc.get(fieldname1);
            String[] images = doc.get(fieldname2).split(",");
            for(String image : images) {
                String[] infos = image.trim().split("\\s+");
                String imageName = infos[0];
                String frequency = infos[1];
                if(mapResults.get(imageName) == null){
                    mapResults.put(imageName, new double[Common.NUM_VISUAL_CONCEPTS]);
                }
                double[] imageVisualConcepts = mapResults.get(imageName);
                imageVisualConcepts[Integer.parseInt(visualConcept)] = Double.parseDouble(frequency);
            }
        }

        reader.close();

        return mapResults;
    }
}
