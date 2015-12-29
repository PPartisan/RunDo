package com.werdpressed.partisan.undoredo.fragmentexample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.werdpressed.partisan.rundo.RunDo;
import com.werdpressed.partisan.undoredo.R;

public class EditorFragment extends Fragment implements View.OnClickListener {

    private EditText mEditText;
    private Button mUndoButton, mRedoButton;

    private RunDo mRunDo;

    public static EditorFragment newInstance() {

        Bundle args = new Bundle();

        EditorFragment fragment = new EditorFragment();
        fragment.setArguments(args);
        return fragment;

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.editor_fragment, container, false);

        mRunDo = RunDo.Factory.getInstance(getFragmentManager());

        mEditText = (EditText) rootView.findViewById(R.id.test_edit_text_1);

        mUndoButton = (Button) rootView.findViewById(R.id.undo_button);
        mRedoButton = (Button) rootView.findViewById(R.id.redo_button);

        mUndoButton.setOnClickListener(this);
        mRedoButton.setOnClickListener(this);

        return rootView;
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

    public EditText getEditText() {
        return mEditText;
    }

}
