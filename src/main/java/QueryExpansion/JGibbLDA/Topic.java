package main.java.QueryExpansion.JGibbLDA;


public class Topic {

    private String word;
    private double theta;



    public Topic (String word, double theta){
        this.word = word;
        this.theta = theta;
    }

    public String toString(){
        return word;
    }
}
