package main.java.QueryExpansion;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class UnigramAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName){
        Tokenizer source = new StandardTokenizer();

        CharArraySet stopWords = getCustomStopWordSet();

        TokenStream filter = new LowerCaseFilter(source);
        TokenStream filter2 = new StopFilter(filter, stopWords);
        return new TokenStreamComponents(source, filter2);
    }

    public CharArraySet getCustomStopWordSet(){
        String stopWordDir = "stop_word.cfg";

        List<String> list = new ArrayList<>();

        String line = "";

        try{
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(stopWordDir);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));


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
