package FindIO;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.regex.*;
import java.util.*;

/**
 * Created by Beyond on 10/15/2014 0015.
 */
public class TextAnalyzer {

    //stop words
    private HashMap<String,Integer> stopmap;
    //idf score
    private HashMap<String, Double> idfmap;
    //tf*idf score
    private HashMap<String,Double> tf_idf_map;
    //for stemming
    private HashMap<String, String> stemmap;
    //analysis result
    public Vector<String> core_words;
    public Vector<String> non_core_words;

    //percentage of core words
    Double percentage = 0.2;

    public TextAnalyzer() throws IOException {

        //read the stoplist
        System.out.println("Loading Stoplist...");
        readStoplist();
        //read the idf list
        System.out.println("Loading IDF...");
        readIDF();
        System.out.println("Loading Dic...");
        //preparation for stemming
        prepareStemming();

        core_words = new Vector<String> ();
        non_core_words = new Vector<String> ();
        tf_idf_map = new HashMap<String,Double> ();

    }
    private void readIDF() throws IOException {

        idfmap = new HashMap<String,Double> ();
        BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream("./src/FindIO/Tool/idf.eng")));
        String line = "";
        //the first line is additional information
        buf.readLine();
        //start from the second line
        while((line = buf.readLine()) != null) {
            String parts[] = line.split("\t");
            idfmap.put(parts[0].trim(), Double.valueOf(parts[1].trim()));
        }
    }

    private void readStoplist() throws IOException {

        stopmap = new HashMap<String,Integer> ();
        BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream("./src/FindIO/Tool/stopwordslist")));
        String line = "";
        while((line = buf.readLine()) != null) {
            stopmap.put(line, null);
        }
    }

    private void prepareStemming()throws IOException {

        stemmap = new HashMap<String, String> ();
        BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream("./src/FindIO/Tool/word-stem")));
        String line = "";
        while((line = buf.readLine()) != null) {

            String parts[] = line.split("\t");
            stemmap.put(parts[0], parts[1]);
        }
    }

    public String getStem(String in) {

        if(stemmap.containsKey(in))
            return stemmap.get(in);
        else
            return in;
    }


    /**
     * pre-processing of the sentence
     * e.g.		we'd => we
     *  		Cool! This is an example. => cool this is an example
     * */
    public String getTokenized(String in) {

        StringBuffer out = new StringBuffer();
//		System.out.println(in);
        int i = 0;
        while(i < in.length()) {
            //deal with the  punctuation in the sentence
            if(in.charAt(i) == '.'
                    || in.charAt(i) == ','
                    || in.charAt(i) == '!'
                    || in.charAt(i) == '?') {
                //if the char is at the end of sentence
                if(i == in.length() - 1)
                    i++;
                    //else we should consider special cases such as "U.S.A", "2.3"
                else if(Character.isWhitespace(in.charAt(i + 1)))
                    i += 1;
            }
            //remove all the quotation marks
            else if(in.charAt(i) == '\"')
                i++;
                //remove all the abbreviation
            else if(in.charAt(i) == '\'') {
                while(!Character.isWhitespace(in.charAt(i)) && i < in.length())
                    i++;
            }
            //store the rest
            else {
                out.append(Character.toLowerCase(in.charAt(i)));
                i++;
            }
        }
//		System.out.println(out.toString());
        return out.toString();
    }

    /**
     * get word stem and filter the stop word
     * */
    public String filterWords(String instr) {

        String words[] = getTokenized(instr).split(" ");
        String outstr = "";
        for(int i = 0; i < words.length; i++) {
            words[i] = getStem(words[i]);
            if(!stopmap.containsKey(words[i]))
                outstr += words[i] + " ";
        }
        return outstr;
    }




    /**
     *get tf-idf score
     * */
    public void analyzeLine (String instr) {

        //clear the map for new input string
        tf_idf_map.clear();
        core_words.removeAllElements();
        non_core_words.removeAllElements();

        System.out.println("analyzing...");
        long start = System.currentTimeMillis();
        //tokenization
        instr = getTokenized(instr);
        //regex
        Pattern p = Pattern.compile("([^\\ ]+)[\\ |.]");
        Matcher m = p.matcher(instr);

        while(m.find()) {
            String word = m.group(1);
            //stemming: change the format of the words
            //e.g. making => make goes => go
            word = getStem(word);
            //filter the stop words
            if(stopmap.containsKey(word) == false && idfmap.containsKey(word)) {
                //update the tf
                if(tf_idf_map.containsKey(word)) {
                    double freq = tf_idf_map.get(word);
                    tf_idf_map.put(word, freq+1);
                }
                else
                    tf_idf_map.put(word, 1.0);
            }
        }

        //sort the tf_idf_map on score
        List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(tf_idf_map.entrySet());
        for(int i = 0; i < list.size(); i++) {
            String key = list.get(i).getKey();
            Double tf_idf = list.get(i).getValue() * idfmap.get(key);
            tf_idf_map.put(key, tf_idf);
            System.out.println(key+" "+tf_idf);
        }

        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                //get idf score and multiply the tf
                double delta = o1.getValue() - o2.getValue();
                if (delta > 0)
                    return -1;
                else if (Math.abs(delta) < 0.00000001)

                    return 0;
                else
                    return 1;
            }
        });

        //20% for core index
        int i;
        for(i = 0; i < list.size() * percentage; i++)
            core_words.add(list.get(i).getKey());
        for(;i < list.size(); i++)
            non_core_words.add(list.get(i).getKey());

        long end = System.currentTimeMillis();
        System.out.println("analyzation done! Time(s): " + (end-start)/1000);
		/*for(i = 0; i < list.size(); i++)
			System.out.println(list.get(i).getKey()+"--"+list.get(i).getValue());*/

    }

    //for testing
    public static void main (String a[])throws IOException {
        TextAnalyzer analyzer = new TextAnalyzer();
		System.out.println(analyzer.filterWords("cool! This is an example, a good one. We'd better to get it down. and a bear"));
        analyzer.analyzeLine("cool! This is an example, a good car. We'd better to get bear down . and a ");
        for(String coreWord : analyzer.core_words){
            System.out.print(coreWord+" ");
        }
        System.out.println();
        for(String noncoreword : analyzer.non_core_words){
            System.out.print(noncoreword+" ");
        }
        System.out.println();
		/*
		String path = "E:/MyFiles/Research/code/Corecorpus/corpus/2M.eng";
		BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line = "";
		int linenum = 0,wordnum = 0,stemnum = 0;
		HashMap<String,String> map = new HashMap<String,String>();
		while((line = buf.readLine()) != null) {

			linenum++;
			String [] words = line.split(" ");
			wordnum += words.length;
			for(int i = 0; i < words.length; i++) {
				words[i] = analyzer.getStem(words[i]);
				if(map.containsKey(words[i]) == false)
				{
					System.out.println(words[i]);
					stemnum++;
				}
				map.put(words[i], null);


			}
		}
		System.out.println(map.size());
		System.out.println(stemnum);
		System.out.println(wordnum);*/
    }
}
