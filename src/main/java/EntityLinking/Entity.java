package main.java.EntityLinking;

public class Entity {



    private String URI;
    private int support;
    private String types;
    private String surfaceForm;//Name
    private int offset ;
    private float similarityScore;
    private float percentageOfSecondRank;


    public String getURI(){
        return URI;
    }

    public void setURI(String URI){
        this.URI = URI;
    }

    public int getSupport(){
        return support;
    }

    public void setSupport(int support){
        this.support = support;
    }


    public String getTypes(){
        return types;
    }

    public void setTypes(String types){
        this.types = types;
    }

    public String getSurfaceForm(){
        return surfaceForm;
    }

    public void setSurfaceForm(String surfaceForm){
        this.surfaceForm = surfaceForm;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public float getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(float similarityScore) {
        this.similarityScore = similarityScore;
    }

    public float getPercentageOfSecondRank() {
        return percentageOfSecondRank;
    }

    public void setPercentageOfSecondRank(float percentageOfSecondRank) {
        this.percentageOfSecondRank = percentageOfSecondRank;
    }

    public String toString(){
        return "EntityWord [" + surfaceForm+ "]";
    }


}
