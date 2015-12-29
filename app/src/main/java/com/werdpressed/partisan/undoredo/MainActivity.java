package com.werdpressed.partisan.undoredo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.werdpressed.partisan.undoredo.activityexample.ActivityExampleActivity;
import com.werdpressed.partisan.undoredo.fragmentexample.FragmentExampleActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button mActivityExampleButton = (Button) findViewById(R.id.activity_example_btn);
        Button mFragmentExampleButton = (Button) findViewById(R.id.fragment_example_btn);

        mActivityExampleButton.setOnClickListener(this);
        mFragmentExampleButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        Intent intent;

        switch (v.getId()) {
            case R.id.activity_example_btn:
                intent = new Intent(this, ActivityExampleActivity.class);
                startActivity(intent);
                break;
            case R.id.fragment_example_btn:
                intent = new Intent(this, FragmentExampleActivity.class);
                startActivity(intent);
                break;
        }

    }

}
