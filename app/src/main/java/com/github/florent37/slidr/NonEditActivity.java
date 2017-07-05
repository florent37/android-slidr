package com.github.florent37.slidr;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.github.florent37.androidslidr.Sushi;

/**
 * Created by florentchampigny on 24/05/2017.
 */

public class NonEditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_non_editable);

        final Sushi slidr = (Sushi) findViewById(R.id.slideure);
        slidr.getSettings().setDisplayMinMax(true);
        slidr.setMin(0);
        slidr.setMax(1000);
        slidr.setCurrentValue(300);
    }
}
