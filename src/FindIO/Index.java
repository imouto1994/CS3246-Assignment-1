package FindIO;

import org.apache.lucene.index.IndexWriter;

public class Index {

    static long startbuilding_time = 0;
    static long initbuilding_time = 0;
    static long totalBuilding_time = 0;
    static long strbuf_time = 0;
    static long index_count = 0;

    static boolean test = false;

    protected IndexWriter MMwriter;

    /**
     * Close the MMWriter and output the testing result
     * @throws Throwable
     */
    public void closeWriter() throws Throwable {
        MMwriter.close();
        if (test) {
            System.out.println("Indexing count:\t" + index_count);
            System.out.println("initTime:\t" + initbuilding_time);
            System.out.println("string buffer time:\t" + strbuf_time);
            System.out.println("total time:\t"
                    + (System.currentTimeMillis() - startbuilding_time));
        }
    }
}