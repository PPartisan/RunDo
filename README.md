# RunDo
![Banner Image](http://oi62.tinypic.com/s49utw.jpg)

__RunDo__ adds Undo/Redo functionality to `EditText` fields in Android. 

## Releases

#### [View Releases](https://github.com/PPartisan/RunDo/releases/ "Changelogs")

Current version is `v1.0.0`

JavaDoc available at [ppartisan.github.io](http://ppartisan.github.io/RunDo/JavaDoc/index.html "JavaDoc")

## Implementation ##

#### Gradle Dependency

##### jcenter() & mavenCentral()

Add the following to your module's `build.gradle` file:

    dependencies {
        compile 'com.werdpressed.partisan:rundo:1.0.0'
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

    RunDo.Factory.getInstance(FragmentManager fm, RunDo.TextLink textLink);
    
To be used in the following way (as an example):

    public class MyActivity extends Activity implements Rundo.TextLink, View.OnClickListener {
    
        private RunDo mRunDo;
        private EditText mEditText;
        private Button mButton;
        
        //...
    
        mRunDo = RunDo.Factory.getInstance(getFragmentManager(), this);
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
    
_See the [**sample app**](https://github.com/PPartisan/RunDo/blob/master/app/src/main/java/com/werdpressed/partisan/undoredo/MainActivity.java) for a complete example_

The `getInstance()` method requires a `FragmentManager` (whether [`android.app.FragmentManager`](http://developer.android.com/reference/android/app/FragmentManager.html) or [`android.suppoer.v4.app.FragmentManager`](http://developer.android.com/reference/android/support/v4/app/FragmentManager.html)) and [`RunDo.TextLink`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.TextLink.html) argument.

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
    
#### Clearing Queues

Use [`clearAllQueues()`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#clearAllQueues()) to remove all elements from both undo and redo queues.

#### Callbacks

Implement `RunDo.Callbacks` to be notified whenever [`undo()`](http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#undo()) or [`redo(http://ppartisan.github.io/RunDo/JavaDoc/com/werdpressed/partisan/rundo/RunDo.html#redo())`]() is called: 

    @Override
    public void undoCalled() {
    }

    @Override
    public void redoCalled() {
    }
    
