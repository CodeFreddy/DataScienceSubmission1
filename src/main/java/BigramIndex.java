package main.java;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.*;

public class BigramIndex {
    // Indexing function for Bigram.
    private static int top_k = 10;

    public static HashMap<String, Float> createBigramIndexFiled(String para_body) {
        try {
            // Get all bigram from paragraph text
            ArrayList<String> bigram_list = analyzeByBigram(para_body);

            // Get all unigram/single term from paragraph text, remove all stop
            // words.
            ArrayList<String> unigram_list = analyzeByUnigram(para_body);

            // Ignore the single word/stop words as paragraph
            if (bigram_list.isEmpty() || unigram_list.isEmpty()) {
                // System.out.println(para_body);
                return new HashMap<String, Float>();
            } else {
                // System.out.println(bigram_list);
            }

            // Generate hashMap with term and term frequency.
            HashMap<String, Integer> bigram_map = countFreq(bigram_list);
            HashMap<String, Integer> unigram_map = countFreq(unigram_list);
            // Calculate all bigram score.
            HashMap<String, Float> results = getFreqBigramScore(bigram_map, bigram_list.size(), unigram_map,
                    unigram_list.size());
            return getTopValuesInMap(results, top_k);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<String, Float>();
        }

    }

    private static HashMap<String, Float> getFreqBigramScore(HashMap<String, Integer> bigram_map,
                                                             int size_of_bigramlist, HashMap<String, Integer> unigram_map, int size_of_unigramlist) {
        HashMap<String, Float> bigram_score = new HashMap<String, Float>();

        for (Map.Entry<String, Integer> entry : bigram_map.entrySet()) {
            float p_bigram = (float) entry.getValue() / size_of_bigramlist;
            String[] keys = entry.getKey().split(" ");
            float score = (float) 0.0;
            try {
                float p_unigram = ((float) unigram_map.get(keys[0]) / size_of_unigramlist)
                        * ((float) unigram_map.get(keys[1]) / size_of_unigramlist);
                score = (float) p_bigram / p_unigram;
            } catch (Exception e) {
                // e.printStackTrace();
                // Found a stop word in bigram term, skip it.
            }
            bigram_score.put(entry.getKey(), score);
        }

        return bigram_score;
    }

    private static HashMap<String, Integer> countFreq(ArrayList<String> str_list) {
        HashMap<String, Integer> str_map = new HashMap<String, Integer>();

        if (str_list.isEmpty()) {
            throw new IllegalStateException("String list cannot be empty!");
        }
        for (String str : str_list) {
            if (str_map.keySet().contains(str)) {
                str_map.put(str, str_map.get(str) + 1);

            } else {
                str_map.put(str, 1);
            }
        }
        return str_map;
    }

    protected static ArrayList<String> analyzeByBigram(String inputStr) throws IOException {
        ArrayList<String> strList = new ArrayList<String>();
        Analyzer analyzer = new BigramAnalyzer();
        TokenStream tokenizer = analyzer.tokenStream("content", inputStr);
        CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
        tokenizer.reset();
        while (tokenizer.incrementToken()) {
            String token = charTermAttribute.toString();
            if (token.contains(" ")) {
                strList.add(token);
            }
            // System.out.println(token);
        }
        tokenizer.end();
        tokenizer.close();
        return strList;
    }

    private static ArrayList<String> analyzeByUnigram(String inputStr) throws IOException {
        ArrayList<String> strList = new ArrayList<String>();
        Analyzer analyzer = new UnigramAnalyzer();
        TokenStream tokenizer = analyzer.tokenStream("content", inputStr);

        CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
        tokenizer.reset();
        while (tokenizer.incrementToken()) {
            String token = charTermAttribute.toString();
            strList.add(token);
            // System.out.println(token);
        }
        tokenizer.end();
        tokenizer.close();
        return strList;
    }

    public static HashMap<String, Float> getTopValuesInMap(Map<String, Float> unsortMap, int k) {
        List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {

            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        HashMap<String, Float> sortedMap = new LinkedHashMap<String, Float>();
        int i = 0;
        for (Map.Entry<String, Float> entry : list)

        {
            if (i < k || k == 0) {
                sortedMap.put(entry.getKey(), entry.getValue());
                i++;
            } else {
                break;
            }
        }

        return sortedMap;
    }
}
