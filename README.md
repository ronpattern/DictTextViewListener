# DisctTextViewListener


Long press on the word of a textview to get its definition (online)


![picture alt](https://github.com/ronpattern/DisctTextViewListener/blob/master/screenshot/screen1.gif)


![picture alt](https://github.com/ronpattern/DisctTextViewListener/blob/master/screenshot/screen2.gif)


## Usage

If you want your text to be scrollable, you will have to encapsulate it in a DictScrollView :

```xml
<org.altmail.dicttextviewlistener.DictScrollView
    android:padding="16dp"
    android:scrollbarStyle="outsideOverlay"
    android:clipToPadding="false"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
 
    <org.altmail.dicttextviewlistener.DictTextView
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:textSize="16sp"
        android:id="@+id/text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/text"
        app:popupAccentColor="@color/colorAccent"
        app:popupBackgroundColor="@color/white"
        app:popupTitleColor="@color/transparent_black"
        app:popupBodyTextColor="@color/dark_secondary_text_color"
        app:popupPrimaryColor="@color/colorPrimary"
        app:longPressCountdown="@integer/defaultLongPressCountdown"
        app:lookUpCountdown="@integer/defaultLookupCountdown"
        app:enablePopupTwoDimensionsScroll="true"/>
    
</org.altmail.dicttextviewlistener.DictScrollView>
```  

The Popup can be dismissed by clicking outside of it, but if you want to use the back button you will have to add these lines in your main activity :


```java

    @Override
    public void onBackPressed() {
        if(!mDictTextView.dismissPopup()) {
            this.finish();
        }
    }

```

### Attribute description


**popupAccentColor :** color of the circular progress bar

**popupPrimaryColor :** color of the popup

**popupBackgroundColor :** background color of the progress bar

**popupTitleColor :** title color

**popupBodyTextColor :** regular text color

**enableTwoDimensionsScroll :** scroll horizontally and vertically, prevent TextView line-break

