package main.java.QueryExpansion;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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

import java.io.*;
import java.util.*;

public class QueryExpansionLTR {
    public interface ResultProcessor {
        void process(List<String> textList, String text);
    }

    class RankInfoComparator implements Comparator<RankInfo> {

        @Override
        public int compare(RankInfo d1, RankInfo d2) {
            if (d1.getScore() < d2.getScore())
                return 1;
            if (d1.getScore() > d2.getScore())
                return -1;
            return 0;
        }
    }

    private IndexSearcher searcher;
    private Map<String,String> pageMap;
    private Map<String,String> sectionMap;
    private String INDEX_DIR;
    private int max_result;
    private QueryParser parser;
    static  private String OUTPUT_DIR="";
    private Map<String,Map<String,String>> relevenceData = null;
    private String relevenceArtical = "/home/tianxiu/trec_eval/trec_eval/trec_eval/test.pages.cbor-article.qrels";
    private String releveanceHera = "/home/tianxiu/trec_eval/trec_eval/trec_eval/test.pages.cbor-hierarchical.qrels";
    private String pageRunFile = "page-QueryExpansionLTR.run";
    private String sectionRunFile = "section-QueryExpansionLTR.run";
    public QueryExpansionLTR(Map<String, String> pageMap,Map<String,String> sectionMap, String indexPath,String output){
        this.pageMap = pageMap;
        this.INDEX_DIR = indexPath;
        this.max_result = 100;
        this.sectionMap = sectionMap;
        OUTPUT_DIR = output;
        this.relevenceData = new HashMap<>();
    }

