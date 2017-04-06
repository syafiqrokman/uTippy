package com.levelzeros.utippy.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.levelzeros.utippy.R;

/**
 * Created by Poon on 6/4/2017.
 */

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";

    public static final String ELEARNING_DOMAIN = "elearning-domain";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        Button mElearningButton = (Button) findViewById(R.id.elearning_button);
        mElearningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
                loginIntent.putExtra(ELEARNING_DOMAIN, getString(R.string.option_elearning));
                startActivity(loginIntent);
            }
        });

        Button mULearnButton = (Button) findViewById(R.id.ulearn_button);
        mULearnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
                loginIntent.putExtra(ELEARNING_DOMAIN, getString(R.string.option_ulearn));
                startActivity(loginIntent);
            }
        });
    }
}
