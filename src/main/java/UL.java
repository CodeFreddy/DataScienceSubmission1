package main.java;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.*;

public class UL {
    public List<String> runFileContent;
    public int resultsNum;
    public Map<String, HashMap<String, Float>> results;
    public QueryParser queryParser;
    public IndexReader indexReader;
    public IndexSearcher indexSearcher;

    PriorityQueue<DocResults> docQueue = new PriorityQueue<>((a, b) -> (a.score < b.score ? 1 : a .score > b.score ?  -1 : 0));

    public  List<String> getList(){
        return runFileContent;
    }

    public static int getWordsSize(List<String> list){
        Set<String> set = new HashSet<>();
        for (String s : list){
            set.add(s);
        }

        return set.size();
    }

    public static List<String> unigramAnalyze(String docBoday) throws IOException {
        List<String> res = new ArrayList<>();

        Analyzer analyzer = new UnigramAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("content",docBoday);

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        while (tokenStream.incrementToken()){
            String str = charTermAttribute.toString();
            res.add(str);
        }

        tokenStream.end();
        tokenStream.close();
        return  res;
    }
}
