package main.java.QueryExpansion;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import rita.*;
import rita.wordnet.*;
import net.sf.extjwnl.*;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;

public class QueryExpansionWordNet {
    private IndexSearcher searcher;
    private Map<String,String> pageMap;
    private Map<String,String> sectionMap;
    private String INDEX_DIR;
    private int max_result;
    private QueryParser parser;
    static  private String OUTPUT_DIR="output";
    private Dictionary wnDict = null;
    private String propsFile = "";
    private Set<String> stopWordsSet;

    public QueryExpansionWordNet(Map<String, String> pageMap,Map<String,String> sectionMap, String indexPath,String output) throws JWNLException, FileNotFoundException {
        this.pageMap = pageMap;
        this.INDEX_DIR = indexPath;
        this.max_result = 100;
        this.sectionMap = sectionMap;
        OUTPUT_DIR = output;
        this.stopWordsSet = new HashSet<>();
        wnDict = Dictionary.getDefaultResourceInstance();


        List<String> list = new ArrayList<>();

        String line = "";
        String stopWordDir = "src/resources/stop_word.cfg";
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordDir));

            while ((line = bufferedReader.readLine()) != null){
                if (!line.isEmpty()){
//                    list.add(line.replace(" ",""));
                    stopWordsSet.add(line.replace(" ",""));
                }
            }

            bufferedReader.close();

//            CharArraySet stopWord = new CharArraySet(list,true);


//            System.out.println(stopWordsSet.size());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void runPage() throws IOException, ParseException {
        try {
            run(pageMap,"page-QueryExpansionWordNet.run");
        } catch (JWNLException e) {
            e.printStackTrace();
        }

    }


    public void runSection() throws IOException,ParseException{
        try {
            run(sectionMap,"section-QueryExpansionWordNet.run");
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }

    public String getExpandedQuery(String queryStr){
//        RiWordNet wordNet = new RiWordNet("/usr/local/WordNet-3.0");
        RiWordNet wordNet = new RiWordNet("/home/tianxiu/WordNet");
        wordNet.randomizeResults(false);
        StringBuilder sb = new StringBuilder();

        String[] splitStr = queryStr.split(" ");

        for (String str : splitStr){
            if (stopWordsSet.contains(str)) continue;
            String pos = wordNet.getBestPos(str);
//                System.out.println(pos+" "+str);
            if (pos == null) {
                sb.append(str);
                sb.append(" ");
                continue;
            }
            String[] s = wordNet.getAllSynsets(str,pos);
//                System.out.println(Arrays.asList(s));
            if (s.length == 0){
                sb.append(str);
            }else{
                sb.append(s[0].toLowerCase());
            }
//
            sb.append(" ");
        }
        return sb.toString();
    }

    public void run(Map<String,String> map,String fileName) throws IOException, ParseException, JWNLException {
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));

        searcher.setSimilarity(new BM25Similarity());

        parser = new QueryParser("content", new StandardAnalyzer());


        Set<String> runFileStr = new HashSet<>();

        for (Map.Entry<String, String> entry:map.entrySet()){
            String queryStr = entry.getValue();
            String queryId = entry.getKey();


            String newQuery = getExpandedQuery(queryStr);

            Query q = parser.parse(QueryParser.escape(queryStr));

            TopDocs tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDoc = tops.scoreDocs;


            for (int i = 0; i < scoreDoc.length;i++ ){
                ScoreDoc score = scoreDoc[i];
                Document doc = searcher.doc(score.doc);

                String paraId = doc.getField("paraid").stringValue();
                float rankScore = score.score;
                int rank = i+1;
                String runStr = queryId+" Q0 "+paraId+" "+rank+ " "+rankScore+" "+"team3"+" QueryExpansionWordNet";

                if (!runFileStr.contains(runStr)){
                    runFileStr.add(runStr);
                }
            }
        }

        writeToFile(fileName,runFileStr);


    }

    private void writeToFile(String filename, Set<String> runfileStrings) {
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



}
