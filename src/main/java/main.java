package main.java;

import main.java.QueryExpansion.QueryExpansion;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

class Main {

    static private String INDEX_DIRECTORY = "/Users/xin/Documents/19Spring/DS/index";
    private String OUTPUT_DIR = "output";
    static final private int Max_Results = 100;

    static IndexData indexer;

    public static void main(String[] args) throws IOException, ParseException {
        System.setProperty("file.encoding", "UTF-8");

        String queryPath = "C:\\CS953\\Assignment1\\queries\\benchmarkY1-train.v2.0.tar\\benchmarkY1\\benchmarkY1-train\\train.pages.cbor-outlines.cbor";

        String dataPath = "/Users/xin/Documents/19Spring/DS/paragraphCorpus/dedup.articles-paragraphs.cbor";




        //INDEX_DIRECTORY = args[0];
        //queryPath = args[1];
        //dataPath = args[2];


       // indexer = new IndexData(INDEX_DIRECTORY, dataPath);
        QueryData queryData = new QueryData(queryPath);
//
        Map<String,String> pageMap = queryData.getAllPageQueries();
        Map<String,String> sectionMap = queryData.getAllSectionQueries();

        // Store all query strings temporarily.


        System.out.println("Got " + pageMap.size() + " pages and " + sectionMap.size() + " sections.");

        // Lucene Search


        SearchData searcher = new SearchData(INDEX_DIRECTORY, pageMap, sectionMap, Max_Results);


        QueryExpansion qe = new QueryExpansion(pageMap,INDEX_DIRECTORY);

        qe.getResult();

        
        System.out.println("Finished");
    }
}
