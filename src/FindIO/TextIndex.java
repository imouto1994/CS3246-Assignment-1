package FindIO;

/**
 * Created by Beyond on 10/11/2014 0011.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Index all text files under a directory.
 */
public class TextIndex extends Index{

    private File indexFile;
    private MMapDirectory MMapDir;
    private IndexWriterConfig config;
    private IndexReader indexReader;
    private AtomicReader areader;
    private String fieldname1 = "tag";
    private String fieldname2 = "img_freq";

    private Field tag_field;
    private Field img_field;

    private TextAnalyzer textAnalyzer;

    // Maximum Buffer Size
    private int MAX_BUFF = 48;

    // to create the TextField for vector insertion
    private StringBuffer strbuf;
    // to create the data_field
    private StringBuffer databuf;

    private FileInputStream binIn;

    public TextIndex() {
        setIndexfile("./src/FindIO/index/textIndex");
    }

    public void setIndexfile(String indexfilename) {
        this.indexFile = new File(indexfilename);
        try {
            this.textAnalyzer = new TextAnalyzer();
        } catch(IOException e){
            System.out.println(Common.MESSAGE_TEXT_ANALYZER_ERROR);
        }
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

        tag_field = new TextField(this.fieldname1, "-1", Field.Store.YES);
        img_field = new TextField(this.fieldname2, "-1", Field.Store.YES);

        strbuf = new StringBuffer();
        databuf = new StringBuffer();
        initbuilding_time = System.currentTimeMillis() - startbuilding_time;
    }


    public void buildIndex(String dataFile) throws Throwable{

        BufferedReader reader = new BufferedReader(new FileReader(dataFile));
        HashMap<String, ArrayList<FindIOPair>> tagImgMap = new HashMap<String, ArrayList<FindIOPair>>();
        String line = null;

        //add the image frequency pair to the tag posting list
        while ((line = reader.readLine()) != null) {
            String[] img_tags = line.split(" ");
            String imgID = Common.removeExtension(img_tags[0]);
            for(int i = 1; i < img_tags.length; i ++) {
                String tag = img_tags[i];
                FindIOPair image_freq = new FindIOPair(imgID, 1);

                if(!tagImgMap.containsKey(tag)){
                    ArrayList<FindIOPair> imgPairList = new ArrayList<FindIOPair>();
                    imgPairList.add(image_freq);
                    tagImgMap.put(tag, imgPairList);
                } else {
                    ArrayList<FindIOPair> imgPairList = tagImgMap.get(tag);
                    imgPairList.add(image_freq);
                }
            }
        }

        for(String tag : tagImgMap.keySet()){
            ArrayList<FindIOPair> imgPairList = tagImgMap.get(tag);
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
    public void addDoc(String tag, ArrayList<FindIOPair> imgPairList) {

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
        this.tag_field.setStringValue(this.textAnalyzer.getStem(tag));
        this.img_field.setStringValue(strbuf.toString());
        doc.add(tag_field);
        doc.add(img_field);

        try {
            MMwriter.addDocument(doc);
        } catch (IOException e) {
            System.err.println("index writer error");
            if (test)
                e.printStackTrace();
        }
    }



    public Map<String, double[]> searchText(String queryString) throws Exception{
        List<String> terms = Arrays.asList(queryString.trim().split("\\s+"));

        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        Query query = parser.parse(queryString);
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
            String tag = doc.get(fieldname1);
            int index = terms.indexOf(tag);
            if(index == -1){
                continue;
            }
            String[] images = doc.get(fieldname2).split(",");
            for(String image : images) {
                String[] infos = image.trim().split("\\s+");
                String imageName = infos[0];
                String freq = infos[1];
                if(mapResults.get(imageName) == null){
                    mapResults.put(imageName, new double[terms.size()]);
                }
                double[] docTerms = mapResults.get(imageName);
                docTerms[index] = Double.parseDouble(freq);
            }
        }
        reader.close();

        return mapResults;
    }


    /**
     * update score mainly used for relevance feedback, the input should be stemmed
     * @param imageID
     * @param tag_score_pairs
     * @throws Throwable
     */
    public void updateScore(String imageID, ArrayList<FindIOPair> tag_score_pairs) throws Throwable{
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile));
        IndexSearcher searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        // :Post-Release-Update-Version.LUCENE_XY:
        QueryParser parser = new QueryParser(fieldname1, analyzer);

