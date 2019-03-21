package main.java.QueryExpansion;


import main.java.jgibblda.InferencerWrapper;
import main.java.jgibblda.Topic;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryExpansionLDA {

    private IndexSearcher searcher;
    private Map<String,String> pageMap;
    private Map<String,String> sectionMap;
    private String INDEX_DIR;
    private int max_result;
    private QueryParser parser;
    static  private String OUTPUT_DIR="";
    private Analyzer analyzer;

    private InferencerWrapper inferencerWrapper;


    public QueryExpansionLDA(Map<String, String> pageMap,
                             Map<String,String> sectionMap,
                             String indexPath, String output)
    {
        this.pageMap = pageMap;
        this.INDEX_DIR = indexPath;
        this.max_result = 100;
        this.sectionMap = sectionMap;
        OUTPUT_DIR = output;
        analyzer = new StandardAnalyzer();
        inferencerWrapper = new InferencerWrapper();
    }


    public void runPage() throws IOException, ParseException {
        run(pageMap,"page-QueryExpansion-LDA.run");

    }


    public void runSection() throws IOException,ParseException{
        run(sectionMap,"section-QueryExpansion-LDA.run");
    }

    public void run(Map<String,String> map,String fileName)
            throws IOException, ParseException
    {
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

            System.out.println("query is: "+queryStr);
            List<Document> docList = getDocList(scoreDoc,searcher);

            if (docList.isEmpty()){
                continue;
            }

            List<QueryTerms> queryTermsList = getDocTerms(docList,analyzer);

            QueryTerms topQueryTerms = queryTermsList.get(0);

            List<String> termList = topQueryTerms.getTerms();

           // Topic[] topics = inferencerWrapper.getTopicsByLDA(termList);

            List<String> expandedList = inferencerWrapper.getWordsByLDA(termList);
            if (expandedList.size() == 0){
                System.err.println("Topics length 0");
                continue;
            }
//            System.err.println("Topics length: "+topics.length);
//            for (Topic topic : topics){
//                expandedList.add(topic.toString());
//            }

            Query newQuery = setBoost(queryStr,expandedList);

            tops = searcher.search(newQuery, max_result);
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

    private Query setBoost(String originalQuery, List<String> expanded_list) throws ParseException {
        if (!expanded_list.isEmpty()) {

            List<String> tmp = new ArrayList<>();

            for (int i = 0; i < 5; i++){
                tmp.add(expanded_list.get(i));
            }


            String rm_str = String.join(" ", tmp);
            Query q = parser.parse(QueryParser.escape(originalQuery) + "^1.5" + QueryParser.escape(rm_str) + "^0.75");
            return q;
        } else {
            Query q = parser.parse(QueryParser.escape(originalQuery));
            return q;
        }
    }


    public List<QueryTerms> getDocTerms(List<Document> documentList, Analyzer analyzer) throws IOException {
        List<QueryTerms> res = new ArrayList<>();

        for (int i = 0; i< documentList.size(); i++) {
            Document doc = documentList.get(i);

            StringBuffer stringbuffer = new StringBuffer();
            String[] docString = doc.getValues("content");

            if (docString.length == 0) continue;

            for(int j = 0; j < docString.length;j++){
                stringbuffer.append(docString[j]+" ");
            }


            QueryTerms docTerms = new QueryTerms(stringbuffer.toString(),analyzer,1);
            res.add(docTerms);
        }

        return res;
    }

    public List<Document> getDocList(ScoreDoc[] scoreDoc,IndexSearcher searcher) throws IOException {
        List<Document> res = new ArrayList<>();

        for (int i = 0; i < scoreDoc.length;i++){
            ScoreDoc s = scoreDoc[i];

            Document doc = searcher.doc(s.doc);

            res.add(doc);
        }

        return res;
    }


}
