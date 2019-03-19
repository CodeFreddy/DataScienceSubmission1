package main.java;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Map;

public class QueryExpansionDocEntity {

    private IndexSearcher searcher;
    private Map<String,String> pageMap;
    private Map<String,String> sectionMap;
    private String INDEX_DIR;
    private int max_result;
    private QueryParser parser;
    static  private String OUTPUT_DIR="";

    public QueryExpansionDocEntity(Map<String, String> pageMap, Map<String,String> sectionMap, String indexPath, String output){
        this.pageMap = pageMap;
        this.INDEX_DIR = indexPath;
        this.max_result = 100;
        this.sectionMap = sectionMap;
        OUTPUT_DIR = output;
    }


    public void runPage() throws IOException, ParseException {
        try {
            run(pageMap,"page-QueryExpansion-Entity.run");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void runSection() throws IOException,ParseException{
        try {
            run(sectionMap,"section-QueryExpansion-Entity.run");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(Map<String,String> map,String fileName) throws Exception{

    }
}
