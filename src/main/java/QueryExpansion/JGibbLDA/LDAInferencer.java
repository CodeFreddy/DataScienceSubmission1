package main.java.QueryExpansion.JGibbLDA;

public class LDAInferencer {

    private LDACmdOption ldaOption;
    private Inferencer inferencer;
    private static LDAInferencer wrapper;
    public static LDAInferencer getInstance(String modelDir, String modelName){
        if (wrapper == null){
            wrapper = new LDAInferencer();
        }

        return wrapper;
    }

    public LDAInferencer(){
        LDACmdOption ldaOption = new LDACmdOption();


        ldaOption.inf = true;
        ldaOption.dir = "./models";

        ldaOption.modelName = "model-final";
        ldaOption.niters = 100;

        Inferencer inferencer = new Inferencer();
        inferencer.init(ldaOption);
        this.inferencer = inferencer;

    }


    public static LDAInferencer getInstance(){
        if (wrapper == null)
            wrapper = new LDAInferencer();
        return  wrapper;
    }


    public Topic[] extractTopicByLDA(String[] input){
        Topic[] t = new Topic[input.length];


        Model newModel = this.inferencer.inference(input);

        int[] topicIndex = new int[input.length];

        double max = -1;
        int maxIndex = -1;


        for (int i = 0; i < newModel.theta.length;i++){
            max = -1;
            maxIndex = -1;

            for (int j = 0; j < newModel.theta[i].length;j++){
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

            for (int j = 0; j < newModel.V; j++){
                if (max < newModel.phi[topicIndex[i]][j]){
                    max = newModel.phi[topicIndex[i]][j];
                    maxIndex = j;
                }

            }


            if (newModel.data.localDict.contains((Integer) maxIndex)){
                t[i] = new Topic(newModel.data.localDict.getWord(maxIndex),newModel.theta[i][topicIndex[i]]);
            }
        }

        return t;
    }
}
