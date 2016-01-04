package com.swibr.app.ui.tutorial;

import android.os.Bundle;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

import com.swibr.app.R;
import com.swibr.app.ui.base.BaseActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by hthetiot on 12/29/15.
 */
public class TutorialActivity extends BaseActivity {

    @Bind(R.id.view_fliper) ViewFlipper viewFlipper;

    private float x1, x2;
    static final int MIN_DISTANCE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivityComponent().inject(this);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

    }

    private void showNext() {

        // Next screen comes in from right.
        viewFlipper.setInAnimation(this, R.anim.slide_in_from_right);
        // Current screen goes out from left.
        viewFlipper.setOutAnimation(this, R.anim.slide_out_to_left);

        // Display next screen.
        viewFlipper.showNext();
    }

    private void showPrevious() {
        // Next screen comes in from left.
        viewFlipper.setInAnimation(this, R.anim.slide_in_from_left);
        // Current screen goes out from right.
        viewFlipper.setOutAnimation(this, R.anim.slide_out_to_right);

        // Display previous screen.
        viewFlipper.showPrevious();
    }

    // Using the following method, we will handle all screen swaps.
    public boolean onTouchEvent(MotionEvent event) {

        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 > x1)
                    {
                        // If there aren't any other children, just break.
                        if (viewFlipper.getDisplayedChild() == 0)
                            break;

                        // Display previous screen.
                        showPrevious();

                    }

                    // Right to left swipe action
                    else
                    {
                        // If there aren't any other children, just break.
                        if (viewFlipper.getDisplayedChild() == viewFlipper.getChildCount() - 1) {
                            finish();
                        } else {
                            // Display next screen.
                            showNext();
                        }
                    }

                } else {

                    // Display next if available
                    if (viewFlipper.getDisplayedChild() < viewFlipper.getChildCount() - 1) {
                        showNext();
                    // Otherwise exist tutorial
                    } else {
                        finish();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
