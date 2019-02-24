package main.java;

import edu.unh.cs.treccar_v2.Data;
import main.java.QueryExpansion.QueryExpansion;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Main {

    static private String INDEX_DIRECTORY = "C:\\CS953\\DataScienceSubmission1\\index";
    static private String OUTPUT_DIR = "C:\\CS953\\DataScienceSubmission1\\output";
    static final private int Max_Results = 100;

    static IndexData indexer;

    public static void main(String[] args) throws IOException, ParseException {
        System.setProperty("file.encoding", "UTF-8");

        String queryPath = "C:\\CS853\\programAssignment3\\test200-train\\train.pages.cbor-outlines.cbor";

        String dataPath = "C:\\CS853\\programAssignment3\\test200-train\\train.pages.cbor-paragraphs.cbor";




        INDEX_DIRECTORY = args[0];
        queryPath = args[1];
        dataPath = args[2];
        OUTPUT_DIR = args[3];

        //indexer = new IndexData(INDEX_DIRECTORY, dataPath);
        QueryData queryData = new QueryData(queryPath);
//
        Map<String,String> pageMap = queryData.getAllPageQueries();
        Map<String,String> sectionMap = queryData.getAllSectionQueries();
        ArrayList<Data.Page> pageList = queryData.getPageList();

        // Store all query strings temporarily.


        System.out.println("Got " + pageMap.size() + " pages and " + sectionMap.size() + " sections.");

        // Lucene Search


        SearchData searcher = new SearchData(INDEX_DIRECTORY, pageMap, sectionMap, Max_Results);
        System.out.println("================");
        System.out.println("length is: " + pageList.size());
        UL page_ul = new UL(pageList, Max_Results, INDEX_DIRECTORY);
        writeFile("UnigramLanguageModel-Laplace.run", page_ul.getList());

        UDS page_uds = new UDS(pageList, Max_Results, INDEX_DIRECTORY, OUTPUT_DIR);

        UJM page_ujm = new UJM(pageList, Max_Results, INDEX_DIRECTORY);
        writeFile("UnigramLanguageModel-JM.run", page_ujm.getList());



        System.out.println("Finished");
    }

    public static void writeFile(String name, List<String> content){
        String fullpath = "C:\\CS953\\DataScienceSubmission1\\output" + "\\" + name;
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
