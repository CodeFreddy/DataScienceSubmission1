package main.java;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Bigram_variation {
    private static final int max_results = 100;

    public static ArrayList<String> getSearchResult(Map<String, String> queriesStr, String indeDir) throws IOException, ParseException {
        System.out.println("Retriving results for " + queriesStr.size() + " queries...");
        ArrayList<String> runFileStr = new ArrayList<>();
        IndexSearcher searcher = null;
        int duplicate = 0;
        try {
            searcher = new IndexSearcher(
                    DirectoryReader.open(FSDirectory.open((new File(indeDir).toPath()))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        searcher.setSimilarity(new BM25Similarity());

        QueryParser parser = new QueryParser("content", new EnglishAnalyzer());
        QueryParser parser2 = new QueryParser("bigram", new EnglishAnalyzer());

        for (Map.Entry<String, String> entry : queriesStr.entrySet()) {

            String queryStr = entry.getValue();
            String queryId = entry.getKey();
            // Get bigram list for query
            ArrayList<String> bigram_list = BigramIndex.analyzeByBigram(queryStr);

            // Create a HashMap to compute score for each document.
            // <documentId,score>
            HashMap<String, Float> score_map = new HashMap<String, Float>();

            if (bigram_list.isEmpty()) {
                System.out.println(queryStr + " ===>Single Word query found.");
                bigram_list.add(queryStr);
            }

            // Query against bigram field with BM25
            for (String term : bigram_list) {
                Query q = parser2.parse(QueryParser.escape(term));
                TopDocs tops = searcher.search(q, max_results);
                ScoreDoc[] scoreDoc = tops.scoreDocs;
                for (int i = 0; i < scoreDoc.length; i++) {
                    ScoreDoc score = scoreDoc[i];
                    Document doc = searcher.doc(score.doc);
                    String paraId = doc.getField("paraid").stringValue();
                    float rankScore = score.score;

                    if (score_map.keySet().contains(paraId)) {
                        score_map.put(paraId, score_map.get(paraId) + rankScore);

                    } else {
                        score_map.put(paraId, rankScore);
                    }

                }
            }

            // Query against content field with BM25 and combine results with
            // bigram query.
            Query q = parser.parse(QueryParser.escape(queryStr));
            TopDocs tops = searcher.search(q, max_results);
            ScoreDoc[] scoreDoc = tops.scoreDocs;
            for (int i = 0; i < scoreDoc.length; i++) {
                ScoreDoc score = scoreDoc[i];
                Document doc = searcher.doc(score.doc);
                String paraId = doc.getField("paraid").stringValue();
                float rankScore = score.score;

                if (score_map.keySet().contains(paraId)) {
                    score_map.put(paraId, score_map.get(paraId) + rankScore);

                } else {
                    score_map.put(paraId, rankScore);
                }

            }

            int rank = 1;
            for (Map.Entry<String, Float> entry1 : BigramIndex.getTopValuesInMap(score_map, max_results).entrySet()) {
                String runStr = queryId + " Q0 " + entry1.getKey() + " " + rank + " " + entry1.getValue() + " FreqBigram";
                rank++;
                if (runFileStr.contains(runStr)) {
                    duplicate++;

                } else {
                    runFileStr.add(runStr);
                }

            }

        }

        System.out.println("Frequent Bigram got " + runFileStr.size() + " results.");
        return runFileStr;
    }
}
