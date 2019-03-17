package org.altmail.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.altmail.dicttextviewlistener.DictTextView;

public class MainActivity extends AppCompatActivity {

    private DictTextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
    }

    @Override
    public void onBackPressed() {

        if(!mTextView.dismissPopup()) {

            this.finish();
        }
    }
}
