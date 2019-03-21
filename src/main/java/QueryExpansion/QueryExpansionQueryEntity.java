package main.java.QueryExpansion;

import main.java.EntityLinking.Entity;
import main.java.EntityLinking.EntityFinder;
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

    public void run(Map<String,String> map,String fileName)  throws Exception{
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));
        searcher.setSimilarity(new BM25Similarity());
        parser = new QueryParser("content", new StandardAnalyzer());

        Set<String> runFileStr = new HashSet<>();
        int count = 0;
        for (Map.Entry<String, String> entry:map.entrySet()){
            String queryStr = entry.getValue();
            System.out.println(count++);
            String queryId = entry.getKey();
            Query q = parser.parse(QueryParser.escape(queryStr));
            TopDocs tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDocArr = tops.scoreDocs;

            if(scoreDocArr.length == 0) continue;

            ScoreDoc score = scoreDocArr[0];
            Document doc= searcher.doc(score.doc);

            String tmp = doc.getField("content").stringValue();

            List<String> expandedQueryList
                    = getExpanedQuery(5,tmp);



            q = weightQuery(queryStr,expandedQueryList);


            tops = searcher.search(q,100);

            ScoreDoc[] scoreDocs = tops.scoreDocs;

            for (int i = 0; i < scoreDocs.length; i++){
                ScoreDoc scoreDoc = scoreDocs[i];
                Document doc1 = searcher.doc(scoreDoc.doc);
                String paraId = doc1.getField("paraid").stringValue();
                float rankScore = scoreDoc.score;
                int rank = i+1;
                String runStr = queryId+" Q0 "+paraId+" "+rank+ " "+rankScore+" "+"team3"+" QueryExpansion";

                runFileStr.add(runStr);
            }

        }

        writeToFile(fileName,runFileStr);

    }

    private void writeToFile(String filename, Set<String> runfileStrings) {
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

    public Query weightQuery(String queryStr, List<String> expandedQueryList) throws ParseException {
        if (!expandedQueryList.isEmpty()){
            String expand_str = String.join(" ",expandedQueryList);
            Query q = parser.parse(QueryParser.escape(queryStr)+"^1.5"+QueryParser.escape(expand_str)+"^0.75");
            return q;
        }else{
            Query q = parser.parse(QueryParser.escape(queryStr));
            return q;
        }
    }

    public List<String> getExpanedQuery(int top, String queryStr)  {
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

        boolean redo = false;


        //for page
        //if (!isSection){
            String EntityAbstr = EntityFinder.getEntity(queryStr.replace(" ","_").toLowerCase(),INDEX_DIR);

            if (!EntityAbstr.isEmpty()){
                //find annotation
                List<Entity> entities = new ArrayList<>();
                try{
                    entities = EntityFinder.getRelatedEntity(EntityAbstr);
                    System.err.println("no Exception" );
                }catch (Exception e){
                    System.err.println("Exception:" + e.getMessage());
                    redo = true;

                    while (redo){
                        try {
                            entities = EntityFinder.getRelatedEntity(EntityAbstr);
                            redo = false;
                        }catch (Exception ex1){
                            System.err.println("Exception:" + ex1.getMessage());
                        }
                    }
                }


                if (entities == null) return expandedQueryList;

                if (entities.size() > 5){
                    for (Entity entity : entities){
                        entityScore.put(entity.getSurfaceForm(),entity.getSimilarityScore());
                    }
                }else if (entities.size() > 0 && entities.size() <= 5){
                    for (Entity entity : entities){
                        entityScore.put(entity.getSurfaceForm(),entity.getSimilarityScore());
                    }

//                    return  expandedQueryList;
                }else{
//                    return expandedQueryList;
                }
            }
        //}
        //for section
//        else{
//            for (int i = 0; i < queryTerms.size();i++){
//                int factor = 1;
//                String queryTerm = queryTerms.get(i);
//                if (i ==0) factor = 3;
//                if (i == queryTerms.size() - 1) factor = 2;
//
//               String EntityAbstr = EntityFinder.getEntity(queryTerm.replace(" ","_").toLowerCase(),INDEX_DIR);
//
//                if (!EntityAbstr.isEmpty()){
//                    List<Entity> entities = new ArrayList<>();
//
//                    try{
//                        entities = EntityFinder.getRelatedEntity(EntityAbstr);
//                    }catch (Exception e){
//                        System.err.println("Exception:" + e.getMessage());
//                        redo = true;
//
//                        while (redo){
//                            try {
//                                entities = EntityFinder.getRelatedEntity(EntityAbstr);
//                                redo = false;
//                            }catch (Exception ex1){
//                                System.err.println("Exception:" + ex1.getMessage());
//                            }
//                        }
//                    }
//
//
//                    if (entities != null){
//                        if (entities.size() > 0){
//                            for (Entity entity : entities){
//                                float score = (float) entity.getSimilarityScore()*factor;
//                                entityScore.put(entity.getSurfaceForm(),score);
//                            }
//                        }else{
//                            System.err.println("Cannot find related entity for term: "+ queryTerm+", skipped");
//                        }
//                    }
//                }
//            }
//
//            if (entityScore.isEmpty()){
//                return expandedQueryList;
//            }
//        }

        Set<String> set = SortMapByScore(entityScore,5);

        expandedQueryList.addAll(set);
        return expandedQueryList;

    }

    public Set<String> SortMapByScore(Map<String,Float> entityScoreMap, int top){
        List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(entityScoreMap.entrySet());


        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {

            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        HashMap<String, Float> sortedMap = new LinkedHashMap<String, Float>();
        int i = 0;
        for (Map.Entry<String, Float> entry : list)

        {
            if (i < top || top == 0) {
                sortedMap.put(entry.getKey(), entry.getValue());
                i++;
            } else {
                break;
            }
        }

        Set<String> res = sortedMap.keySet();

        return res;




    }
}
