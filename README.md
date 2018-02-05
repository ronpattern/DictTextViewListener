# DisctTextViewListener


Long press on the word of a textview to search for its definition online


![picture alt](https://github.com/ronpattern/DisctTextViewListener/blob/master/screenshot/screen1.gif)


![picture alt](https://github.com/ronpattern/DisctTextViewListener/blob/master/screenshot/screen2.gif)


## Usage

You can use a regular TextView, however if you want your text to be scrollable, you will have to encapsulate it in a DictScrollView :

```xml
<org.altmail.dicttextviewlistener.DictScrollView
    android:padding="16dp"
    android:scrollbarStyle="outsideOverlay"
    android:clipToPadding="false"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <TextView
        android:textSize="16sp"
        android:id="@+id/text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/text" />
</org.altmail.dicttextviewlistener.DictScrollView>
```  

**In main Activy or Fragment :**  


```java

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.text);
        DictTouchListener touchListener = new DictTouchListener(textView, (ViewGroup) findViewById(android.R.id.content));
        textView.setOnTouchListener(touchListener);
    }

```

The Popup can be dismissed by clicking outside of it, but if you want to use the back button you will have to add these lines on your main activity :


```java

    @Override
    public void onBackPressed() {
        if(!myDictTouchListener.dismissPopup()) {
            this.finish();
        }
    }

```

