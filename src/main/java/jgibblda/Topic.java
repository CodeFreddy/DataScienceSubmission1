package main.java.jgibblda;

public class Topic {
    private String word;
    private double theta;

    public Topic(String word, double theta) {
        this.word = word;
        this.theta = theta;
    }

    public String toString() {
        System.err.println("word is : "+word);
        return word;
    }
}
