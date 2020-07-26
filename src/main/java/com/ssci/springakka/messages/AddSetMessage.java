package com.ssci.springakka.messages;

public class AddSetMessage {

    private int sets;

    public AddSetMessage(int sets) {
        this.sets = sets;
    }

    public int getSets() {
        return sets;
    }

}
