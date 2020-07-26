package com.ssci.springakka.messages;

import java.util.Map;

public class ProjectProcessedMessage {

    private Map<String, Long> data;

    public ProjectProcessedMessage(Map<String, Long> data) {
        this.data = data;
    }

    public Map<String, Long> getData() {
        return data;
    }
}
