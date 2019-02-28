package main.java;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class BigramAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        ShingleFilter sf = new ShingleFilter(source, 2, 2);
        sf.setTokenSeparator(" ");
        TokenStream filter = new LowerCaseFilter(sf);
        return new TokenStreamComponents(source, filter);
    }

}