    public Map<String,Map<String,String>> readData(String file_name){
        Map<String,Map<String,String>> query = new HashMap<>();

        File f = new File(file_name);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String text = null;
            while ((text = br.readLine()) != null){

                String queryId = text.split(" ")[0];
                String paraID = text.split(" ")[2];
                String rank = text.split(" ")[3];
                if (query.containsKey(queryId))
                    query.get(queryId).put(paraID, rank);
                else {
                    Map<String, String> docs = new HashMap<String, String>();
                    docs.put(paraID, rank);
                    query.put(queryId, docs);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (br != null)
                br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return query;

    }

    public void runPage() throws IOException, ParseException {
        run(pageMap,relevenceArtical,pageRunFile);
    }

    public void runSection() throws IOException, ParseException {
        run(sectionMap,releveanceHera,sectionRunFile);
    }

    private  RankInfo getRankInfoById(String id, ArrayList<RankInfo> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (RankInfo rank : list) {
            if (rank.getParaId().equalsIgnoreCase(id)) {
                return rank;
            }
        }
        return null;
    }

    public List<String> generateFeature(Map<String,ArrayList<RankInfo>> feature1,
                                Map<String,ArrayList<RankInfo>> feature2,
                                        Map<String,String> map){

        List<String> writeToFileList = new ArrayList<>();

        for (Map.Entry<String, String> entry:map.entrySet()){
            String queryId = entry.getKey();

            ArrayList<RankInfo> rankInfoList1 = feature1.get(queryId);
            ArrayList<RankInfo> rankInfoList2 = feature2.get(queryId);

            if (!relevenceData.containsKey(queryId))
                continue;
            Map<String,String> revelanceMap = relevenceData.get(queryId);

//            ArrayList<String> totalDocs = getAllDocId(rankInfoList1,rankInfoList2);
            Map<String, RankInfo> totalDocsMap = getAllDocId(rankInfoList1,rankInfoList2);

            //System.out.println("Total :" + totalDocsMap.size() + " docs for Query: " + queryId);


            for (Map.Entry<String,RankInfo> newEnty: totalDocsMap.entrySet()){
                String id = newEnty.getKey();
                String rank = Integer.toString(newEnty.getValue().getRank());
                String rankScore = Float.toString(newEnty.getValue().getScore());


                RankInfo r1 = getRankInfoById(id,rankInfoList1);
                RankInfo r2 = getRankInfoById(id,rankInfoList2);

                float f1 = (float) ((r1 == null) ? 0.0 : (float) 1 / r1.getRank());
                float f2 = (float) ((r2 == null) ? 0.0 : (float) 1 / r2.getRank());

                int relevant = 0;

                if (revelanceMap.get(id) != null) {
                    if (Integer.parseInt(revelanceMap.get(id)) > 0) {
                        relevant = 1;
                    }
                }

                String line = relevant + " qid:" + queryId + " 1:" + f1 + " 2:" + f2 +" # DocId:" + id+" "+rank+" "+rankScore;
//                System.out.println(line);
                writeToFileList.add(line);

            }

        }

        return writeToFileList;

    }

    public void writeDataFile(String filename, List<String> datafileString) {
        String fullpath = OUTPUT_DIR + "/" + filename;
        try (FileWriter runfile = new FileWriter(new File(fullpath))) {
            for (String line : datafileString) {
                runfile.write(line + "\n");
            }

            runfile.close();
        } catch (IOException e) {
            System.out.println("Could not open " + fullpath);
        }
    }

    public Map<String,RankInfo> getAllDocId(ArrayList<RankInfo> list1,ArrayList<RankInfo> list2){

        ArrayList<RankInfo> total_rankInfo = new ArrayList<RankInfo>();
        Map<String,RankInfo> map = new HashMap<>();

        if (list2 != null || list1.size() != 0)
            total_rankInfo.addAll(list1);
        if (list2 != null || list2.size() != 0)
            total_rankInfo.addAll(list2);

        for (RankInfo rankInfo: list1){
            String paraid = rankInfo.getParaId();

            if (!map.containsKey(paraid)){
                map.put(paraid,rankInfo);
            }else{
                RankInfo r2 = map.get(paraid);

                if (rankInfo.getScore() > r2.getScore() ){
                    map.put(paraid,rankInfo);
                }
            }
        }

        for (RankInfo rankInfo: list2){
            String paraid = rankInfo.getParaId();

            if (!map.containsKey(paraid)){
                map.put(paraid,rankInfo);
            }else{
                RankInfo r2 = map.get(paraid);

                if (rankInfo.getScore() > r2.getScore() ){
                    map.put(paraid,rankInfo);
                }
            }
        }



//        ArrayList<String> total_documents = new ArrayList<String>();
//        for (RankInfo rank : total_rankInfo) {
//            total_documents.add(rank.getParaId());
//        }
//        Set<String> hs = new HashSet<>();
//        hs.addAll(total_documents);
//        total_documents.clear();
//        total_documents.addAll(hs);
        return map;
    }

    public Map<String,ArrayList<RankInfo>>  featureBM25(Map<String,String> map) throws ParseException, IOException {
        Map<String,ArrayList<RankInfo>> result = new HashMap<>();
        int count=1;
        for (Map.Entry<String, String> entry:map.entrySet()){
            System.out.println(count+" / "+map.size());
            count++;
            String queryStr = entry.getValue();
            String queryId = entry.getKey();
            Query q = parser.parse(QueryParser.escape(queryStr));
            TopDocs tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDoc = tops.scoreDocs;
            ArrayList<RankInfo> rankInfoList = new ArrayList<>();
            for (int i = 0; i < scoreDoc.length;i++ ){
                ScoreDoc score = scoreDoc[i];
                Document doc = searcher.doc(score.doc);
                String docId = doc.getField("paraid").stringValue();
                float rankScore = scoreDoc[i].score;

                RankInfo rank = new RankInfo();
                rank.setQueryStr(queryId);
                rank.setParaId(docId);
                rank.setRank(i + 1);
                rank.setScore(rankScore);
                rank.setTeam_method_name("team3-QueryExpansion-LTR");
                rank.setParaContent(doc.get("content"));
                rankInfoList.add(rank);
            }

            result.put(queryId,rankInfoList);

        }
        return result;
    }


    public Map<String,ArrayList<RankInfo>> featureQueryExpansion(Map<String,String> map) throws ParseException, IOException {
        Map<String,ArrayList<RankInfo>> result = new HashMap<>();
        int count = 1;
        for (Map.Entry<String, String> entry:map.entrySet()){
            System.out.println(count+" / "+map.size());
            count++;
            String queryStr = entry.getValue();
            String queryId = entry.getKey();

            Query q = parser.parse(QueryParser.escape(queryStr));
            TopDocs tops = searcher.search(q, max_result);
            ScoreDoc[] scoreDoc = tops.scoreDocs;


            List<String> expandQueryList = expandQueryByRocchio(5,scoreDoc);


            Query q_rm = setBoost(queryStr,expandQueryList);

            tops = searcher.search(q_rm, max_result);
            ScoreDoc[] newScoreDoc = tops.scoreDocs;
            ArrayList<RankInfo> rankInfoList = new ArrayList<>();
            for (int i = 0; i < newScoreDoc.length;i++ ){
                ScoreDoc score = newScoreDoc[i];
                Document doc = searcher.doc(score.doc);
                String docId = doc.getField("paraid").stringValue();
                float rankScore = newScoreDoc[i].score;

                RankInfo rank = new RankInfo();
                rank.setQueryStr(queryId);
                rank.setParaId(docId);
                rank.setRank(i + 1);
                rank.setScore(rankScore);
                rank.setTeam_method_name("team3-QueryExpansion-LTR");
                rank.setParaContent(doc.get("content"));
                rankInfoList.add(rank);
            }

            result.put(queryId,rankInfoList);

        }
        return result;
    }

    public void run(Map<String,String> map,String filename,String outputFile) throws IOException, ParseException {
        relevenceData = readData(filename);


        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIR).toPath()))));

        searcher.setSimilarity(new BM25Similarity());

        parser = new QueryParser("content", new StandardAnalyzer());

        Map<String,ArrayList<RankInfo>> bm25Result = featureBM25(map);
        Map<String,ArrayList<RankInfo>> qeResult = featureQueryExpansion(map);

        List<String> writeToFileList = generateFeature(bm25Result,qeResult,map);

        writeDataFile("feature.txt",writeToFileList);

        //run Ranklib from java
        String trainFile = OUTPUT_DIR+"/feature.txt";

        String cmd1 = "java -jar /home/xl1044/RankLib.jar -train ";
        cmd1 += trainFile;
        cmd1 += " -ranker 3 -metric2t MAP -save ";
        String modeDir = OUTPUT_DIR+"/model.txt";
        cmd1 += modeDir;

        runCmd(cmd1);

        String cmd2 = "java -jar /home/xl1044/RankLib.jar -rank ";
        cmd2 += trainFile;
        cmd2 += " -load ";
        cmd2 += modeDir;
        cmd2 += " -score ";
        String scorePath = OUTPUT_DIR+"/score.txt";

        cmd2+= scorePath;

        runCmd(cmd2);

        //now we havd score.txt and feature.txt if everything runds good

        Map<String,List<RankInfo>> queryIdMap = generateRunFile(trainFile,scorePath);

        List<String> runStr = rankResult(queryIdMap);

        writeDataFile(outputFile,runStr);
    }
    public  List<String> rankResult(Map<String,List<RankInfo>> map){
        List<String> runFileStr = new ArrayList<String>();
        for (Map.Entry<String,List<RankInfo>> entry:map.entrySet()){
            String queryID = entry.getKey();
            List<RankInfo> rankInfoList = entry.getValue();
            PriorityQueue<RankInfo> priorityQueue = new PriorityQueue<>(new RankInfoComparator());
            for (RankInfo r: rankInfoList){
                priorityQueue.add(r);
            }

            while (!priorityQueue.isEmpty()){
                RankInfo r = priorityQueue.poll();
                String rank = Integer.toString(r.getRank());
                String score = Float.toString(r.getScore());
                String runStr = queryID + " Q0 "+r.getParaId()+" "+rank+" "+score+" team3 QueryExpansionLTR";
                if (!runFileStr.contains(runStr)){
                    runFileStr.add(runStr);
                }
            }
        }

        return runFileStr;
    }

    public static Map<String,List<RankInfo>> generateRunFile(String modeldir,String scoreDir){
//        System.out.println("mode: " +modeldir +"  score dir:"+scoreDir );
        Map<String,List<RankInfo>> map = new HashMap<>();
        File f = new File(modeldir);
        BufferedReader br = null;
        BufferedReader br2 = null;
        File f2 = new File(scoreDir);
        try {
            br = new BufferedReader(new FileReader(f));
            br2 = new BufferedReader(new FileReader(f2));
            String text = null;
            String text2 = null;
            while ((text = br.readLine()) != null && (text2 = br2.readLine()) != null){
                //feature read
                String[] strArr = text.split(" ");
//                System.out.println("feature: "+text);
//                System.out.println("score: "+text2);
                String queryId = strArr[1].substring(4);
                String paraId = strArr[5].substring(6);
                String rank = strArr[6];
                String rankScore = strArr[7];

                //score read
//                System.out.print(text2 + " => ");

                String[] strArrScore = text2.split("\\s+");
//
                String score = strArrScore[2];
//                System.out.println(score);
//                System.out.println(strArr.length);



                RankInfo rankInfo = new RankInfo();
                rankInfo.setRank(Integer.parseInt(rank));
                rankInfo.setScore(Float.parseFloat(rankScore));
                rankInfo.setParaId(paraId);
                rankInfo.setQueryStr(queryId);
                if (map.containsKey(queryId)){
                    map.get(queryId).add(rankInfo);
                }else{
                    List<RankInfo> list = new ArrayList<>();
                    list.add(rankInfo);
                    map.put(queryId,list);
                }
            }

            br.close();
            br2.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public  List<String> runCmd(String cmd){
        return runCommand(cmd, new ResultProcessor() {
            @Override
            public void process(List<String> textList, String text) {
                textList.add(text);
            }
        });
    }

    private List<String> runCommand(String cmd, ResultProcessor processor) {
        List<String> resultList = new ArrayList<String>();
        String[] cmds = {"/bin/sh", "-c", cmd};
        Process pro = null;
        try {
            pro = Runtime.getRuntime().exec(cmds);
            pro.waitFor();
            InputStream in = pro.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = read.readLine()) != null) {
                processor.process(resultList, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resultList;
    }

    public List<String> expandQueryByRocchio(int top, ScoreDoc[] scoreDocs) throws IOException {
        List<String> expandedList = new ArrayList<>();
        Map<String,Float> term_map = new HashMap<>();


        for (int i = 0; i < scoreDocs.length;i++){
            ScoreDoc score = scoreDocs[i];
            Document doc = searcher.doc(score.doc);
            String paraBody = doc.getField("content").toString();

            //document term vector
            List<String> unigram_list = analyze(paraBody);

            int rank = i+1;

            float p = (float) 1 / (rank + 1);


            for (String termStr : unigram_list){
                //tf
                int tf_w = getFreq(termStr, unigram_list);
                //wrong length, document length
                int tf_list = scoreDocs.length;
                float term_score = p * ((float) tf_w / tf_list);
                if (term_map.keySet().contains(termStr)) {
                    //term_map.put(termStr, term_map.get(termStr) + term_score);
                    continue;

                } else {
                    term_map.put(termStr, term_score);
                }
            }



        }
        Set<String> termSet = getTop(term_map, 5);

        expandedList.addAll(termSet);

        return expandedList;
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

    private  List<String> analyze(String inputStr) throws IOException{
        List<String> strList = new ArrayList<>();
        //double check with the token
        Analyzer test = new EnglishAnalyzer(getStopWordSet());

        TokenStream tokenizer = test.tokenStream("content", inputStr);

        CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
        tokenizer.reset();
        while (tokenizer.incrementToken()) {
            String token = charTermAttribute.toString();
            strList.add(token);
        }
        tokenizer.end();
        tokenizer.close();

        return strList;
    }

    private int getFreq(String term, List<String> list) {
        int frequency = Collections.frequency(list, term);
        return frequency;
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

    public CharArraySet getStopWordSet(){
        //String stopWordDir = "/home/xl1044/ds/Query_Expansion/QueryExpaison/File/stop_word.cfg";
        String stopWordDir = "src/resources/stop_word.cfg";

        List<String> list = new ArrayList<>();

        String line = "";

        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordDir));

            while ((line = bufferedReader.readLine()) != null){
                if (!line.isEmpty()){
                    list.add(line.replace(" ",""));
                }
            }

            bufferedReader.close();

            CharArraySet stopWord = new CharArraySet(list,true);

            return  stopWord;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


}
