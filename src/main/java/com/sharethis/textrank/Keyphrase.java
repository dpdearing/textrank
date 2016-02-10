package com.sharethis.textrank;


public class Keyphrase {

    private String phrase;
    private double metric;

    public Keyphrase(String phrase, double metric) {
        this.phrase = phrase;
        this.metric = metric;
    }

    public String getPhrase() {
        return phrase;
    }

    public double getMetric() {
        return metric;
    }
}
