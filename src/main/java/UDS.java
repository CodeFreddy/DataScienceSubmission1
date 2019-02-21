package main.java;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;
import edu.unh.cs.treccar_v2.Data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.reflect.Array;
import java.util.ArrayList;


class RunFileString{
    public String queryId;
    public String paraId;
    public int rank;
    public float score;
    public String methodName;
    public  String teamName;
    RunFileString()
    {
        queryId = "";
        paraId = "";
        rank = 0;
        score = 0.0f;
        methodName = "DS-Dirichlet";
        teamName = "Team 3";
    }

    RunFileString(String qid, String pid, int r, float s)
    {
        queryId = qid;
        paraId = pid;
        rank = r;
        score = s;
        methodName = "DS-Dirichlet";
        teamName = "Team 3";
    }

    public String toString()
    {
        return (queryId + " Q0 " + paraId + " " + rank + " " + score + " " + teamName + " " + methodName);
    }
}


public class UDS {
    private static String INDEX_DIR;
    private static String OUTPUT_PATH;
    //public static String PARAGRAPH = "C:\\CS853\\programAssignment3\\test200-train\\train.pages.cbor-paragraphs.cbor";
    private  static String output = "results_uds.run";
    private int resultsNum;

    // returns IndexReader
    private static IndexReader getIndexReader(String path) throws IOException {
        return DirectoryReader.open(FSDirectory.open((new File(path).toPath())));
    }

    public static SimilarityBase getSimilarity()
    {
        SimilarityBase similarity = new SimilarityBase() {
            @Override
            protected float score(BasicStats basicStats, float v, float v1) {
                float totalTF = basicStats.getTotalTermFreq();
                return (v + 1000) / (totalTF + 1000);
            }

            @Override
            public String toString() {
                return null;
            }
        };
        return similarity;
    }

    public UDS(ArrayList<Data.Page> pageList, int resultsNum, String indexPath, String outputPath)
    {
        INDEX_DIR = indexPath;
        OUTPUT_PATH = outputPath;
        this.resultsNum = resultsNum;
        try{
            ArrayList<RunFileString> results = new ArrayList<>();
            for(Data.Page page: pageList)
            {
                String queryStr = page.getPageId();
                ArrayList<RunFileString> res = getRanked(queryStr);
                results.addAll(res);
            }
            writeArrayToFile(results);
        }catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
    }

    private ArrayList<RunFileString> getRanked(String query) throws IOException{
        IndexSearcher indexSearcher = new IndexSearcher(getIndexReader(INDEX_DIR));
        indexSearcher.setSimilarity(getSimilarity());

        QueryParser parser = new QueryParser("parabody", new EnglishAnalyzer());
        ArrayList<RunFileString> ret = new ArrayList<>();

        try{
            Query q = parser.parse(query);
            TopDocs topDocs = indexSearcher.search(q, resultsNum);
            ScoreDoc[] scores = topDocs.scoreDocs;
            for(int i = 0; i < scores.length; i++)
            {
                Document doc = indexSearcher.doc(scores[i].doc);
                String docId = doc.get("paraid");
                float score = scores[i].score;
                RunFileString tmp = new RunFileString(query, docId, i, score);
                ret.add(tmp);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return ret;
    }


    private void writeArrayToFile(ArrayList<RunFileString> list) throws IOException{
        File dir = new File(OUTPUT_PATH);
        if(!dir.exists())
        {
            if(dir.mkdir())
            {
                System.out.println("output directory generated...");
            }
        }
        File file = new File(OUTPUT_PATH + "\\" + output);
        if(file.createNewFile())
        {
            System.out.println(output + " generated...");
        }
        BufferedWriter buff = new BufferedWriter(new FileWriter(file));
        for(RunFileString line: list)
        {
            buff.write(line.toString() + "\n");
        }
        buff.close();
    }


}