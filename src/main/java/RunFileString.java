package main.java;

class RunFileString {
    public String queryId;
    public String paraId;
    public int rank;
    public float score;
    public String methodName;
    public  String teamName;
    RunFileString()
    {
        queryId = "";
        paraId = "";
        rank = 0;
        score = 0.0f;
        methodName = "DS-Dirichlet";
        teamName = "Team 3";
    }

    RunFileString(String qid, String pid, int r, float s, String mName)
    {
        queryId = qid;
        paraId = pid;
        rank = r;
        score = s;
        methodName = mName;
        teamName = "Team 3";
    }

    public String toString()
    {
        return (queryId + " Q0 " + paraId + " " + rank + " " + score + " " + teamName + " " + methodName);
    }
}
