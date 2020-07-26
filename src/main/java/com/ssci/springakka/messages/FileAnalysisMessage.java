package com.ssci.springakka.messages;

public class FileAnalysisMessage {

    private String fileName;

    public FileAnalysisMessage(String file) {
        this.fileName = file;
    }

    public String getFileName() {
        return fileName;
    }
}
