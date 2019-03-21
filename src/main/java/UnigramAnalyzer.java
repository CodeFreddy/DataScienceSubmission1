package main.java;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import main.java.QueryExpansion.QueryExpansion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class UnigramAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String filedName) {
        Tokenizer source = new StandardTokenizer();
        QueryExpansion q = new QueryExpansion();
        CharArraySet stopWords = q.getStopWordSet();

        TokenStream filter = new LowerCaseFilter(source);
        TokenStream filter2 = new StopFilter(filter, stopWords);
        return new TokenStreamComponents(source, filter2);
    }


}
