package com.ssci.springakka.messages;

public class AddItemMessage {

    private int addendOne;
    private int addendTwo;
    private int sum;

    public AddItemMessage(int addendOne, int addendTwo) {
        this.addendOne = addendOne;
        this.addendTwo = addendTwo;

        this.sum = 0;
    }

    public int getAddendOne() {
        return addendOne;
    }

    public int getAddendTwo() {
        return addendTwo;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

}
