package main.java;


import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import main.java.EntityLinking.Entity;
import main.java.EntityLinking.EntityFinder;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexData {
//    static final private String INDEX_DIRECTORY = "index";

    private static IndexWriter indexWriter;
    public static EntityFinder entityFinder = new EntityFinder();
    public static List<Data.Paragraph> reIndex = new ArrayList<>();

    //INDEX_DIRECTORY is the path of index, filepath is the directory of corpus
    public IndexData(String INDEX_DIRECTORY,String filePath) throws Exception {

        //change to Eng analyzer
        Directory indexDir = FSDirectory.open((new File(INDEX_DIRECTORY)).toPath());

        //IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());

        IndexWriterConfig config = new IndexWriterConfig(new EnglishAnalyzer());

        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        indexWriter = new IndexWriter(indexDir, config);

        System.out.println("Start Indexing...");

        int count = 0;
        for(Data.Paragraph p: DeserializeData.iterableParagraphs(new FileInputStream(new File(filePath))))
        {
            //System.out.println(count++);
            Document doc = convertToLuceneDoc(p);
            if (doc != null){
                indexWriter.addDocument(doc);
                System.out.println(count++);

                if (count %100 == 0){
                    indexWriter.commit();
                }
            }

        }

        System.out.println("=======================");
        System.out.println("Indexing was done");
//        indexWriter.commit();


    }

    public static Document convertToLuceneDoc (Data.Paragraph paragraph) throws Exception
    {
        Document doc = new Document();
        doc.add(new StringField("paraid", paragraph.getParaId(), Field.Store.YES));//id
        doc.add(new TextField("content", paragraph.getTextOnly(), Field.Store.YES));//body
//        HashMap<String, Float> bigram_score = BigramIndex.createBigramIndexFiled(paragraph.getTextOnly());
//        doc.add(new TextField("bigram", bigram_score.toString(), Field.Store.YES));

        List<Entity> linkedEntity = new ArrayList<>();

        //System.out.println("query is:"+paragraph.getTextOnly());

        try{
            linkedEntity = entityFinder.getRelatedEntity(paragraph.getTextOnly());

        }catch (Exception e){
            System.err.println("cannot get json response from spotlight");
            try{
                linkedEntity = entityFinder.getRelatedEntity(paragraph.getTextOnly());
            }catch (Exception e1){
                System.err.println("Again!!cannot get json response from spotlight");
                reIndex.add(paragraph);
                return null;
            }
        }

        if (linkedEntity !=null    ){
            //System.out.println("size: "+linkedEntity.size());
            for (Entity entity : linkedEntity ){
                String e = entity.getURI().substring(entity.getURI().lastIndexOf("/")+1);

                doc.add(new StringField("spotlight",e,Field.Store.YES));
            }
        }

        return doc;
    }


    public static void reIndex() throws Exception{
        System.out.print("start to reindex");
        while (reIndex.size() != 0){
            System.out.println("reIndex.size():"+reIndex.size());
            for (int i = reIndex.size()-1;i >= 0 ; i--){
                Data.Paragraph p = reIndex.get(i);
                Document doc = new Document();
                doc.add(new StringField("paraid", p.getParaId(), Field.Store.YES));//id
                doc.add(new TextField("content", p.getTextOnly(), Field.Store.YES));//body

                List<Entity> linkedEntity = new ArrayList<>();
                try{
                    linkedEntity = entityFinder.getRelatedEntity(p.getTextOnly());

                }catch (Exception e){
                    System.err.println("cannot get json response from spotlight");
                    doc.add(new StringField("spotlight",p.getTextOnly(),Field.Store.YES));

                }

                reIndex.remove(i);
                if (linkedEntity !=null    ){
                    //System.out.println("size: "+linkedEntity.size());
                    for (Entity entity : linkedEntity ){
                        String e = entity.getURI().substring(entity.getURI().lastIndexOf("/")+1);

                        doc.add(new StringField("spotlight",e,Field.Store.YES));
                    }
                }

//                HashMap<String, Float> bigram_score = BigramIndex.createBigramIndexFiled(paragraph.getTextOnly());
//                doc.add(new TextField("bigram", bigram_score.toString(), Field.Store.YES));


                indexWriter.addDocument(doc);

            }
        }
        indexWriter.close();

    }

}
