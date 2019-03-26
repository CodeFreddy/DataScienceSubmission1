package main.java.EntityLinking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class EntityFinder {

    public  static String getEntity(String queryStr, String indexPath){
        String  result = "";

        try {
            IndexSearcher searcher =  new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexPath).toPath()))));

            searcher.setSimilarity(new BM25Similarity());

            return getEntity(queryStr,searcher);

        } catch (IOException | ParseException e) {
            System.err.println("Error! Cannot find the entity" + e.getMessage());
        }

        return result;
    }

    public  static Map<String,Integer> reTryMap = new HashMap<>();

    //not so clear with the filed and parser
    public  static String getEntity(String queryStr, IndexSearcher searcher) throws IOException, ParseException {
        String result = "";
//
//        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
//
//
//        //fuzzyQuery implemented like edit distance
//        queryBuilder.add(new FuzzyQuery(new Term("content",queryStr)), BooleanClause.Occur.SHOULD);
//
//        Query q = queryBuilder.build();
//
        QueryParser parser = new QueryParser("content", new StandardAnalyzer());

        Query q = parser.parse(QueryParser.escape(queryStr));
        TopDocs tops = searcher.search(q,1);


        ScoreDoc[] scoreDocs = tops.scoreDocs;

        if(scoreDocs.length == 0){
            System.err.println("Cannot get relevent result");
            return queryStr;
        }else{
            ScoreDoc score = scoreDocs[0];

            Document doc = searcher.doc(score.doc);

            String name = doc.getField("content").stringValue();
            result = doc.getField("content").stringValue();

            System.out.println("Entity abstract:" +result);
            return result;
        }




    }

    public  static List<Entity> getRelatedEntity(String content) throws Exception  {
        List<Entity> list = new ArrayList<>();

            String jsonStr = SpotLight.getRelatedJson(content);
//            System.out.println(jsonStr);

            Gson gson = new GsonBuilder().setLenient().create();
            JsonParser jsonParser = new JsonParser();

            JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonStr);

            Type listType = new TypeToken<List<Entity>>() {
            }.getType();

            list = gson.fromJson(jsonObject.get("Resources"), listType);



//        if (list != null){
//            for (Entity e : list){
//                System.out.println(e.getURI());
//                String uri = e.getURI();
//
//                uri = uri.substring(uri.lastIndexOf("/")+1);
//                System.out.println(uri);
//            }
//        }

        return list;
    }
}