        for(FindIOPair pair : tag_score_pairs){
            String tag = pair.getID();
            double add_score = pair.getValue();

            Query query = parser.parse(tag);

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
            if(hits.length == 0){ //It's a new tag
                Document doc = new Document();
                String img_score = imageID+" "+add_score+",";

                // set fields for document
                this.tag_field.setStringValue(this.textAnalyzer.getStem(tag));
                this.img_field.setStringValue(img_score);
                doc.add(tag_field);
                doc.add(img_field);
                MMwriter.addDocument(doc);
            } else {
            //The tag is included in the index
                int docId = hits[0].doc;

                //retrieve the old document
                Document doc = searcher.doc(docId);

                //replacement field value
                String currentScores = doc.get(fieldname2);
                String[] img_score_pairs = currentScores.split(",");
                StringBuilder stringBuilder = new StringBuilder();

                boolean isImageContained = false;

                for(String img_score_pair : img_score_pairs){
                    String[] img_score = img_score_pair.split(" ");
                    String img = img_score[0];
                    double old_score = Double.valueOf(img_score[1]);
                    double new_score = old_score + add_score;
                    if(img.equals(imageID)){
                        img_score_pair = img+" "+new_score;
                        isImageContained = true;
                    }
                    stringBuilder.append(img_score_pair+",");
                }

                if(!isImageContained) { //If the image was not covered by the tag, append it to the tail
                    stringBuilder.append(imageID+" "+add_score+",");
                }

                //remove all occurrences of the old field
                doc.removeFields(fieldname2);

                this.img_field.setStringValue(stringBuilder.toString());
                if(test)
                    System.out.println(stringBuilder.toString());
                //insert the replacement
                doc.add(img_field);
                Term tagTerm = new Term(this.fieldname1, tag);
                MMwriter.updateDocument(tagTerm, doc);
            }

            MMwriter.commit();

            reader.close();
            closeWriter();
        }
    }

    public static void main(String[] args){
        TextIndex textIndex = new TextIndex();
        textIndex.setIndexfile("./src/FindIO/index/textIndex");
        try{
            textIndex.initBuilding();
            textIndex.buildIndex("./src/FindIO/Datasets/train/image_tags.txt");
        } catch(Throwable e) {
            System.out.println(Common.MESSAGE_TEXT_INDEX_ERROR);
            if(test)
                e.printStackTrace();
        }

        try{
            Map<String, double[]> resultMap = textIndex.searchText("3");
            for(Map.Entry<String, double[]> entry : resultMap.entrySet()){
                String imageName = entry.getKey();
                double[] scores = entry.getValue();
                System.out.print(imageName+"\t");
                for (double score : scores){
                    System.out.print(score+" ");
                }
                System.out.println();
            }
        }catch(Throwable e) {
            System.out.println(Common.MESSAGE_TEXT_INDEX_ERROR);
            if(test)
                e.printStackTrace();
        }

//        String imageID = "0087_2173846805";
//        String tag = "panda";
//        double added_score = 1.0;
//        try{
//            textIndex.initBuilding();
//            ArrayList<FindIOPair> tag_scores_pairs = new ArrayList<FindIOPair>();
//            FindIOPair pair = new FindIOPair(tag, added_score);
//            tag_scores_pairs.add(pair);
//            textIndex.updateScore(imageID, tag_scores_pairs);
//        } catch(Throwable e){
//            System.out.println(Common.MESSAGE_TEXT_UPDATE_ERROR);
//            e.printStackTrace();
//        }
    }
}
