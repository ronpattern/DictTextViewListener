package org.altmail.dicttextviewlistener;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

/**
 * Created by rom on 05/02/18.
 */

public class DictScrollView extends ScrollView {


    private boolean mScrollable = true;

    public DictScrollView(Context context) {
        super(context);
    }

    public DictScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DictScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DictScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setScrollingEnabled(boolean enabled) {
        mScrollable = enabled;
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return mScrollable && super.onTouchEvent(ev);
            default:
                return super.onTouchEvent(ev);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return mScrollable && super.onInterceptTouchEvent(ev);
    }

    public void setHandler(final Handler handler) {
        if(handler != null) {
            getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    handler.removeCallbacksAndMessages(null);
                }
            });
        }
    }
}
