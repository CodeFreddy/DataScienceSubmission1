package main.java;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class UJM {

    private static String INDEX_DIR;
    private static String OUTPUT_PATH;
    //public static String PARAGRAPH = "C:\\CS853\\programAssignment3\\test200-train\\train.pages.cbor-paragraphs.cbor";
    private  static String output = "";
    private int resultsNum;
    private String teamName = "Team3";
    private String methodName = "UnigramLanguageModel-JM-Smoothing";
    // returns IndexReader
    private static IndexReader getIndexReader(String path) throws IOException {
        return DirectoryReader.open(FSDirectory.open((new File(path).toPath())));
    }

    public static SimilarityBase getSimilarity() throws IOException {
        SimilarityBase freqSim = new SimilarityBase() {
            @Override
            protected float score(BasicStats stats, float freq, float docLen) {
                return freq;
            }

            @Override
            public String toString() {
                return null;
            }
        };
        return freqSim;
    }

    public UJM(Map<String, String> queriesStr, int resultsNum, String indexPath, String outputPath, String outputName)
    {
        INDEX_DIR = indexPath;
        OUTPUT_PATH = outputPath;
        this.resultsNum = resultsNum;
        output = outputName;
        HashMap<String, Float> results = new HashMap<>();
        ArrayList<String> runFileList = new ArrayList<>();
        try{

            for(Map.Entry<String,String> entry : queriesStr.entrySet())
            {
                String queryStr = entry.getValue();
                String queryId = entry.getKey();
                results = getRanked(queryStr);
                int i = 0;
                for(Map.Entry<String, Float> entry1: results.entrySet())
                {
                    String runFileString = queryId + " Q0 " + entry1.getKey() + " " + i++ + " " + entry1.getValue() + " " + teamName + " " + methodName;
                    runFileList.add(runFileString);
                }
            }

        }catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
        writeToRunFile(runFileList, outputPath+"/"+outputName);
    }

    private HashMap<String, Float> getRanked(String queryStr) throws IOException{
        IndexReader indexReader = getIndexReader(INDEX_DIR);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        float lambda = (float)0.9;
        indexSearcher.setSimilarity(getSimilarity());

        QueryParser parser = new QueryParser("content", new EnglishAnalyzer());
        HashMap<String, Float> doc_score = new HashMap<>();
        try {
            Query q = parser.parse(QueryParser.escape(queryStr));
            TopDocs topDocs = indexSearcher.search(q, resultsNum);
            ScoreDoc[] hits = topDocs.scoreDocs;

            for(int i = 0; i < hits.length; i++)
            {
                Document doc = indexSearcher.doc(hits[i].doc);
                String docId = doc.getField("paraid").stringValue();
                String docBody = doc.get("content");
                ArrayList<Float> pwt = new ArrayList<>();

                ArrayList<String> unigramList = unigramAnalyze(docBody);
                ArrayList<String> queryList = unigramAnalyze(queryStr);

                //size of vocabulary
                int sizeOfDoc = unigramList.size();


                for(String term : queryList)
                {
                    Term t = new Term("content", term);
                    float totalTermFreq = indexReader.totalTermFreq(t);
                    int tf = countExactTermFreq(term, unigramList);
                    float p = JMSmoothing(tf, sizeOfDoc, lambda, totalTermFreq);
                    pwt.add(p);

                }
                float score = getScoreWithLog(pwt);
                doc_score.put(docId, score);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return sortByValue(doc_score);
    }

    private static float JMSmoothing(int tf_given_term, int all_term_tf, float lambda, float totalTermFreq) {
        float p = (float) lambda * tf_given_term / all_term_tf + (1-lambda)* totalTermFreq;
        return p;
    }

    // Get exact count.
    private static int countExactTermFreq(String term, ArrayList<String> list) {
        int occurrences = Collections.frequency(list, term);
        return occurrences;
    }

    // Get score from list of p.
    private static float getScoreWithLog(ArrayList<Float> p_list) {
        float score = 0;
        for (Float wt : p_list) {
            score = (float) ((float) score + Math.log(wt));
        }
        return score;

    }

    // Sort Descending HashMap<String, float>Map by its value
    private static HashMap<String, Float> sortByValue(Map<String, Float> unsortMap) {

        List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {

            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        HashMap<String, Float> sortedMap = new LinkedHashMap<String, Float>();
        for (Map.Entry<String, Float> entry : list)

        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static ArrayList<String> unigramAnalyze(String docBody) throws IOException {
        ArrayList<String> res = new ArrayList<>();

        Analyzer analyzer = new UnigramAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("content",docBody);

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        while (tokenStream.incrementToken()){
            String str = charTermAttribute.toString();
            res.add(str);
        }

        tokenStream.end();
        tokenStream.close();
        return  res;
    }

    public static void writeToRunFile(ArrayList<String> strList, String path) {
        // write to run file.

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;

        try {
            fWriter = new FileWriter(path);
            bWriter = new BufferedWriter(fWriter);

            for (String line : strList) {

                bWriter.write(line);
                bWriter.newLine();
            }

            System.out.println("Write all ranking result to run file: " + path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bWriter != null) {
                    bWriter.close();
                }
                if (fWriter != null) {
                    fWriter.close();
                }
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }

    }


}