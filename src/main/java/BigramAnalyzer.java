package main.java;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class BigramAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();


        ShingleFilter sf = new ShingleFilter(source, 2, 2);
        sf.setTokenSeparator(" ");
        TokenStream filter1 = new LowerCaseFilter(sf);
        TokenStream filter2 = new StopFilter(filter1, stopWords);

        return new TokenStreamComponents(source, filter2);
    }

}