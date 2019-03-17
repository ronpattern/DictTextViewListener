package org.altmail.dicttextviewlistener;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class DictTextView extends android.support.v7.widget.AppCompatTextView {

    private final DictTouchListener mTouchListener;
    private final DictParams mDictParams;

    public DictTextView(Context context) {
        this(context, null);
    }

    public DictTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DictTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, org.altmail.dicttextviewlistener.R.styleable.DictTextView, defStyleAttr, org.altmail.dicttextviewlistener.R.style.DictTextView);

        final int accentColor = typedArray.getColor(org.altmail.dicttextviewlistener.R.styleable.DictTextView_accentColor, 0);
        final int bodyTextColor = typedArray.getColor(org.altmail.dicttextviewlistener.R.styleable.DictTextView_bodyTextColor, 0);
        final int countdown = typedArray.getInteger(org.altmail.dicttextviewlistener.R.styleable.DictTextView_lookUpCountdown, 0);
        final int longPressCountdown = typedArray.getInteger(org.altmail.dicttextviewlistener.R.styleable.DictTextView_longPressCountdown, 0);
        final int primaryColor = typedArray.getColor(org.altmail.dicttextviewlistener.R.styleable.DictTextView_primaryColor, 0);
        final int titleTextColor = typedArray.getColor(org.altmail.dicttextviewlistener.R.styleable.DictTextView_titleTextColor, 0);
        final int backgroundColor = typedArray.getColor(org.altmail.dicttextviewlistener.R.styleable.DictTextView_backgroundColor, 0);

        final float cornerRadius = typedArray.getDimension(org.altmail.dicttextviewlistener.R.styleable.DictTextView_cornerRadius, 0f);
        final float strokeWidth = typedArray.getDimension(org.altmail.dicttextviewlistener.R.styleable.DictTextView_strokeWidth, 0f);

        final boolean enableTwoDimensionsScroll = typedArray.getBoolean(R.styleable.DictTextView_enableTwoDimensionsScroll, true);

        typedArray.recycle();

        mDictParams = new DictParams(accentColor, bodyTextColor, primaryColor,
                titleTextColor, countdown, backgroundColor, longPressCountdown, strokeWidth, cornerRadius, enableTwoDimensionsScroll);

        mTouchListener = new DictTouchListener(this, mDictParams);

        setOnTouchListener(mTouchListener);
    }

    public boolean dismissPopup() {

        return mTouchListener.dismissPopup();
    }


    public void setPopupBodyTextColor(int bodyTextColor) {

        mDictParams.mBodyTextColor = bodyTextColor;
    }

    public void setPopupTitleTextColor(int titleTextColor) {

        mDictParams.mTitleTextColor = titleTextColor;
    }

    public void setPopupPrimaryColor(int primaryColor) {

        mDictParams.mPrimaryColor = primaryColor;
    }

    public void setPopupAccentColor(int accentColor) {

        mDictParams.mAccentColor = accentColor;
    }

    public void setLookupCountdown(int countdown) {

        mDictParams.mCountdown = countdown;
    }

    public void setLongPressCountdown(int longPressCountdown) {

        mDictParams.mLongPressCountdown = longPressCountdown;
    }

    public void setPopupBackgroundColor(int backgroundColor) {

        mDictParams.mBackgroundColor = backgroundColor;
    }

    public void setPopupStrokeWidth(float strokeWidth) {

        mDictParams.mStrokeWidth = strokeWidth;
    }

    public void setPopupCornerRadius(float cornerRadius) {

        mDictParams.mCornerRadius = cornerRadius;
    }
}
