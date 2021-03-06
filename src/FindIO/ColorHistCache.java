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
import java.util.HashMap;
import java.util.Map;

public class ColorHistCache extends Index {

    private File indexFile;
    private MMapDirectory MMapDir;
    private IndexWriterConfig config;
    private IndexReader indexReader;
    private AtomicReader areader;
    private String fieldname1 = "imageID";
    private String fieldname2 = "colorHistogram";

    private Field img_field;
    private Field hist_field;

    // Maximum Buffer Size
    private int MAX_BUFF = 48;

    // to create the TextField for vector insertion
    private StringBuffer strbuf;
    // to create the data_field
    private StringBuffer databuf;

    private FileInputStream binIn;

    public ColorHistCache() {
        setIndexfile("src/FindIO/cache/colorHistCache");
    }

    public void setIndexfile(String indexfilename) {
        this.indexFile = new File(indexfilename);
        if(test){
            System.out.println("The Index File is set: " + indexfilename);
        }
    }

    /**
     * Initialization for building the index
     *
     * @throws Throwable
     * */
    public void initBuilding() throws Throwable {
        startbuilding_time = System.currentTimeMillis();

        Analyzer analyzer = new StandardAnalyzer();
        MMapDir = new MMapDirectory(indexFile);

        // set the configuration of index writer
        config = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);
        config.setRAMBufferSizeMB(MAX_BUFF);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        // use Memory Map to store the index
        MMwriter = new IndexWriter(MMapDir, config);

        img_field = new TextField(this.fieldname1, "-1", Field.Store.YES);
        hist_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        databuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }

    /* Build the index */
    public void buildCache(String groundTruthFile) throws Throwable{

        BufferedReader reader = new BufferedReader(new FileReader(groundTruthFile));
        String line = null;
        //add the image frequency pair to the tag posting list
        while ((line = reader.readLine()) != null) {
            if(line.trim().isEmpty()){
                continue;
            }
            String[] img_folders = line.split("\\s+");
            String imgID = img_folders[0];
            String folder = img_folders[1];
            boolean isFileExists = false;
            for(int i = 1; i < img_folders.length; i++){
                String imgPath = "./src/FindIO/Datasets/test/query/"+folder+"/"+imgID;
                File image = new File(imgPath);
                if(image.exists() && !image.isDirectory()){
                    isFileExists = true;
                    double[] colorHist = ColorHistExtraction.getHist(image);
                    addDoc(Common.removeExtension(imgID), colorHist);
                    break;
                }
            }
            if(!isFileExists) {
                System.out.println(Common.MESSAGE_FILE_NOTEXIST+":\t"+imgID);
            }
            index_count++;
        }
        System.out.println("Number of index: " + index_count);
        reader.close();
        closeWriter();
    }


    /**
     * Add a document. The document contains two fields: one is the element id,
     * the other is the values on each dimension
     *
     * @param imgID: image file name
     * @param colorHist: color histogram
     * */
    public void addDoc(String imgID, double[] colorHist) {

        Document doc = new Document();
        // clear the StringBuffer
        strbuf.setLength(0);
        // set new Text for payload analyzer
        long start = System.currentTimeMillis();
        for (int i = 0; i < colorHist.length; i++) {
            double histBinValue = colorHist[i];
            if(histBinValue > 0){
                strbuf.append(i + " " + histBinValue + " ");
            }
        }
        strbuf_time += (System.currentTimeMillis() - start);

        // set fields for document
        this.img_field.setStringValue(imgID);
        this.hist_field.setStringValue(strbuf.toString().trim());
        doc.add(img_field);
        doc.add(hist_field);

        try {
            MMwriter.addDocument(doc);
            System.out.println(Common.MESSAGE_FILE_INDEX_SUCCESS + imgID);
        } catch (IOException e) {
            System.err.println(Common.MESSAGE_HIST_INDEX_ERROR);
            if (test){
                e.printStackTrace();
            }
        }
    }

    public Map<String, double[]> searchImgHist(String imageID) throws Exception{
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        Query query = parser.parse(imageID);
        if(test)
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
            String[] colorBins = doc.get(fieldname2).split(" ");
            if(mapResults.get(imageName) == null){
                mapResults.put(imageName, ColorHistExtraction.getDefaultColorHist());
            }
            double[] colorHist = mapResults.get(imageName);
            for(int i = 0; i < colorBins.length - 1; i += 2){
                colorHist[Integer.parseInt(colorBins[i])] = Double.parseDouble(colorBins[i + 1]);
            }
        }
        reader.close();

        return mapResults;
    }

    /* Main Function for building the index */
    public static void main(String[] args){
        ColorHistCache colorCache = new ColorHistCache();
        try{
            colorCache.initBuilding();
            colorCache.buildCache("src/FindIO/query_groundTruth.txt");
        } catch(Throwable e) {
            System.out.println(Common.MESSAGE_HIST_INDEX_ERROR);
            if(test){
                e.printStackTrace();
            }
        }
    }
}
