package main.java;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UJM {

    public List<String> runFileContent;
    public int resultsNum;
    public Map<String, HashMap<String, Float>> results;
    public QueryParser queryParser;
    public IndexReader indexReader;
    public IndexSearcher indexSearcher;

    PriorityQueue<DocResults> docQueue = new PriorityQueue<>((a, b) -> (a.score < b.score ? 1 : a .score > b.score ?  -1 : 0));

    public UJM(Map<String, String> queriesStr, int resultsNum, String indexPath) throws IOException{
        runFileContent = new ArrayList<>();
        results = new HashMap<>();
        this.resultsNum = resultsNum;

        queryParser = new QueryParser("content",new EnglishAnalyzer());

        indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath())));

        indexReader = indexSearcher.getIndexReader();
        float sumTotalTermFreq = indexReader.getSumTotalTermFreq("content");

        SimilarityBase custom = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {

                return (float) (0.9*(freq/ docLen));
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
            if (!results.containsKey(queryId)){
                results.put(queryId,new HashMap<>());
            }

            //for each word in a query

            for (String term : queryStr.split(" ")){
                Term t = new Term("content",term);
                TermQuery termQuery = new TermQuery(t);


                TopDocs topDocs = indexSearcher.search(termQuery,resultsNum);
                float totalTermFreq = indexReader.totalTermFreq(t);

                ScoreDoc[] scores = topDocs.scoreDocs;

                for (int i = 0; i < scores.length;i++){
                    Document doc = indexSearcher.doc(scores[i].doc);
                    String paraId = doc.get("paraid");

                    if (!results.get(queryId).containsKey(paraId)){
                        results.get(queryId).put(paraId,0.0f);
                    }

                    float score = results.get(queryId).get(paraId);

                    //score += (float)(scores[i].score / (docSize + wordsSize));
                    score += (float) Math.log10((scores[i].score + (0.1 * (totalTermFreq / sumTotalTermFreq))));
                    results.get(queryId).put(paraId, score);
                }

            }
        }

        for (Map.Entry<String, HashMap<String, Float>> queryResult : results.entrySet()){
            String queryId = queryResult.getKey();
            HashMap<String, Float> paraResults = queryResult.getValue();

            for (Map.Entry<String, Float> paraResult : paraResults.entrySet()) {
                String paraId = paraResult.getKey();
                //float score = paraResult.getValue();
                float score = (float)Math.pow(10, paraResult.getValue());
                DocResults docResult = new DocResults(paraId, score);
                docQueue.add(docResult);
            }


            DocResults d;

            int count = 0;

            while ((d = docQueue.poll()) != null && count < 100){
                runFileContent.add(queryId + "  Q0 "+d.paraId + " "+count+" "+d.score+ " Team3-UL-JM");
                count++;
            }

            docQueue.clear();
        }
    }


    public  List<String> getList(){
        return runFileContent;
    }

}