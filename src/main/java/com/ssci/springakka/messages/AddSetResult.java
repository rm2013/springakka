package com.ssci.springakka.messages;


import java.util.List;

public class AddSetResult {

    private List<AddItemMessage> addItems;

    public AddSetResult(List<AddItemMessage> addItems) {
        this.addItems = addItems;
    }

    public List<AddItemMessage> getAddItems() {
        return addItems;
    }
}
