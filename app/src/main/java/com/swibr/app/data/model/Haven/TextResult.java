package com.swibr.app.data.model.Haven;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shide on 23/07/2016.
 */
public class TextResult implements Serializable {


    public TextBlock[] text_block = null; // this is the haven parsed object
    private List<TextBlock> mTextBlocks = null;


    public TextResult() {
        this.mTextBlocks = new ArrayList<>();
        this.text_block = new TextBlock[0];
    }

    public TextResult(int length) {
        this.mTextBlocks = new ArrayList<>();
        this.text_block = new TextBlock[length];
    }

    public TextResult(List<TextBlock> textBlocks) {
        this.mTextBlocks = textBlocks;
        this.syncBlocks();
    }

    private void syncBlocks() {
        this.text_block = new TextBlock[mTextBlocks.size()];
        for (int i = 0; i < mTextBlocks.size(); i++) {
            this.text_block[i] = mTextBlocks.get(i);
        }
    }


    public void addTextBlock(TextBlock tb) {
        this.mTextBlocks.add(tb);
        this.syncBlocks();
    }

    public void removeTextBlock(TextBlock tb) {
        this.mTextBlocks.remove(tb);
        this.syncBlocks();
    }

    public void removeTextBlock(int index) {
        this.mTextBlocks.remove(index);
        this.syncBlocks();
    }

    public void clear() {
        this.mTextBlocks.clear();
        this.text_block = null;
    }


}
