package com.yjarc.sonarus.UIHelper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.yjarc.sonarus.R;


public class ProgressBarMTE extends LinearLayout {

    View rootView;
    ProgressBar p1, p2;

    public ProgressBarMTE(Context context){
        super (context);
        init(context);
    }

    public ProgressBarMTE(Context context, AttributeSet attrs){
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        rootView = inflate(context, R.layout.progress_bar_mte, this);

        p1 = (ProgressBar) rootView.findViewById(R.id.progressBar_left);
        p2 = (ProgressBar) rootView.findViewById(R.id.progressBar_right);
    }

    public void setMax(int max){
        p1.setMax(max);
        p2.setMax(max);
    }

    public void setProgress(int progress){
        p1.setProgress(progress);
        p2.setProgress(progress);
    }

}
