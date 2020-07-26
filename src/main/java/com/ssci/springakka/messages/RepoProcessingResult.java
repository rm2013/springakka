package com.ssci.springakka.messages;

import com.ssci.springakka.model.Contributor;

import java.util.List;

public class RepoProcessingResult {

    private List<Contributor> contributors;

    public RepoProcessingResult(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }
}
