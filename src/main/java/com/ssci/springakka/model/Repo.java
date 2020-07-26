package com.ssci.springakka.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Repo {
    private Long id;
    private String name;
    @JsonAlias("contributors_url")
    private String contributorsUrl;


    public Repo(Long id, String name, String contributorsUrl) {
        this.id = id;
        this.name = name;
        this.contributorsUrl = contributorsUrl;
    }

    public Repo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContributorsUrl() {
        return contributorsUrl;
    }

    public void setContributorsUrl(String contributorsUrl) {
        this.contributorsUrl = contributorsUrl;
    }
}
