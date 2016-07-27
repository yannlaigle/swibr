package com.swibr.app.ui.main;

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import butterknife.Bind;
import butterknife.ButterKnife;

import com.swibr.app.R;
import com.swibr.app.data.model.Article.Article;

public class SwibrsAdapter extends RecyclerView.Adapter<SwibrsAdapter.SwibrViewHolder> {

    private List<Article> mArticles;

    @Inject
    public SwibrsAdapter() {
        mArticles = new ArrayList<>();
    }

    public void setSwibrs(List<Article> articles) {
        mArticles = articles;
    }

    @Override
    public SwibrViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_swibr, parent, false);
        return new SwibrViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final SwibrViewHolder holder, int position) {
        Article article = mArticles.get(position);
        holder.hexColorView.setBackgroundColor(Color.parseColor(article.hexColor));
        holder.nameTextView.setText(article.title);
        holder.descTextView.setText(article.description);
    }

    @Override
    public int getItemCount() {
        return mArticles.size();
    }

    class SwibrViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.view_hex_color)
        View hexColorView;
        @Bind(R.id.text_name)
        TextView nameTextView;
        @Bind(R.id.text_email)
        TextView descTextView;
        @Bind(R.id.card_view)
        CardView cardView;

        public SwibrViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
