# RunDo
![Banner Image](http://oi62.tinypic.com/s49utw.jpg)

__RunDo__ allows developers to add Undo/Redo functionality to editable text fields in Android.  

This library aims to counteract some of the performance problems that can occur when working with large text blocks in Android in the following ways:
* Text is only saved to memory when the user stops typing. The bulk of code is only run when required, rather than updating with every key stroke.
* Only text that has changed between saves is committed to memory, rather than saving all text present in the widget.
* Only short, altered sections of text are inserted whenever an `undo()` or `redo()` method is called, and text fields are updated intelligently based on whether old text is to be added to, deleted from or replaced. This can drastically increase performance on older hardware, as calling `setText()` with large volumes of text can cause UI freezes.

## Releases

#### [View Releases](https://github.com/PPartisan/RunDo/releases/ "Changelogs")

Current version is `v0.2.3`

JavaDoc available at [ppartisan.github.io](http://ppartisan.github.io/RunDo/JavaDoc/index.html "JavaDoc")

## Implementation ##

#### Gradle Dependency

##### jcenter() & mavenCentral()

Add the following to your module's `build.gradle` file:

    dependencies {
        compile 'com.werdpressed.partisan:rundo:0.2.3'
    }
    
##### maven

If you experience issues, add the following in addition to the dependency above:

    repositories {
        maven {
            url 'https://dl.bintray.com/ppartisan/maven/'
        }
    }

#### Instantiation

`RunDo` extends `android.app.Fragment`:

    RunDo mRunDo;
    //...
    mRunDo = (RunDo) getFragmentManager().findFragmentByTag(RunDo.TAG);

    if (mRunDo == null) {
       testEditText = (EditText) findViewById(R.id.test_edit_text);
       mRunDo = RunDoMixer.newInstance(testEditText.getId());
       getFragmentManager()
           .beginTransaction()
           .add(mRunDo, RunDo.TAG)
           .commit();
    }
    
`newInstance(int editTextResourceId)` is shorthand for `newInstance(editTextResourceId, 0, 0)`.

The `newInstance()` method takes the `id` of either an `EditText` object, or the `id` of an object of a class that inherits from `EditText`, as its sole argument. It is possible to pass `0` and later assign something suitable with `setEditText(Object object)`.

There are two additional, optional, parameters if using `newInstance(int editTextResourceId, int countDown, int arraySize)`. The first sets the countdown timer, in milliseconds, and determines how long  the system will wait from the last keypress before saving any altered text to the Undo queue. A value of less than one will revert to the default value of two seconds. The final parameter specifies the maximum size of the Undo and Redo queues before old entries are deleted. Again, a value of less than one will use the default size of ten.

JavaDoc entry for [`newInstance(int)`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#newInstance(int) "newInstance(int)") and [`newInstance(int, int, int)`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#newInstance(int,%20int,%20int) "newInstance(int, int, int)")

#### Calling Undo/Redo

To call Undo or Redo, use the `undo()` and `redo()` methods on the `RunDo` object:

    mRunDo.undo();
    mRunDo.redo();

For example:

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
    
#### Visual Feedback

![Undo Empty Feedback](http://oi59.tinypic.com/2v9u81e.jpg)

A `Toast` notification will appear to let the user know that they have reached the end of the Undo/Redo queue. This will either occur if they are at one extreme end of a full queue, or if the next entry is `null`.

To deactivate this feature, or change the message, use `setUndoQueueEmptyMessage(boolean condition, String message)` and `setRedoQueueEmptyMessage(coolean condition, String message)`. Setting `condition` to `false` will deactivate these notifications entirely, whilst any non-`null` text in the second argument will override the default message.

#### Hardware Keyboard Shortcuts

By default, the follwing shortcuts are enabled for hardware keyboard users:

* `Ctrl + Z` for Undo
* `Ctrl + Y` for Redo

Deactivate with `setKeyboardShortcuts(false)`.

#### Clearing Queues

To clear all Undo and Redo queues, use `clearAllQueues()`

#### Callbacks

To receive a callback whenever `undo()` or `redo()` is called, have your class implement `RunDoMixer.UndoRedoCallbacks`. This will provide the following methods:

    @Override
    public void undoCalled() {
    }

    @Override
    public void redoCalled() {
    }
    
#### Error Handling

The `undo()` and `redo()` methods should not throw any errors, but are nonetheless enclosed in `try/catch` blocks in case `IndexOutOfBoundsException` occurs when calculating deviation points or setting text.

By default, a `Toast` notification will appear with the exception's `toString()` result as its content, the stack trace printed to `LogCat` and all Undo and Redo queues cleared. To disable this behaviour, use `setAutoErrorHandling(false)`.

To create your own error handling, implement `RunDo.ErrorHandlingCallback` and use the resulting methods:

    @Override
    public void undoError(IndexOutOfBoundsException e) {
        //Error handling code
    }

    @Override
    public void redoError(IndexOutOfBoundsException e) {
        //Error handling code
    }

It is strongly recommended that `clearAllQueues()` is called if auto error handling is disabled, as it is likely the data stored in both queues is invalid.

## Other Extras...

__RunDo__ relies heavily on a custom `String` comparison class called `SubtractStrings`. `SubtractStrings` compares two `String` objects, and returns data regarding:
* The points where the two strings first and last deviate from each other.
* The `subString` from both the old text and new text, if one replaced a section of text in the other.
* Options to return values in respect to the larger of the two strings, or in respect to the "new" text in relation to the "old" text.

This class may be useful in its own right for some projects.
