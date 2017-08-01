package com.github.florent37.slidr;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.github.florent37.androidslidr.Slidr;

/**
 * Created by florentchampigny on 24/05/2017.
 */

public class EditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        final Slidr slidr = (Slidr) findViewById(R.id.slideure);
        slidr.setMin(200);
        slidr.setBubbleClickedListener(new Slidr.BubbleClickedListener() {
            @Override
            public void bubbleClicked(Slidr slidr) {

            }
        });
        slidr.setListener(new Slidr.Listener() {
            @Override
            public void valueChanged(Slidr slidr, float currentValue) {

            }
        });
    }
}
