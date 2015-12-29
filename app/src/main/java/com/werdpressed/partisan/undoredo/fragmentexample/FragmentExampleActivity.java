package com.werdpressed.partisan.undoredo.fragmentexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.werdpressed.partisan.rundo.RunDo;
import com.werdpressed.partisan.undoredo.R;

public class FragmentExampleActivity extends AppCompatActivity implements RunDo.TextLink {

    private EditorFragment mEditorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_example);

        getSupportActionBar().setTitle(getClass().getSimpleName());

        mEditorFragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.container);

        if (mEditorFragment == null) {
            mEditorFragment = EditorFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, mEditorFragment)
                    .commit();
        }

    }

    @Override
    public EditText getEditText() {
        return mEditorFragment.getEditText();
    }
}
