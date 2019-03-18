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
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EntityFinder {

    public static String getEntity(String queryStr, String indexPath){
        String  result = "";

        try {
            IndexSearcher searcher =  new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexPath).toPath()))));

            searcher.setSimilarity(new BM25Similarity());

            return getEntity(queryStr,searcher);

        } catch (IOException e) {
            System.err.println("Error! Cannot find the entity" + e.getMessage());
        }

        return result;
    }



    //not so clear with the filed and parser
    public static String getEntity(String queryStr, IndexSearcher searcher) throws IOException {
        String result = "";

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();


        //fuzzyQuery implemented like edit distance
        queryBuilder.add(new FuzzyQuery(new Term("content",queryStr)), BooleanClause.Occur.SHOULD);

        Query q = queryBuilder.build();

        QueryParser parser = new QueryParser("content", new StandardAnalyzer());

        TopDocs tops = searcher.search(q,1);


        ScoreDoc[] scoreDocs = tops.scoreDocs;

        if(scoreDocs.length == 0){
            System.err.println("Cannot get relevent result");
        }


        ScoreDoc score = scoreDocs[0];

        Document doc = searcher.doc(score.doc);

        String name = doc.getField("content").stringValue();
        result = doc.getField("content").stringValue();

        return result;

    }

    public static List<Entity> getRelatedEntity(String content) throws Exception {
        List<Entity> list = new ArrayList<>();
        String jsonStr = SpotLight.getRelatedJson(content);
        Gson gson = new GsonBuilder().create();
        JsonParser jsonParser = new JsonParser();

        JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonStr);

        Type listType = new TypeToken<List<Entity>>(){}.getType();
        list = gson.fromJson(jsonObject.get("Resources"),listType);
        return  list;
    }
}
