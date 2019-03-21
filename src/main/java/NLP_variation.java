package main.java;

import edu.stanford.nlp.ie.util.RelationTriple;
import main.java.EntityLinking.Entity;
import main.java.EntityLinking.EntityFinder;
import main.java.NLP.NLP_Document;
import main.java.NLP.NLP_Processor;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.parser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class NLP_variation {
    // Run with BM25, extracted the relations from content.
    // Rank the document that entities has relations with entities in query
    // higher.
    // Re rank the file.

    private static int max_result = 20; // Max number for Lucene docs
    private static QueryParser parser = new QueryParser("content", new EnglishAnalyzer());

    private static NLP_Processor NLPpipeline = NLP_Processor.getInstance();
    private static ExecutorService workers;
    private static Integer thread_timeout = 30;
    private static Integer pool_size = 20;
    private static float obj_factor = (float) 0.6;
    private static float subj_factor = (float) 0.4;

    public static ArrayList<String> getResults(Map<String, String> queriesStr, String index_dir)
            throws Exception {

        System.out.println("NLP query variation ====> Retrieving results for " + queriesStr.size() + " queries...");
        ArrayList<String> runFileStr = new ArrayList<String>();

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(index_dir).toPath()))));
        searcher.setSimilarity(new BM25Similarity());

        int duplicate = 0;
        int queryCount = 1;
        for (Map.Entry<String,String> entry : queriesStr.entrySet()) {
            System.out.println("Processing query " + queryCount + " / " + queriesStr.size() + " ...");
            String queryStr = entry.getValue();
            String queryId = entry.getKey();
            queryCount++;
            Query q = parser.parse(QueryParser.escape(queryStr));
            HashMap<Document, Float> scoredDocs = new HashMap<>();
            TopDocs init_tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDoc = init_tops.scoreDocs;
            List<String> redoList = new ArrayList<>();
            for (int i = 0; i < scoreDoc.length; i++) {
                ScoreDoc score = scoreDoc[i];
                Document doc = searcher.doc(score.doc);
                String paraId = doc.getField("paraid").stringValue();
                String paraContent = doc.getField("content").stringValue();
                float init_score = score.score;
                // List<String> entities =
                // Arrays.asList(doc.getValues("spotlight"));

                NLP_Document.Paragraph paraObj = NLPpipeline.convertToNL_DocumentWithOpenIE(paraContent);
                List<Entity> spotLight_entities = null;

                try {
                    spotLight_entities = EntityFinder.getRelatedEntity(queryStr);
                }catch (Exception e)
                {
                    System.err.println("Can't get json " + e.getMessage());
                    redoList.add(queryStr);
                }
                if(redoList.size() != 0)
                {
                    System.out.println("Start to redo...");
                    int times = 5;
                    while(times >= 0)
                    {
                        times--;
                        System.out.println("redolist size: " + redoList.size());
                        try {
                            spotLight_entities = EntityFinder.getRelatedEntity(redoList.get(0));
                            //redoList.remove(0);
                        }catch (Exception e)
                        {
                            System.err.println("Can't get json: "+ e.getMessage());
                        }

                    }
                    redoList.remove(0);
                }



                List<String> entities = new ArrayList<String>();
                // Reduce the boots factor, when spotlight can't find any
                // entities.
                // Else, keep the origin boots.
                float reduce_factor = (float) 1;
                if (spotLight_entities == null || spotLight_entities.isEmpty()) {
                    reduce_factor = (float) 0.5;
                    // // Check if it's section path query
                    if (queryStr.contains("/")) {

                        entities = Arrays.asList(queryStr.split("/"));
                    } else {
                        entities.add(queryStr);
                    }
                } else {
                    for (Entity e : spotLight_entities) {
                        entities.add(e.getSurfaceForm().toLowerCase());
                    }
                }



                // Caculate score
                float final_score = getRerankedScore(init_score, reduce_factor, entities, paraObj);
                scoredDocs.put(doc, final_score);
            }

            // Re-rank the documents with new scores.
            int rank = 1;
            HashMap<Document, Float> rankedDocs = getSortedDocs(scoredDocs);
            for (Map.Entry<Document, Float> entry1 : rankedDocs.entrySet()) {
                Document doc = entry1.getKey();
                String paraId = doc.getField("paraid").stringValue();

                String runStr = queryId + " Q0 " + paraId + " " + rank + " " + entry1.getValue() + " NLP_query_variation";
                if (runFileStr.contains(runStr)) {
                    duplicate++;
                    // logger.debug("Found duplicate: " + runStr);
                } else {
                    runFileStr.add(runStr);
                }

                rank++;
            }

        }

        System.out.println("NLP query variation ====> Got " + runFileStr.size() + " results. Found " + duplicate + " duplicates.");

        return runFileStr;

    }

    private static float getRerankedScore(Float init_score, Float reduce_factor, List<String> entities, NLP_Document.Paragraph paraObj) {

        // Get a list of relations in this paragraph. <Entity, Relation, Entity>
        ArrayList<RelationTriple> relations = paraObj.getAllRelationsTriple();
        float obj_score = (float) 0.0;
        float subj_score = (float) 0.0;
        for (String entity : entities) {
            List<Integer> counts = getParticipatedNumber(entity, relations);

            int obj_c = counts.get(0);
            int subj_c = counts.get(1);

            obj_score = +(float) obj_c * obj_factor * reduce_factor;
            subj_score = +(float) subj_c * subj_factor * reduce_factor;
        }

        float final_score = init_score + obj_score + subj_score;

        return final_score;
    }


    // Count the number of how many times that this entity participate in
    // relations as an object and as a subject
    // Output as List[NumOfObject, NumOfSubject]
    // Object counts = List[0]
    // Subject counts = List[1]
    private static List<Integer> getParticipatedNumber(String entity, ArrayList<RelationTriple> relations) {
        int objCount = 0;
        int subCount = 0;

        if (!relations.isEmpty()) {
            entity = entity.toLowerCase();
            for (RelationTriple rel : relations) {
                String object = rel.objectGloss().toLowerCase();
                String subject = rel.subjectGloss().toLowerCase();

                if (object.contains(entity) || entity.contains(object)) {
                    objCount++;
                }
                if (subject.contains(entity) || entity.contains(subject)) {
                    subCount++;
                }
            }
        }

        List<Integer> counts = new ArrayList<Integer>(2);
        counts.add(objCount);
        counts.add(subCount);

        return counts;
    }

    // Sort in DESC order
    private static HashMap<Document, Float> getSortedDocs(HashMap<Document, Float> unsortedMap) {

        List<Map.Entry<Document, Float>> list = new LinkedList<Map.Entry<Document, Float>>(unsortedMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Document, Float>>() {

            public int compare(Map.Entry<Document, Float> o1, Map.Entry<Document, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        HashMap<Document, Float> sortedMap = new LinkedHashMap<Document, Float>();
        int i = 0;
        for (Map.Entry<Document, Float> entry : list)

        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
