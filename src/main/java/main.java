package main.java;

import edu.unh.cs.treccar_v2.Data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Main {

    static private String INDEX_DIRECTORY = "/Users/xin/Documents/19Spring/DS/index";
    private static String OUTPUT_DIR = "output/";
    static final private int Max_Results = 100;

    static IndexData indexer;

    public static void main(String[] args) throws IOException {
        System.setProperty("file.encoding", "UTF-8");

        String queryPath = "C:\\CS953\\Assignment1\\queries\\benchmarkY1-train.v2.0.tar\\benchmarkY1\\benchmarkY1-train\\train.pages.cbor-outlines.cbor";

        String dataPath = "/Users/xin/Documents/19Spring/DS/paragraphCorpus/dedup.articles-paragraphs.cbor";




        //INDEX_DIRECTORY = args[0];
        //queryPath = args[1];
        //dataPath = args[2];


       // indexer = new IndexData(INDEX_DIRECTORY, dataPath);
        QueryData queryData = new QueryData(queryPath);
//
        Map<String,String> pageMap = queryData.getAllPageQueries();
        Map<String,String> sectionMap = queryData.getAllSectionQueries();
        ArrayList<Data.Page> pageList = queryData.getPageList();
        ArrayList<Data.Section> sectionList = queryData.getSectionList();

        // Store all query strings temporarily.


        System.out.println("Got " + pageMap.size() + " pages and " + sectionMap.size() + " sections.");

        // Lucene Search


        SearchData searcher = new SearchData(INDEX_DIRECTORY, pageMap, sectionMap, Max_Results);

        UL page_ul = new UL(pageList, Max_Results, INDEX_DIRECTORY);
        writeFile("UnigramLanguageModel-Laplce-Smoothing.run", page_ul.getList());

        UJM page_ujm = new UJM(pageList, Max_Results, INDEX_DIRECTORY);
        writeFile("UnigramLanguageModel-Jelinek-Mercer-Smoothing.run", page_ujm.getList());

        UDS page_uds = new UDS(pageList, Max_Results, INDEX_DIRECTORY, OUTPUT_DIR);


        System.out.println("Finished");
    }

    public static void writeFile(String name, List<String> content){
        String fullpath = OUTPUT_DIR + name;
        System.out.println(fullpath);
        try (FileWriter runfile = new FileWriter(new File(fullpath))) {
            for (String line : content) {
                runfile.write(line + "\n");
            }

            runfile.close();
        } catch (IOException e) {
            System.out.println("Could not open " + fullpath);
        }
    }
}
