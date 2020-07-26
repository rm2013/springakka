package com.ssci.springakka.messages;


public class AddItemResult {

    private AddItemMessage addItemMessage;

    public AddItemResult(AddItemMessage addItemMessage) {
        this.addItemMessage = addItemMessage;
    }

    public AddItemMessage getAddItemMessage() {
        return addItemMessage;
    }
}
