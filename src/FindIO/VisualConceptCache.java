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
import java.util.HashMap;
import java.util.Map;

public class VisualConceptCache extends Index{
    private File indexFile;
    private MMapDirectory MMapDir;
    private IndexWriterConfig config;

    private String fieldname1 = "imageID";
    private String fieldname2 = "concepts";

    private Field concept_field;
    private Field img_field;

    // Maximum Buffer Size
    private int MAX_BUFF = 48;

    // to create the TextField for vector insertion
    private StringBuffer strbuf;

    public static void main(String[] args){
        VisualConceptCache vcIndex = new VisualConceptCache();
        try{
            vcIndex.initBuilding();
            vcIndex.buildCache("src/FindIO/query_groundTruth.txt");
        } catch(Throwable e) {
            e.printStackTrace();
            System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
            if(test){
                e.printStackTrace();
            }
        }
    }

    public VisualConceptCache(){
        setIndexfile("./src/FindIO/cache/vcCache");
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

        // use Memory Map to store the index
        MMwriter = new IndexWriter(MMapDir, config);

        img_field = new TextField(this.fieldname1, "-1", Field.Store.YES);
        concept_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }


    public void buildCache(String gtFile) throws Throwable{
        String imageListFilePath = "src/FindIO/Features/Visual Concept/trainImageList.txt";
        String imageListFileName = "trainImageList.txt";
        String dataFolder = "src/FindIO/Datasets/test/query";
        try {
            VisualConceptExtraction.generateImageList(imageListFilePath, dataFolder);
            VisualConceptExtraction.getVisualConceptsForImages(imageListFileName);
        } catch (Throwable e){
            e.printStackTrace();
            System.err.println("Error occurs when writing to image list file");
            if(test){
                e.printStackTrace();
            }
        }

        BufferedReader reader = new BufferedReader(new FileReader(gtFile));
        String line = null;

        //add the vc image pair to the hashmap
        while ((line = reader.readLine()) != null) {
            if(line.trim().isEmpty()){
                continue;
            }
            String[] img_folders = line.split("\\s+");
            String imageID = img_folders[0];
            String vcTxtName = imageID.substring(0, imageID.lastIndexOf('.'))+".txt";
            String folderName = img_folders[1];
            String vcFilePath = "./src/FindIO/Datasets/test/query/"+folderName+"/"+vcTxtName;
            try {
                double[] concepts = readVcFile(Common.removeExtension(imageID), vcFilePath);
                addDoc(Common.removeExtension(imageID), concepts);
            } catch(FileNotFoundException e) {
                System.out.println(Common.MESSAGE_FILE_NOTEXIST+": "+vcFilePath);
                e.printStackTrace();
            } catch(IOException e){
                System.out.println(Common.MESSAGE_VC_INDEX_ERROR);
                e.printStackTrace();
            }
            index_count++;
        }
        System.out.println("Number of index: " + index_count);
        reader.close();
        closeWriter();
    }


    public double[] readVcFile(String imgID, String vcFilePath) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(vcFilePath));
        double[] concepts = new double[Common.NUM_VISUAL_CONCEPTS];
        String line = null;

        //add the image score pair to the visual concept posting list
        while ((line = reader.readLine()) != null) {
            String[] img_scores = line.trim().split(" ");
            for(int concept = 0; concept < img_scores.length; concept++) {
                double score = Double.parseDouble(img_scores[concept]);
                concepts[concept] = score;
            }
        }

        reader.close();
        File vcFile = new File(vcFilePath);
        if(vcFile.exists()){
            vcFile.delete();
        }

        return concepts;
    }

    /**
     * Add a document. The document contains two fields: one is the element id,
     * the other is the values on each dimension
     *
     * @param imageID: the ID of the image
     * @param concepts: the list of concepts
     * */
    public void addDoc(String imageID, double[] concepts) {

        Document doc = new Document();
        // clear the StringBuffer
        strbuf.setLength(0);
        // set new Text for payload analyzer
        long start = System.currentTimeMillis();
        for (int i = 0; i < concepts.length; i++) {
            double conceptRelateValue = concepts[i];
            if(conceptRelateValue > 0){
                strbuf.append(i + " " + conceptRelateValue + " ");
            }
        }
        strbuf_time += (System.currentTimeMillis() - start);

        // set fields for document
        this.img_field.setStringValue(imageID);
        this.concept_field.setStringValue(strbuf.toString().trim());
        doc.add(img_field);
        doc.add(concept_field);

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

    public Map<String, double[]> searchVisualConceptsForImage(String imageID) throws Exception {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        Query query = parser.parse(imageID);
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
            String imageName = doc.get(fieldname1);
            String[] conceptsInfo = doc.get(fieldname2).split(" ");
            if(mapResults.get(imageName) == null){
                mapResults.put(imageName, new double[Common.NUM_VISUAL_CONCEPTS]);
            }
            if(conceptsInfo.length < 2){
                continue;
            }
            double[] concepts = mapResults.get(imageName);
            for(int i = 0; i < conceptsInfo.length; i += 2){
                concepts[Integer.parseInt(conceptsInfo[i])] = Double.parseDouble(conceptsInfo[i + 1]);
            }
        }
        reader.close();

        return mapResults;
    }
}
