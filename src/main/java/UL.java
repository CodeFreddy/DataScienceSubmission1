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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class UL {
    public List<String> runFileContent;
    public int resultsNum;
    public Map<String, HashMap<String, Float>> results;
    public QueryParser queryParser;
    public IndexReader indexReader;
    public IndexSearcher indexSearcher;

    PriorityQueue<DocResults> docQueue = new PriorityQueue<>((a, b) -> (a.score < b.score ? 1 : a .score > b.score ?  -1 : 0));


    public UL(Map<String, String> queriesStr, int resultsNum, String indexPath) throws IOException, ParseException {
        runFileContent = new ArrayList<>();
        results = new HashMap<>();
        this.resultsNum = resultsNum;

        queryParser = new QueryParser("content",new EnglishAnalyzer());
        Directory directory = FSDirectory.open(Paths.get(indexPath));
        //indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath())));

        //indexReader = indexSearcher.getIndexReader();

        indexReader = DirectoryReader.open(directory);
        indexSearcher = new IndexSearcher(indexReader);

        SimilarityBase custom = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {

                return (float) ((freq + 1 / docLen));
            }

            @Override
            public String toString() {
                return null;
            }
        };

        indexSearcher.setSimilarity(custom);

        for (Map.Entry<String,String> entry : queriesStr.entrySet()){
            String queryStr = entry.getValue();
            String queryId = entry.getKey();
            //System.out.println("Query ID: " + queryId);
            if (!results.containsKey(queryId)){
                results.put(queryId,new HashMap<>());
            }

            //for each word in a query

            for(String term: queryStr.split(" ")) {
                Term t = new Term("content", term);
                TermQuery termQuery = new TermQuery(t);
                //System.out.println("Page name: " + term);
                TopDocs topDocs = indexSearcher.search(termQuery, resultsNum);
                ScoreDoc[] scores = topDocs.scoreDocs;
                System.out.println("Score size: " + scores.length);
                for (int i = 0; i < scores.length; i++) {
                    Document doc = indexSearcher.doc(scores[i].doc);
                    String paraId = doc.get("paraid");
                    String docBody = doc.get("content");
//                    System.out.println("paraid: " + paraId);
//                    System.out.println("parabody: " + docBody);

                    List<String> unigramList = unigramAnalyze(docBody);

                    int wordsSize = getWordsSize(unigramList);
                    int docSize = unigramList.size();

                    if (!results.get(queryId).containsKey(paraId)) {
                        results.get(queryId).put(paraId, 0.0f);
                    }

                    float score = results.get(queryId).get(paraId);

                    score += (float) (scores[i].score / (docSize + wordsSize));
                    results.get(queryId).put(paraId, score);
                }
            }

        }

        for (Map.Entry<String, HashMap<String, Float>> queryResult : results.entrySet()){
            String queryId = queryResult.getKey();
            HashMap<String, Float> paraResults = queryResult.getValue();

            for (Map.Entry<String, Float> paraResult : paraResults.entrySet()) {
                String paraId = paraResult.getKey();
                float score = paraResult.getValue();
                DocResults docResult = new DocResults(paraId, score);
                docQueue.add(docResult);
            }


            DocResults d;

            int count = 0;

            while ((d = docQueue.poll()) != null && count < 100){
                runFileContent.add(queryId + "  Q0 "+d.paraId + " "+count+" "+d.score+ " Team3-UL");
                count++;
            }

            docQueue.clear();
        }


    }


    public  List<String> getList(){
        return runFileContent;
    }

    public static int getWordsSize(List<String> list){
        Set<String> set = new HashSet<>();
        for (String s : list){
            set.add(s);
        }

        return set.size();
    }

    public static List<String> unigramAnalyze(String docBoday) throws IOException {
        List<String> res = new ArrayList<>();

        Analyzer analyzer = new UnigramAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("content",docBoday);

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
}
