package main.java.QueryExpansion;

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
            run(pageMap,"page-QueryExpansion-Doc-Entity.run");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void runSection() throws IOException,ParseException{
        try {
            run(sectionMap,"section-QueryExpansion-Doc-Entity.run");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(Map<String,String> map,String fileName) throws Exception{
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));

        searcher.setSimilarity(new BM25Similarity());
        Set<String> runFileStr = new HashSet<>();
        parser = new QueryParser("content", new StandardAnalyzer());
        for (Map.Entry<String, String> entry:map.entrySet()){
            String queryStr = entry.getValue();
            String queryId = entry.getKey();

            Query q0 = parser.parse(QueryParser.escape(queryStr));
            TopDocs initial_top = searcher.search(q0,10);
            ScoreDoc[] initial_scoreDocs = initial_top.scoreDocs;



            //get top k entity from index document

            List<String> expandedEntity = getExpandedTerm(5,searcher,initial_scoreDocs);

            Query q_rm = setBoost(queryStr,expandedEntity);

            TopDocs tops = searcher.search(q_rm, max_result);
            ScoreDoc[] newScoreDoc = tops.scoreDocs;

            for (int i = 0; i < newScoreDoc.length;i++ ){
                ScoreDoc score = newScoreDoc[i];
                Document doc = searcher.doc(score.doc);

                String paraId = doc.getField("paraid").stringValue();
                float rankScore = score.score;
                int rank = i+1;
                String runStr = queryId+" Q0 "+paraId+" "+rank+ " "+rankScore+" "+"team3"+" QueryExpansion-Doc-Entity";

                if (!runFileStr.contains(runStr)){
                    runFileStr.add(runStr);
                }
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

    private Query setBoost(String originalQuery, List<String> expanded_list) throws ParseException {
        if (!expanded_list.isEmpty()) {
            String rm_str = String.join(" ", expanded_list);
            Query q = parser.parse(QueryParser.escape(originalQuery) + "^1.5" + QueryParser.escape(rm_str) + "^0.75");
            return q;
        } else {
            Query q = parser.parse(QueryParser.escape(originalQuery));
            return q;
        }
    }

    public List<String> getExpandedTerm(int top_k, IndexSearcher searcher, ScoreDoc[] scoreDocs) throws IOException {
        List<String> releventList = new ArrayList<>();

        Map<String,Float> entityMap = new HashMap<>();

        for (int i = 0; i < scoreDocs.length;i++){
            ScoreDoc score = scoreDocs[i];
            Document doc = searcher.doc(score.doc);
            String paraID = doc.getField("paraid").stringValue();
            List<String> entityList = Arrays.asList(doc.getValues("spotlight"));

            if (entityList.isEmpty()){
                System.out.println("no available entity ");
            }else{
                int rank = i+1;
                entityList = removeDup(entityList);
                float p = (float) 1 / (rank + 1);
                for (String entity : entityList){
                    //tf
                    int tf_w = getFreq(entity, entityList);
                    //wrong length, document length
                    //int tf_list = entityList.size()
                    int tf_list = scoreDocs.length;
                    float entity_score = p * ((float) tf_w / tf_list);
                    if (entityMap.keySet().contains(entity.replace("_"," "))) {
                        //term_map.put(termStr, term_map.get(termStr) + term_score);
                        continue;

                    } else {
                        entityMap.put(entity.replace("_"," "), entity_score);
                    }
                }
            }

        }

        Set<String> entitySet = getTop(entityMap, 5);
        releventList.addAll(entitySet);

        return releventList;
    }

    public static Set<String> getTop(Map<String, Float> unsortMap, int k) {
        List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {

            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<String, Float> Map = new LinkedHashMap<>();

        int i = 0;
        for (Map.Entry<String, Float> entry : list)

        {
            if (i < k || k == 0) {
                Map.put(entry.getKey(), entry.getValue());
                i++;
            } else {
                break;
            }
        }

        return Map.keySet();
    }

    private int getFreq(String term, List<String> list) {
        int frequency = Collections.frequency(list, term);
        return frequency;
    }

    public List<String> removeDup(List<String> entityList){
        List<String> tmp = new ArrayList<>();
        Set<String> set = new HashSet<>();
        set.addAll(entityList);
        tmp.addAll(set);

        return tmp;
    }
}
