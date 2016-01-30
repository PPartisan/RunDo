# RunDo
![Banner Image](http://oi62.tinypic.com/s49utw.jpg)

__RunDo__ adds Undo/Redo functionality to `EditText` fields in Android.

## Releases

#### [View Releases](https://github.com/PPartisan/RunDo/releases/ "Changelogs")

[ ![Download](https://api.bintray.com/packages/ppartisan/maven/rundo/images/download.svg) ](https://bintray.com/ppartisan/maven/rundo/_latestVersion)

Current version is `v1.0.4`

JavaDoc available at [ppartisan.github.io](http://ppartisan.github.io/RunDo/JavaDoc/index.html "JavaDoc")

## Implementation ##

#### Gradle Dependency

##### jcenter() & mavenCentral()

Add the following to your module's `build.gradle` file:

    dependencies {
        compile 'com.werdpressed.partisan:rundo:1.0.4'
    }
    
##### maven

In addition to the dependency above, add:

    repositories {
        maven {
            url 'https://dl.bintray.com/ppartisan/maven/'
        }
    }

#### Usage

Recommended usage is via the following method:

    RunDo.Factory.getInstance(FragmentManager fm);
    
To be used in the following way (as an example):

    public class MyActivity extends Activity implements Rundo.TextLink, View.OnClickListener {
    
        private RunDo mRunDo;
        private EditText mEditText;
        private Button mButton;
        
        //...
    
        mRunDo = RunDo.Factory.getInstance(getFragmentManager());
        mButton.setOnClickListener(this);
        
        //...
        
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
            return mEditText;
        }
        
    }
    
_See the [**sample app**](https://github.com/PPartisan/RunDo/blob/master/app/src/main/java/com/werdpressed/partisan/undoredo/) for a complete example_

The `getInstance()` method requires a `FragmentManager` (whether [`android.app.FragmentManager`](http://developer.android.com/reference/android/app/FragmentManager.html) or [`android.suppoer.v4.app.FragmentManager`](http://developer.android.com/reference/android/support/v4/app/FragmentManager.html)) argument.

`RunDo` implementations extend either [`android.app.Fragment`](http://developer.android.com/reference/android/app/Fragment.html) or [`android.support.v4.app.Fragment`](http://developer.android.com/reference/android/support/v4/app/Fragment.html). 

#### Calling Undo/Redo

To call Undo or Redo, use the `undo()` and `redo()` methods:

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
    
#### Tweaking Parameters

There are two ways to customise `RunDo` objects; [`setQueueSize(int size)`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#setQueueSize(int)) and [`setTimerLength(long lengthInMillis)`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#setTimerLength(long)).

`setQueueSize()` adjusts the size of the undo and redo queues to hold the specified number of entries, before entries from the opposite end of the queue begin to be removed. The default size is `10`. Calling this method will clear all current entries from both queues.

`setTimerLength()` adjust the countdown between the user's last text entry and the period at which any altered text is saved to the undo queue. The timer is reset if further text is entered during this period. The default value is `2000` milliseconds (2 seconds).
    
#### Clearing Queues

Use [`clearAllQueues()`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#clearAllQueues()) to remove all elements from both undo and redo queues.

#### Callbacks

Implement [`RunDo.Callbacks`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.Callbacks.html) to be notified whenever [`undo()`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#undo()) or [`redo()`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#redo()) is called: 

    @Override
    public void undoCalled() {
    }

    @Override
    public void redoCalled() {
    }
    
