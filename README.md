# RunDo
![Banner Image](https://lh3.googleusercontent.com/DRLJ-TQltjoYbdjlBbWSSe3WCi8ynHg6o_mX77CJ0jbyN5m-DFfmO7EUPb8WLvBIf3UyBBeemYbbTpA=w1885-h877-rw)

__RunDo__ allows developers to add Undo/Redo functionality to editable text fields in Android.  

This library aims to counteract some of the performance problems that can occur when working with large text blocks in Android in the following ways:
* Text is only saved to memory when the user stops typing. The bulk of code is only run when required, rather than updating with every key stroke.
* Only text that has changed between saves is committed to memory, rather than saving all text present in the widget.
* Only short, altered sections of text are inserted whenever an `undo()` or `redo()` method is called, and text fields are updated intelligently based on whether old text is to be added to, deleted from or replaced. This can drastically increase performance on older hardware, as calling `setText()` with large volumes of text can cause UI freezes.

## Implementation ##

### Gradle Dependency

##### jcenter()

Add the following to your module's `build.gradle` file:

    dependencies {
        compile 'com.werdpressed.partisan:rundo:0.1'
    }
    
##### maven

If you experience issues, add the following in addition to the dependency above:

    repositories {
        maven {
            url 'https://dl.bintray.com/ppartisan/maven/'
        }
    }

#### Instantiation

As the class extends `android.app.Fragment`, it requires `FragmentManager`

    RunDoMixer mRunDoMixer;
    //...
    mRunDoMixer = (RunDoMixer) getFragmentManager().findFragmentByTag(RunDoMixer.RUNDO_MIXER_TAG);

    if (mRunDoMixer == null) {
       testEditText = (EditText) findViewById(R.id.test_edit_text);
       mRunDoMixer = RunDoMixer.newInstance(testEditText.getId(), 0, 0);
       getFragmentManager()
           .beginTransaction()
           .add(mRunDoMixer, RunDoMixer.RUNDO_MIXER_TAG)
           .commit();
    }
The `newInstance()` method takes three paramters, all `int` values. The first is the `id` of either an `EditText` object, or the `id` of an object of a class that inherits from `EditText`. It is possible to pass `0` and later assign something suitable with `setEditText(Object object)`.

The other two parameters are optional. The first sets the countdown timer, in milliseconds, and determines how long  the system will wait from the last keypress before saving any altered text to the Undo queue. A value of less than one will revert to the default value of two seconds. The final parameter specifies the maximum size of the Undo and Redo queues before old entries are deleted. The default size is ten.

#### Calling Undo/Redo

To call Undo or Redo, use the `undo()` and `redo()` methods on the `RunDoMixer` object:

    mRunDoMixer.undo();
    mRunDoMixer.redo();

For example:

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.undo_button:
                mRunDoMixer.undo();
                break;
            case R.id.redo_button:
                mRunDoMixer.redo();
                break;
        }

    }
    
#### Visual Feedback

![Undo Empty Feedback](https://lh4.googleusercontent.com/06wUD6FtSokb8tH4-81IXukLrWFZiSIuWwbOZFFDIqkpqUg_klAL2SO6WqaDOrwsJd_8nX9ZN-AfvUg=w1896-h899)

A `Toast` notification will appear to let the user know that they have reached the end of the Undo/Redo queue. This will either occur if they are at one extreme end of a full queue, or if the next entry is `null`.

It is possible to deactivate this feature, or change the message, with `setUndoQueueEmptyMessage(boolean condition, String message)` and `setRedoQueueEmptyMessage(coolean condition, String message)`. Setting `condition` to `false` will deactivate these notifications entirely, whilst any non-`null` text in the second argument will override the default message.

#### Hardware Keyboard Shortcuts

By default, the follwing shortcuts are ennabled for hardware keyboard users:
* `Ctrl + Z` for Undo
* `Ctrl + Y` for Redo
It is possible to deactive these with `setKeyboardShortcuts(false)`.

### Callbacks

To receive a callback whenever `undo()` or `redo()` is called, have your class implement `RunDoMixer.UndoRedoCallbacks`. This will provide the following methods:

    @Override
    public void undoCalled() {
    }

    @Override
    public void redoCalled() {
    }

## Other Extras...

__RunDo__ relies heavily on a custom `String` comparison class called `SubtractStrings`. `SubtractStrings` compares two `String` objects, and returns data regarding:
* The points where the two strings first and last deviate from each other.
* The `subString` from both the old text and new text, if one replaced a section of text in the other.
* Options to return values in respect to the larger of the two strings, or in respect to the "new" text in relation to the "old" text.

This class may be useful in its own right for some projects.
