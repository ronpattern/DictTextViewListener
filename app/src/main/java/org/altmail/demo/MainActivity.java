package org.altmail.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import org.altmail.dicttextviewlistener.DictTouchListener;

public class MainActivity extends AppCompatActivity {

    private TextView mTextView;
    private DictTouchListener mTouchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.text);
        mTouchListener = new DictTouchListener(mTextView, (ViewGroup) findViewById(android.R.id.content));
        mTextView.setOnTouchListener(mTouchListener);
    }

    @Override
    public void onBackPressed() {
        if(!mTouchListener.dismissPopup()) {
            this.finish();
        }
    }
}
