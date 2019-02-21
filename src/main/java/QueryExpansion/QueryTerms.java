package main.java.QueryExpansion;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class QueryTerms {
    private String [] terms = new String[0];
    private int [] termFreqs = new int[0];
    private List<String> termList = new ArrayList<>();
    private int id;
    public String getField() { return null;  }

    /**
     *
     * @param queryTerms The original list of terms from the query, can contain duplicates
     */
    public QueryTerms(String [] queryTerms) {

        processTerms(queryTerms);
    }



    public QueryTerms(String queryString, Analyzer analyzer,int id) throws IOException {
        this.id = id;
        if (analyzer != null)
        {
            TokenStream stream = analyzer.tokenStream("", new StringReader(queryString));
            if (stream != null)
            {
                List<String> terms = new ArrayList<String>();

                processTerms(queryString.split(" "));

                tokenTerms(queryString);
            }
        }
    }

    private void tokenTerms(String queryString) throws IOException{
        Analyzer analyzer = new UnigramAnalyzer();

        Reader reader = new StringReader(queryString);
        TokenStream tokenStream = analyzer.tokenStream("content",queryString);

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        while (tokenStream.incrementToken()){
            String token = charTermAttribute.toString();
            termList.add(token);

        }

        tokenStream.end();
        tokenStream.close();
    }

    public Map<String,Float> getScoreRank(){

        List<String> res = new ArrayList<>();
        Map<String, Float> map = new HashMap<>();
        Map<Float,String> orderedMap = new TreeMap<>();
        int rank = id;

        float p = (float) 1 / (rank+1);

        this.termList = merge(termList);

        for (String term : termList){

            int tf = Collections.frequency(termList,term);
            int total= termList.size();

            float score = p*((float) tf / total);
            if (map.containsKey(term)){
                map.put(term,map.get(term)+score);

            }else{
                map.put(term,score);
            }

        }

        for (Map.Entry<String,Float> entry : map.entrySet()){
            String key = entry.getKey();
            Float value = entry.getValue();

            orderedMap.put(value,key);
        }

        Map<String, Float> finalMap = new HashMap<>();

        int count = 5;
        //score order decen
        for (Map.Entry<Float,String> entry: orderedMap.entrySet()){
            String str  = entry.getValue();

            if (count > 0){
                finalMap.put(entry.getValue(),entry.getKey());
                count--;
            }else{
                break;
            }

        }





        return  finalMap;


    }

    private List<String> merge(List<String> terms){
        List<String> tmpList = new ArrayList<>();

        Set<String> tmpSet= new HashSet<>();

        tmpSet.addAll(terms);
        tmpList.addAll(tmpSet);

        return tmpList;
    }
    private void processTerms(String[] queryTerms) {
        if (queryTerms != null) {
            Arrays.sort(queryTerms);
            Map<String,Integer> tmpSet = new HashMap<String,Integer>(queryTerms.length);
            //filter out duplicates
            List<String> tmpList = new ArrayList<String>(queryTerms.length);
            List<Integer> tmpFreqs = new ArrayList<Integer>(queryTerms.length);
            int j = 0;
            for (int i = 0; i < queryTerms.length; i++) {
                String term = queryTerms[i];
                Integer position = tmpSet.get(term);
                if (position == null) {
                    tmpSet.put(term, Integer.valueOf(j++));
                    tmpList.add(term);
                    tmpFreqs.add(Integer.valueOf(1));
                }
                else {
                    Integer integer = tmpFreqs.get(position.intValue());
                    tmpFreqs.set(position.intValue(), Integer.valueOf(integer.intValue() + 1));
                }
            }
            terms = tmpList.toArray(terms);
            //termFreqs = (int[])tmpFreqs.toArray(termFreqs);
            termFreqs = new int[tmpFreqs.size()];
            int i = 0;
            for (final Integer integer : tmpFreqs) {
                termFreqs[i++] = integer.intValue();
            }
        }
    }

    public int[] getTermFreqs(){
        return termFreqs;
    }

    public String[] getTerms() {
        return terms;
    }

    public int size(){
        return terms.length;
    }

    public int indexOf(String term){
        int res = Arrays.binarySearch(terms,term);

        return res >= 0 ? res : -1;
    }

    public int[] indexesOf(String[] terms, int start, int len) {
        int res[] = new int[len];

        for (int i=0; i < len; i++) {
            res[i] = indexOf(terms[i]);
        }
        return res;
    }


    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i=0; i<terms.length; i++) {
            if (i>0) sb.append(", ");
            sb.append(terms[i]).append('/').append(termFreqs[i]);
        }
        sb.append('}');
        return sb.toString();
    }




}
