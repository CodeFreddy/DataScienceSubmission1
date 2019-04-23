package main.java.jgibblda;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

public class InferencerWrapper {

    private static InferencerWrapper wrapper;
    private LDACmdOption ldaOption;
    private Inferencer inferencer;

    private String modelDir = "models";
    private String modelName = "model-final";

    public  InferencerWrapper(){

        ldaOption = new LDACmdOption();
        ldaOption.inf = true;
        ldaOption.dir = modelDir;
        ldaOption.modelName = modelName;//"model-LDA";
        ldaOption.niters = 100;
        Inferencer inferencer = new Inferencer();
        inferencer.init(ldaOption);
        this.inferencer = inferencer;

    }

    public List<String> getWordsByLDA(List<String> input){
        String[] arguments = new String[] {"-est",

                "-alpha","2","-beta","0.1","-ntopics","2","-niters","1000",
                "-savestep" ,"100","-twords", "5","-dir","models",
                "-dfile", "newdocs.dat"
        };

//        write(input);
        LDA lda = new LDA();

        lda.main(arguments);

        return lda.getWordRanking();
    }

    public void write(List<String> input){
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("models/newdocs.data"));
            out.write("1\n");
            for (String s: input){
                out.write(s+" ");
            }
            out.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }




    public Topic[] getTopicsByLDA(List<String> input){

        //write file
        String[] array = input.stream().toArray(String[]::new);

        Topic t [] = new Topic[array.length];
        Model newModel = inferencer.inference(array);

        int[] topicIndex = new int[array.length];

        double max=-1;
        int maxIndex=-1;

        //theta: document - topic distributions, size M x K
        for (int i = 0; i < newModel.theta.length;i++){
            max=-1;
            maxIndex=-1;

            for (int j = 0; j < newModel.theta[i].length; j++){
                if (max < newModel.theta[i][j]){
                    max = newModel.theta[i][j];
                    maxIndex = j;
                }
            }

            topicIndex[i] = maxIndex;
        }


        for (int i = 0; i < topicIndex.length;i++){
            max = -1;
            maxIndex = -1;


            //V is vocabulary size,for each word
            for (int j = 0; j < newModel.V;j++){
                if (max < newModel.phi[topicIndex[i]][j]){
                    max = newModel.phi[topicIndex[i]][j];
                    maxIndex = j;
                }
            }


            if (newModel.data.localDict.contains((Integer)maxIndex)){
                t[i] = new Topic(newModel.data.localDict.getWord(maxIndex),
                        newModel.theta[i][topicIndex[i]]);
            }
        }

        return t;


    }
}
