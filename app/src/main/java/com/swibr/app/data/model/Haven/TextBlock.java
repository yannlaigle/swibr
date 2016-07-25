package com.swibr.app.data.model.Haven;

/**
 * Created by Shide on 23/07/2016.
 */

public class TextBlock {
    public String text;
    public int left;
    public int top;
    public int width;
    public int height;

    public TextBlock(TextBlock textBlock) {

        this.text = textBlock.text;
        this.left = textBlock.left;
        this.top = textBlock.top;
        this.width = textBlock.width;
        this.height = textBlock.height;
    }

    public TextBlock(String text) {
        this.text = text;
    }

    public TextBlock(String text, int left, int top, int width, int height) {
        this.text = text;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }
}
