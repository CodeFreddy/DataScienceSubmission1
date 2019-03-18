package main.java;

import main.java.EntityLinking.Entity;
import main.java.EntityLinking.EntityFinder;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QueryExpansionQueryEntity {
    private IndexSearcher searcher;
    private Map<String,String> pageMap;
    private Map<String,String> sectionMap;
    private String INDEX_DIR;
    private int max_result;
    private QueryParser parser;
    static  private String OUTPUT_DIR="";


    public QueryExpansionQueryEntity(Map<String, String> pageMap, Map<String,String> sectionMap, String indexPath, String output){
        this.pageMap = pageMap;
        this.INDEX_DIR = indexPath;
        this.max_result = 100;
        this.sectionMap = sectionMap;
        OUTPUT_DIR = output;
    }


    public void runPage() throws IOException, ParseException {
        run(pageMap,"page-QueryExpansion.run");

    }


    public void runSection() throws IOException,ParseException{
        run(sectionMap,"section-QueryExpansion.run");
    }

    public void run(Map<String,String> map,String fileName) throws IOException, ParseException{
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));
        searcher.setSimilarity(new BM25Similarity());
        parser = new QueryParser("content", new StandardAnalyzer());

        ArrayList<String> runFileStr = new ArrayList<>();

        for (Map.Entry<String, String> entry:map.entrySet()){
            String queryStr = entry.getValue();
            String queryId = entry.getKey();


        }
    }

    public List<String> getExpanedQuery(int top, String queryStr) throws Exception {
        //for both page and section query

        // to identify page or section
        boolean isSection = false;


        Map<String, Float> entityScore = new HashMap<>();
        List<String> expandedQueryList = new ArrayList<>();
        List<String> queryTerms = new ArrayList<>();

        if (queryStr.contains("/")){
            isSection = true;
            queryTerms = Arrays.asList(queryStr.split("/"));

        }

        //for page
        if (!isSection){
            String firstEntity = EntityFinder.getEntity(queryStr.replace(" ","_").toLowerCase(),INDEX_DIR);

            if (!firstEntity.isEmpty()){
                List<Entity> entities = EntityFinder.getRelatedEntity(firstEntity);

                if (entities == null) return expandedQueryList;

                if (entities.size() > 5){
                    for (Entity entity : entities){
                        entityScore.put(entity.getSurfaceForm(),entity.getSimilarityScore());
                    }
                }else if (entities.size() > 0 && entities.size() <= 5){
                    for (Entity entity : entities){
                        expandedQueryList.add(entity.getSurfaceForm());
                    }

                    return  expandedQueryList;
                }else{
                    return expandedQueryList;
                }
            }
        }
        //for section
        else{

        }


        return expandedQueryList;

    }
}
