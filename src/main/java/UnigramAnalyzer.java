package main.java;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class UnigramAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String filedName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream filter = new LowerCaseFilter(source);
        return new TokenStreamComponents(source, filter);
    }
}
