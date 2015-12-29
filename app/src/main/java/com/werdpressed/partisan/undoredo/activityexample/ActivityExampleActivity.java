package com.werdpressed.partisan.undoredo.activityexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.werdpressed.partisan.rundo.RunDo;
import com.werdpressed.partisan.undoredo.R;

public class ActivityExampleActivity extends AppCompatActivity implements
        View.OnClickListener, RunDo.TextLink {

    private EditText testEditTextOne;

    private RunDo mRunDo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_example);

        getSupportActionBar().setTitle(getClass().getSimpleName());

        testEditTextOne = (EditText) findViewById(R.id.test_edit_text_1);

        Button undoButton = (Button) findViewById(R.id.undo_button);
        Button redoButton = (Button) findViewById(R.id.redo_button);

        undoButton.setOnClickListener(this);
        redoButton.setOnClickListener(this);

        mRunDo = RunDo.Factory.getInstance(getSupportFragmentManager());

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.undo_button:
                mRunDo.undo();
                break;
            case R.id.redo_button:
                mRunDo.redo();
                break;
        }

    }

    @Override
    public EditText getEditText() {
        return (testEditTextOne);
    }
}
