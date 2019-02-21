package main.java.QueryExpansion;


import com.sun.org.apache.bcel.internal.generic.PUSH;
import main.java.QueryExpansion.JGibbLDA.LDAInferencer;
import main.java.QueryExpansion.JGibbLDA.Topic;
import org.apache.lucene.analysis.Analyzer;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class QueryExpansion {
    private static  String INDEX_DIR ="";

    public Map<String,String> queriesStr;//pageMap
    public static int max_result= 100;
    public static String OUTPUT_DIR = "output";
    public static IndexSearcher searcher;
    public QueryExpansion(Map<String,String> pageMap, String indexPath){
        this.queriesStr = pageMap;

        INDEX_DIR = indexPath;


    }


    public List<String> getResult() throws IOException, ParseException {
        List<String> res = new ArrayList<>();


        searcher = new IndexSearcher(
                DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));

        searcher.setSimilarity(new BM25Similarity());

        QueryParser parser = new QueryParser("content", new EnglishAnalyzer());

        for (Map.Entry<String,String> entry : queriesStr.entrySet()){
            String queryStr = entry.getValue();
            String queryId = entry.getKey();
            Query q = parser.parse(QueryParser.escape(queryStr));
            TopDocs tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDoc = tops.scoreDocs;


            //apply query expansion on top of the socreDoc.
            Query expandedQuery = querExpandByLDA(queryStr,tops,parser);



            TopDocs newTops = searcher.search(expandedQuery,100);

            ScoreDoc[] newScoreDoc = newTops.scoreDocs;
            for (int i = 0; i < newScoreDoc.length;i++){
                ScoreDoc score = newScoreDoc[i];
                Document doc = searcher.doc(score.doc);
                String paraId = doc.getField("paraid").stringValue();
                float rankScore = score.score;
                int rank = i + 1;

                // String runStr = "enwiki:" + queryStr.replace(" ", "%20") + " Q0 " + paraId + " " + rank + " "+ rankScore + " BM25";

                String runStr = queryId+" Q0 "+paraId+" "+rank+ " "+rankScore+" "+"team3"+" QE+BM25";
                System.out.println(runStr);
                res.add(runStr);
            }

        }

        writeToFile("QueryExpansion.run",res);

        return  res;
    }

    private  void writeToFile(String filename, List<String> runfileStrings) {
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



    public  Query querExpandByLDA(String queryStr, TopDocs hits,QueryParser parser) throws IOException, ParseException {
        List<Document> documentList = getDocument(hits);

        List<QueryTerms> termsList = getDocumentTerms(documentList,new EnglishAnalyzer());


        float max =0;
        Set<String> set = new HashSet<>();

        for (QueryTerms qt : termsList){
            Map<String,Float> tmp = qt.getScoreRank();
            for (Map.Entry<String,Float> entry : tmp.entrySet()){
                String key = entry.getKey();
                Float val = entry.getValue();

                if (val > max && set.size() < 5){
                    max = val;
                    set.add(key);
                }
            }
        }
        String[] array = Arrays.copyOf(set.toArray(), set.size(),
                String[].class);

        Topic[] topics = LDAInferencer.getInstance().extractTopicByLDA(array);
        Query expandedQuery;

        String newQueryStr = topics[0].toString();
        if (newQueryStr.length() != 0)
            expandedQuery = parser.parse(QueryParser.escape(queryStr)+"^0.6"+QueryParser.escape(newQueryStr)+"^0.4");
        else
            expandedQuery = parser.parse(QueryParser.escape(queryStr));

        return  expandedQuery;

    }

    public List<QueryTerms> getDocumentTerms(List<Document> hits, Analyzer analyzer) throws IOException {
        List<QueryTerms> docsTerms = new ArrayList<>();

        for (int i = 0; i < hits.size(); i++ ){
            Document doc = hits.get(i);

            //get the string of the document

            StringBuffer docTxBuffer = new StringBuffer();

            String[] docTxFld = doc.getValues("content");


            if (docTxFld.length == 0) continue;

            for (int j = 0; j < docTxFld.length;j++){
                docTxBuffer.append(docTxFld[i]+" ");
            }

            QueryTerms qtv = new QueryTerms(docTxBuffer.toString(),analyzer,i+1);

            docsTerms.add(qtv);

        }

        return docsTerms;
    }

    public List<Document> getDocument(TopDocs hits) throws IOException {
        List<Document> vhits = new ArrayList<>();

        ScoreDoc[] scoreDoc = hits.scoreDocs;
        for (int i = 0; i < scoreDoc.length;i++){
            ScoreDoc score = scoreDoc[i];
            Document doc = searcher.doc(score.doc);
            vhits.add(doc);
        }


        return vhits;
    }
}
