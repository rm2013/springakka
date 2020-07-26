package com.ssci.springakka.messages;

public class RepoMessage {

    private String repoName;
    private String contributorsUrl;
    private String authorization;

    public RepoMessage(String repoName, String contributorsUrl, String authorization) {
        this.repoName = repoName;
        this.contributorsUrl = contributorsUrl;

        this.authorization = authorization;
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getContributorsUrl() {
        return contributorsUrl;
    }

    public void setContributorsUrl(String contributorsUrl) {
        this.contributorsUrl = contributorsUrl;
    }
}
