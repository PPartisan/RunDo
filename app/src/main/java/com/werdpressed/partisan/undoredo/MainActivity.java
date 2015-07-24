package com.werdpressed.partisan.undoredo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        UndoRedoMixer.UndoRedoTextWatcher,
        UndoRedoMixer.UndoRedoCallbacks{

    SubtractStrings subtractStrings;
    TextView output, statusOutput, replacedText;
    Button outputButton, undoButton, redoButton;
    EditText testEditText;
    UndoRedoMixer mUndoRedoMixer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        output = (TextView) findViewById(R.id.text_output);
        statusOutput = (TextView) findViewById(R.id.status_textView);
        replacedText = (TextView) findViewById(R.id.replaced_text_textView);

        outputButton = (Button) findViewById(R.id.text_output_button);
        undoButton = (Button) findViewById(R.id.undo_button);
        redoButton = (Button) findViewById(R.id.redo_button);

        subtractStrings = new SubtractStrings(this);

        mUndoRedoMixer = (UndoRedoMixer) getFragmentManager().findFragmentByTag(UndoRedoMixer.UNDO_REDO_MIXER_TAG);

        if (mUndoRedoMixer == null) {
            testEditText = (EditText) findViewById(R.id.test_edit_text);
            mUndoRedoMixer = UndoRedoMixer.newInstance(testEditText.getId(), 0, 0);
            getFragmentManager()
                    .beginTransaction()
                    .add(mUndoRedoMixer, UndoRedoMixer.UNDO_REDO_MIXER_TAG)
                    .commit();
        }

        outputButton.setOnClickListener(this);
        undoButton.setOnClickListener(this);
        redoButton.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subtractStrings = null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.text_output_button:
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog_AppCompat_Light);
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.input_text_dialog, null);
                builder.setView(view);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText oldText, newText;
                        SubtractStrings.AlterationType alterationType;

                        oldText = (EditText) ((AlertDialog) dialog).findViewById(R.id.old_text_entry);
                        newText = (EditText) ((AlertDialog) dialog).findViewById(R.id.new_text_entry);

                        output.setText(subtractStrings.findAlteredText(oldText.getText().toString(), newText.getText().toString()));

                        alterationType = subtractStrings.findAlterationType(oldText.getText().toString().toCharArray(), newText.getText().toString().toCharArray());
                        statusOutput.setText(String.valueOf(alterationType));
                        replacedText.setText(subtractStrings.findReplacedText(alterationType, oldText.getText().toString().toCharArray(), newText.getText().toString().toCharArray()));

                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("Dismiss", null);
                builder.show();
                break;
            case R.id.undo_button:
                mUndoRedoMixer.undo();
                break;
            case R.id.redo_button:
                mUndoRedoMixer.redo();
                break;
        }

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void undoCalled() {
        Toast.makeText(this, "undo called", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void redoCalled() {
        Toast.makeText(this, "redo called", Toast.LENGTH_SHORT).show();
    }
}
