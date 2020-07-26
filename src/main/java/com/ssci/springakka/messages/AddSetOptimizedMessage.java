package com.ssci.springakka.messages;

public class AddSetOptimizedMessage {

    private int sets;

    public AddSetOptimizedMessage(int sets) {
        this.sets = sets;
    }

    public int getSets() {
        return sets;
    }

}
