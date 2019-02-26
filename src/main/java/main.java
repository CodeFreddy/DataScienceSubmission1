package main.java;

import edu.unh.cs.treccar_v2.Data;
import main.java.QueryExpansion.QueryExpansion;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
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
        //dataPath = args[2];
        OUTPUT_DIR = args[2];

        //indexer = new IndexData(INDEX_DIRECTORY, dataPath);
        QueryData queryData = new QueryData(queryPath);
//
        Map<String,String> pageMap = queryData.getAllPageQueries();
        Map<String,String> sectionMap = queryData.getAllSectionQueries();
        ArrayList<Data.Page> pageList = queryData.getPageList();
        ArrayList<Data.Section> sectionList = queryData.getSectionList();
        // Store all query strings temporarily.


        System.out.println("Got " + pageMap.size() + " pages and " + sectionMap.size() + " sections.");

        // Lucene Search


        //SearchData searcher = new SearchData(INDEX_DIRECTORY, pageMap, sectionMap, Max_Results);



        System.out.println("================");
        System.out.println("length is: " + pageList.size());


        UL page_ul = new UL(pageMap, Max_Results, INDEX_DIRECTORY);
        UL section_ul = new UL(sectionMap, Max_Results, INDEX_DIRECTORY);
        writeFile("UnigramLanguageModel-Laplace-Page.run", page_ul.getList());
        writeFile("UnigrameLanguageModel-Laplace-Section.run", section_ul.getList());

        UDS page_uds = new UDS(pageMap, Max_Results, INDEX_DIRECTORY, OUTPUT_DIR, "UnigramLanguageModel-UDS-Page.run");
        UDS section_uds = new UDS(sectionMap,Max_Results, INDEX_DIRECTORY, OUTPUT_DIR, "UnigramLanguageModel-UDS-Section.run");
        UJM page_ujm = new UJM(pageMap, Max_Results, INDEX_DIRECTORY);
        UJM section_ujm = new UJM(sectionMap, Max_Results, INDEX_DIRECTORY);
        writeFile("UnigramLanguageModel-JM-Page.run", page_ujm.getList());
        writeFile("UnigramLanguageModel-JM-Section.run", section_ujm.getList());


        System.out.println("Finished");
    }

    public static void writeFile(String name, List<String> content){
        String fullpath = OUTPUT_DIR + "/" + name;
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
