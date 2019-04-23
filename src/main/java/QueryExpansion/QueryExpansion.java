package main.java.QueryExpansion;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;

public class QueryExpansion {
    private IndexSearcher searcher;
    private Map<String,String> pageMap;
    private Map<String,String> sectionMap;
    private String INDEX_DIR;
    private int max_result;
    private QueryParser parser;
    static  private String OUTPUT_DIR="";
    public QueryExpansion(Map<String, String> pageMap,Map<String,String> sectionMap, String indexPath,String output){
        this.pageMap = pageMap;
        this.INDEX_DIR = indexPath;
        this.max_result = 100;
        this.sectionMap = sectionMap;
        OUTPUT_DIR = output;

    }

    public QueryExpansion()
    {

    }



    public void runPage() throws IOException, ParseException {
        run(pageMap,"page-QueryExpansion.run");

    }


    public void runSection() throws IOException,ParseException{
        run(sectionMap,"section-QueryExpansion.run");
    }
    public void run(Map<String,String> map,String fileName) throws IOException, ParseException {

        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));

        searcher.setSimilarity(new BM25Similarity());

        parser = new QueryParser("content", new StandardAnalyzer());
        ArrayList<String> runFileStr = new ArrayList<String>();
        for (Map.Entry<String, String> entry:map.entrySet()){
            String queryStr = entry.getValue();
            String queryId = entry.getKey();
            Query q = parser.parse(QueryParser.escape(queryStr));


            TopDocs tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDoc = tops.scoreDocs;

            List<String> expandQueryList = expandQueryByRocchio(5,scoreDoc);


            Query q_rm = setBoost(queryStr,expandQueryList);

            tops = searcher.search(q_rm, max_result);
            ScoreDoc[] newScoreDoc = tops.scoreDocs;

            for (int i = 0; i < newScoreDoc.length;i++ ){
                ScoreDoc score = newScoreDoc[i];
                Document doc = searcher.doc(score.doc);

                String paraId = doc.getField("paraid").stringValue();
                float rankScore = score.score;
                int rank = i+1;
                String runStr = queryId+" Q0 "+paraId+" "+rank+ " "+rankScore+" "+"team3"+" QueryExpansion";

                if (!runFileStr.contains(runStr)){
                    runFileStr.add(runStr);
                }
            }

        }

        writeToFile(fileName,runFileStr);

    }

    private Query setBoost(String originalQuery, List<String> expanded_list) throws ParseException {
        if (!expanded_list.isEmpty()) {
            String rm_str = String.join(" ", expanded_list);
            Query q = parser.parse(QueryParser.escape(originalQuery) + "^1.5" + QueryParser.escape(rm_str) + "^0.75");
            return q;
        } else {
            Query q = parser.parse(QueryParser.escape(originalQuery));
            return q;
        }
    }

    private void writeToFile(String filename, ArrayList<String> runfileStrings) {
        String fullpath = OUTPUT_DIR + "/" + filename;

        try (FileWriter runfile = new FileWriter(new File(fullpath))) {
            for (String line : runfileStrings) {
                runfile.write(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("Could not open " + fullpath);
        }

        System.out.println("wrote file to "+ OUTPUT_DIR);
    }

    public List<String> expandQueryByRocchio(int top, ScoreDoc[] scoreDocs) throws IOException {
        List<String> expandedList = new ArrayList<>();
        Map<String,Float> term_map = new HashMap<>();


        for (int i = 0; i < scoreDocs.length;i++){
            ScoreDoc score = scoreDocs[i];
            Document doc = searcher.doc(score.doc);
            String paraBody = doc.getField("content").toString();

            //document term vector
            List<String> unigram_list = analyze(paraBody);

            int rank = i+1;

            float p = (float) 1 / (rank + 1);


            for (String termStr : unigram_list){
                //tf
                int tf_w = getFreq(termStr, unigram_list);
                //wrong length, document length
                int tf_list = scoreDocs.length;
                float term_score = p * ((float) tf_w / tf_list);
                if (term_map.keySet().contains(termStr)) {
                    //term_map.put(termStr, term_map.get(termStr) + term_score);
                    continue;

                } else {
                    term_map.put(termStr, term_score);
                }
            }



        }
        Set<String> termSet = getTop(term_map, 5);

        expandedList.addAll(termSet);

        return expandedList;
    }

    public static Set<String> getTop(Map<String, Float> unsortMap, int k) {
        List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {

            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<String, Float> Map = new LinkedHashMap<>();

        int i = 0;
        for (Map.Entry<String, Float> entry : list)

        {
            if (i < k || k == 0) {
                Map.put(entry.getKey(), entry.getValue());
                i++;
            } else {
                break;
            }
        }

        return Map.keySet();
    }


    private int getFreq(String term, List<String> list) {
        int frequency = Collections.frequency(list, term);
        return frequency;
    }

    public CharArraySet getStopWordSet(){
        //String stopWordDir = "/home/xl1044/ds/Query_Expansion/QueryExpaison/File/stop_word.cfg";
        String stopWordDir = "src/resources/stop_word.cfg";

        List<String> list = new ArrayList<>();

        String line = "";

        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordDir));

            while ((line = bufferedReader.readLine()) != null){
                if (!line.isEmpty()){
                    list.add(line.replace(" ",""));
                }
            }

            bufferedReader.close();

            CharArraySet stopWord = new CharArraySet(list,true);

            return  stopWord;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private  List<String> analyze(String inputStr) throws IOException{
        List<String> strList = new ArrayList<>();
        //double check with the token
        Analyzer test = new EnglishAnalyzer(getStopWordSet());

        TokenStream tokenizer = test.tokenStream("content", inputStr);

        CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
        tokenizer.reset();
        while (tokenizer.incrementToken()) {
            String token = charTermAttribute.toString();
            strList.add(token);
        }
        tokenizer.end();
        tokenizer.close();

        return strList;
    }

    public Map<String, Float> combine(Map<String, Float> originalQuery, Map<String, Float> expanededQuery){

        for (Map.Entry<String,Float> entry : originalQuery.entrySet()){
            String term = entry.getKey();
            float weight = entry.getValue();
            if (expanededQuery.containsKey(term)){
                expanededQuery.put(term,expanededQuery.get(term)+weight);
            }
            else{
                expanededQuery.put(term,weight);
            }
        }

        return expanededQuery;


    }

}
