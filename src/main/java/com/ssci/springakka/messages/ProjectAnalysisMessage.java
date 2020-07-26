package com.ssci.springakka.messages;

public class ProjectAnalysisMessage {

    private String projectName;
    private int tries;
    private String authorization;
    private boolean async;
    private boolean akka;

    public ProjectAnalysisMessage(String projectName, int tries, String authorization, boolean async, boolean akka) {
        this.projectName = projectName;
        this.tries = tries;
        this.authorization = authorization;
        this.async = async;
        this.akka = akka;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAuthorization() {
        return authorization;
    }

    public int getTries() {
        return tries;
    }

    public boolean getAsync() {
        return async;
    }

    public boolean getAkka() {
        return akka;
    }

}
