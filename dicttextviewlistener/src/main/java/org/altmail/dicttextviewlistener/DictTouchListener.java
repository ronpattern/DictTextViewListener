package org.altmail.dicttextviewlistener;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by rom on 02/02/18.
 */

public class DictTouchListener implements View.OnTouchListener {

    private static final String DEBUG_TAG = "DictTouchListener";

    static final String MESSAGE_TAG = "message";
    static final String RESULT_TAG = "result";

    private static final String LAST_WORD_DEFAULT_VALUE = "";

    private static final float POPUP_PERCENT_WIDTH_SCREEN_SIZE = 0.90f;
    private static final float POPUP_PERCENT_HEIGHT_SCREEN_SIZE = 0.70f;

    private static final float MAX_FLOAT_ANIMATOR_VALUE = 1f;
    private static final float MIN_FLOAT_ANIMATOR_VALUE = 0f;

    private static final int MIN_PROGRESS_DRAWABLE_LEVEL = 0;
    private static final int MAX_PROGRESS_DRAWABLE_LEVEL = 10000;

    private static final int POPUP_DISPLAY_COUNTDOWN = 400;

    private static final int DISPLAY_RESULT_FADE_IN_ANIMATION_TIME = 600;
    private static final int PROGRESS_BAR_ANIMATION_TIME = 2000;

    private static final int POPUP_EXPANSION_ANIMATION_TIME = 300;
    private static final int DEFAULT_SOCKET_PORT = 2628;
    private static final String DEFAULT_SERVER_ADDRESS = "dict.org";

    private static final int CURRENT_POPUP_LIST_INDEX = 0;

    private static final int SCROLL_THRESHOLD = 15;

    private static final int SHOW_PROGRESS_EVENT_MESSAGE_ID = 0;
    private static final int DISPLAY_RESULT_EVENT_MESSAGE_ID = 1;
    private static final int DISPLAY_ERROR_EVENT_MESSAGE_ID = 2;

    private PointF mPointerPosition;
    private ImageView mProgressBackground;
    private CharSequence mLastWord;
    private View mBackgroundFilter;
    private ConstraintLayout mRoot;
    private ScrollView mPopupContent;
    private ProgressBar mProgressBar;
    private DefineTask mDefineTask;

    private boolean mTriggered, mPopupVisible;
    private int[] mWordOffsetInterval;

    private final TextView mTextView;
    private final ValueAnimator mValueAnimator;
    private final ViewGroup mContainer;
    private final ArrayList<View> mPopupList;
    private final Context mContext;

    private final int mPadding, mGradientHeight;
    private final boolean mIsPortraitMode, mHasLockableScrollView;

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case SHOW_PROGRESS_EVENT_MESSAGE_ID:
                    Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                    if(v != null) {
                        v.vibrate(100);
                    }
                    if(mHasLockableScrollView) {
                        ((DictScrollView)mTextView.getParent()).setScrollingEnabled(false);
                    }
                    showProgress(mPointerPosition.x, mPointerPosition.y);
                    break;
                case DISPLAY_RESULT_EVENT_MESSAGE_ID:
                    ParcelableLinkedList result = message.getData().getParcelable(RESULT_TAG);
                    if(mPopupVisible && mTriggered) {
                        mProgressBar.setVisibility(View.GONE);
                        displayDefinitions(new DictParser(result.getLinkedList()));
                    }
                    break;
                case DISPLAY_ERROR_EVENT_MESSAGE_ID:
                    String msg = message.getData().getString(MESSAGE_TAG);
                    if(mPopupVisible && mTriggered) {
                        mProgressBar.setVisibility(View.GONE);
                        displayError(msg);
                    }
                    break;
            }
            return false;
        }
    });

    public DictTouchListener(TextView textView, ViewGroup container) {
        mTextView = textView;
        mContainer = container;
        mLastWord = LAST_WORD_DEFAULT_VALUE;
        mPopupList = new ArrayList<View>();
        mContext = mContainer.getContext();
        if(mTextView.getParent() instanceof DictScrollView) {
            mHasLockableScrollView = true;
            ((DictScrollView)mTextView.getParent()).setHandler(mHandler);
        } else {
            mHasLockableScrollView = false;
        }
        mPadding = mContext.getResources().getDimensionPixelSize(R.dimen.scroll_view_padding);
        mIsPortraitMode = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        mGradientHeight = mContext.getResources().getDimensionPixelSize(R.dimen.gradient_border_height);
        mValueAnimator = ValueAnimator.ofInt(MIN_PROGRESS_DRAWABLE_LEVEL, MAX_PROGRESS_DRAWABLE_LEVEL);
        mValueAnimator.setDuration(PROGRESS_BAR_ANIMATION_TIME);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if(mProgressBackground != null) {
                    mProgressBackground.setImageLevel((Integer) valueAnimator.getAnimatedValue());
                }
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                if(mPopupVisible) {
                    expandPopup();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
    }

    private void expandPopup() {
        mTriggered = true;
        final int outHeight = (int) (mContainer.getHeight() * (mIsPortraitMode?POPUP_PERCENT_HEIGHT_SCREEN_SIZE:POPUP_PERCENT_WIDTH_SCREEN_SIZE));
        final int outWidth = (int) (mContainer.getWidth() * (mIsPortraitMode?POPUP_PERCENT_WIDTH_SCREEN_SIZE:POPUP_PERCENT_HEIGHT_SCREEN_SIZE));
        final int outX = (int) ((mContainer.getWidth() - outWidth)/2);
        final int outY = (int) ((mContainer.getHeight() - outHeight)/2);
        FrameLayout.LayoutParams l = (FrameLayout.LayoutParams)mPopupList.get(CURRENT_POPUP_LIST_INDEX).getLayoutParams();
        final int currentWidth = mRoot.getWidth();
        final int currentHeight = mRoot.getHeight();
        final int currentX = l.leftMargin;
        final int currentY = l.topMargin;
        ViewCompat.setBackground(mRoot, ContextCompat.getDrawable(mContext, R.drawable.popup_window_background_full));
        mProgressBackground.setVisibility(View.GONE);
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(MIN_FLOAT_ANIMATOR_VALUE, MAX_FLOAT_ANIMATOR_VALUE);
        valueAnimator.setDuration(POPUP_EXPANSION_ANIMATION_TIME);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mBackgroundFilter = new View(mContext);
        mBackgroundFilter.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mBackgroundFilter.setBackgroundColor(ContextCompat.getColor(mContext, R.color.transparent_black));
        mBackgroundFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeExpandedPopup();
            }
        });
        mContainer.addView(mBackgroundFilter);
        mPopupList.get(CURRENT_POPUP_LIST_INDEX).bringToFront();
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int width = (int) (currentWidth + ((float)valueAnimator.getAnimatedValue() * (outWidth - currentWidth)));
                int height = (int) (currentHeight + ((float)valueAnimator.getAnimatedValue() * (outHeight - currentHeight)));
                int X = (int) (currentX + ((float)valueAnimator.getAnimatedValue() * (outX - currentX)));
                int Y = (int) (currentY + ((float)valueAnimator.getAnimatedValue() * (outY - currentY)));
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)mPopupList.get(0).getLayoutParams();
                layoutParams.width = width;
                layoutParams.height = height;
                layoutParams.setMargins(X, Y, 0, 0);
                mPopupList.get(CURRENT_POPUP_LIST_INDEX).setLayoutParams(layoutParams);
                mBackgroundFilter.setAlpha((float)valueAnimator.getAnimatedValue());
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                addPopupContent();
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        valueAnimator.start();
    }

    private void addPopupContent() {
        mRoot.setPadding(0, 0, 0, mPadding);
        mPopupContent = new ScrollView(mContext);
        mPopupContent.setId(R.id.popup_content);
        mPopupContent.setPadding(mPadding, mPadding, mPadding, mPadding);
        mPopupContent.setClipToPadding(false);
        mPopupContent.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mProgressBar = new ProgressBar(mContext);
        mProgressBar.setId(R.id.progress_bar);
        View topGradient = new View(mContext);
        View bottomGradient = new View(mContext);
        topGradient.setBackgroundResource(R.drawable.gradient_border_top);
        bottomGradient.setBackgroundResource(R.drawable.gradient_border_bottom);
        topGradient.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mGradientHeight, Gravity.TOP));
        bottomGradient.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mGradientHeight, Gravity.BOTTOM));
        FrameLayout fl = new FrameLayout(mContext);
        fl.setId(R.id.frame_layout);
        fl.addView(topGradient);
        fl.addView(bottomGradient);
        mRoot.addView(mPopupContent);
        mRoot.addView(mProgressBar);
        mRoot.addView(fl);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.constrainHeight(R.id.popup_content, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainWidth(R.id.popup_content, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(R.id.progress_bar, ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainWidth(R.id.progress_bar, ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainHeight(R.id.frame_layout, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(R.id.frame_layout, ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.connect(R.id.popup_content, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.popup_content, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.popup_content, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(R.id.popup_content, ConstraintSet.TOP, R.id.word, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(R.id.frame_layout, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.frame_layout, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.frame_layout, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(R.id.frame_layout, ConstraintSet.TOP, R.id.word, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(R.id.progress_bar, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.progress_bar, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.progress_bar, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.progress_bar, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo(mRoot);
        doLookup();
    }

    private void showProgress(float x, float y) {
        CharSequence text = mTextView.getText();
        int offset = mTextView.getOffsetForPosition(x, y);
        if(offset<mTextView.length()) {
            if(!Character.isLetter(text.charAt(offset))) {
                if(mLastWord.equals(LAST_WORD_DEFAULT_VALUE)) {
                    offset = getClosestWord(offset, text);
                    if(offset == -1) {
                        return;
                    }
                } else {
                    return;
                }
            }
            int left = offset;
            int right = offset;
            boolean isLetter = true;
            while (isLetter) {
                if (left > 0 && Character.isLetter(text.charAt(left - 1))) {
                    left--;
                } else {
                    isLetter = false;
                }
            }
            isLetter = true;
            while (isLetter) {
                if (right < text.length() - 1 && Character.isLetter(text.charAt(right + 1))) {
                    right++;
                } else {
                    right++;
                    isLetter = false;
                }
            }
            if (right <= text.length() - 1) {
                CharSequence word = text.subSequence(left, right);
                if (word.length() > 1) {
                    if (!word.equals(mLastWord)) {
                        if (mPopupVisible) {
                            removePopup();
                            mPointerPosition = new PointF(x, y);
                        }
                        mLastWord = word;
                        mWordOffsetInterval = new int[]{left, right};
                        final Rect rect = getCoordinates(left, right);
                        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                        View v = layoutInflater.inflate(R.layout.popup_window_layout, null);
                        mProgressBackground = (ImageView) v.findViewById(R.id.img);
                        mRoot = (ConstraintLayout) v.findViewById(R.id.root);
                        TextView textView = v.findViewById(R.id.word);
                        textView.setText(word);
                        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        int width = v.getMeasuredWidth();
                        int height = v.getMeasuredHeight();
                        int outLeft;
                        if (width >= rect.width()) {
                            outLeft = rect.left - ((width - rect.width()) / 2);
                        } else {
                            outLeft = rect.left + ((rect.width() - width) / 2);
                        }
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.setMargins(outLeft, rect.top - (int) mTextView.getTextSize() - height, 0, 0);
                        v.setLayoutParams(layoutParams);
                        Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
                        mContainer.addView(v);
                        v.startAnimation(fadeIn);
                        mPopupList.add(CURRENT_POPUP_LIST_INDEX, v);
                        mPopupVisible = true;
                        mValueAnimator.start();
                    }
                }
            }
        }
    }

    // case with space or comma, point ...
    private static int getClosestWord(int offset, CharSequence text) {
        if(offset>1 && Character.isLetter(text.charAt(offset-1)) && Character.isLetter(text.charAt(offset-2))) {
            return offset-1;
        } else if(offset<text.length()-2 && Character.isLetter(text.charAt(offset+1)) && Character.isLetter(text.charAt(offset+2))) {
            return offset+1;
        } else {
            return -1;
        }
    }

    private Rect getCoordinates(int left, int right) {
        final Rect textViewRect = new Rect();
        final Layout textViewLayout = mTextView.getLayout();
        double startXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(left);
        double endXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(right);
        int currentLineStartOffset = textViewLayout.getLineForOffset(left);
        textViewLayout.getLineBounds(currentLineStartOffset, textViewRect);
        double parentTextViewTopAndBottomOffset = (
                mTextView.getY() -
                        (mHasLockableScrollView?((DictScrollView)mTextView.getParent()).getScrollY():0) +
                        mTextView.getCompoundPaddingTop()
        );
        textViewRect.top += parentTextViewTopAndBottomOffset;
        textViewRect.bottom += parentTextViewTopAndBottomOffset;
        textViewRect.left += (
                mTextView.getX() +
                        startXCoordinatesOfClickedText +
                        mTextView.getCompoundPaddingLeft() -
                        (mHasLockableScrollView?((DictScrollView)mTextView.getParent()).getScrollX():0)
        );
        textViewRect.right = (int) (
                textViewRect.left +
                        endXCoordinatesOfClickedText -
                        startXCoordinatesOfClickedText
        );
        return textViewRect;
    }

    private void removeExpandedPopup() {
        mPopupVisible = false;
        mTriggered = false;
        if (mDefineTask != null && ! mDefineTask.isCancelled()) {
            mDefineTask.cancel(true);
        }
        if(mHasLockableScrollView) {
            ((DictScrollView)mTextView.getParent()).setScrollingEnabled(true);
        }
        mLastWord = LAST_WORD_DEFAULT_VALUE;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(MAX_FLOAT_ANIMATOR_VALUE, MIN_FLOAT_ANIMATOR_VALUE);
        valueAnimator.setDuration(POPUP_EXPANSION_ANIMATION_TIME);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mPopupList.get(CURRENT_POPUP_LIST_INDEX).setAlpha((float)valueAnimator.getAnimatedValue());
                mBackgroundFilter.setAlpha((float)valueAnimator.getAnimatedValue());
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                mContainer.removeView(mBackgroundFilter);
                mContainer.removeView(mPopupList.get(CURRENT_POPUP_LIST_INDEX));
                mPopupList.remove(CURRENT_POPUP_LIST_INDEX);
                mPopupContent = null;
                mProgressBar = null;
                mBackgroundFilter = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        valueAnimator.start();
    }

    private void doLookup()
    {
        LinkedList<String> commands = new LinkedList<String>();
        commands.add("DEFINE * " + mLastWord);
        commands.add("QUIT");
        mDefineTask = new DefineTask(mHandler, DEFAULT_SERVER_ADDRESS, DEFAULT_SOCKET_PORT, commands);
        mDefineTask.execute();
    }

    private void removePopup() {
        mPopupVisible = false;
        mValueAnimator.cancel();
        final View v = mPopupList.get(CURRENT_POPUP_LIST_INDEX);
        mLastWord = LAST_WORD_DEFAULT_VALUE;
        Animation fadeOut = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        mContainer.removeView(v);
                        mPopupList.remove(v);
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        v.startAnimation(fadeOut);
    }

    private void displayDefinitions(DictParser dictParser)
    {
        final LinearLayout linearLayout = new LinearLayout(mContext);
        ScrollView.LayoutParams layoutParams = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setVisibility(View.INVISIBLE);
        mPopupContent.addView(linearLayout);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinkedList<String> definitions = dictParser.result();
        ListIterator<String> i = definitions.listIterator();
        while (i.hasNext()) {
            SpannableString dictionary = new SpannableString(i.next() + DictParser.NEWLINE);
            dictionary.setSpan(new StyleSpan(Typeface.BOLD), 0, dictionary.length(), 0);
            TextView dictionaryView = new TextView(mContext);
            dictionaryView.setLayoutParams(lp);
            dictionaryView.setText(dictionary);
            linearLayout.addView(dictionaryView);

            if (i.hasNext()) {
                TextView definitionView = new TextView(mContext);
                definitionView.setLayoutParams(lp);
                definitionView.setText(i.next());
                linearLayout.addView(definitionView);
            }
        }
        Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        fadeIn.setDuration(DISPLAY_RESULT_FADE_IN_ANIMATION_TIME);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                linearLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        linearLayout.startAnimation(fadeIn);
    }

    private void displayError(String message) {
        TextView textView = new TextView(mContext);
        ScrollView.LayoutParams layoutParams = new ScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        textView.setLayoutParams(layoutParams);
        mPopupContent.addView(textView);
        textView.setText(message);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case ACTION_DOWN :
                mPointerPosition = new PointF(motionEvent.getX(), motionEvent.getY());
                mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS_EVENT_MESSAGE_ID, POPUP_DISPLAY_COUNTDOWN);
                break;
            case  ACTION_MOVE:
                if(mPointerPosition != null) {
                    if (mPopupVisible) {
                        int offset = mTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY());
                        if(offset<mWordOffsetInterval[0] || offset>mWordOffsetInterval[1]) {
                            showProgress(motionEvent.getX(), motionEvent.getY());
                        }
                    } else if(Math.abs(mPointerPosition.x - motionEvent.getX())>SCROLL_THRESHOLD || Math.abs(mPointerPosition.y - motionEvent.getY())>SCROLL_THRESHOLD) {
                        mHandler.removeCallbacksAndMessages(null);
                        mPointerPosition = null;
                    }
                }
                break;
            case ACTION_UP :
                mHandler.removeCallbacksAndMessages(null);
                if(mPointerPosition != null) {
                    mPointerPosition = null;
                }
                if(!mTriggered && mPopupVisible) {
                    if(mHasLockableScrollView) {
                        ((DictScrollView)mTextView.getParent()).setScrollingEnabled(true);
                    }
                    removePopup();
                }
                break;
        }
        return true;
    }

    public boolean dismissPopup() {
        if(mPopupVisible && mTriggered) {
            removeExpandedPopup();
            return true;
        } else {
            return false;
        }
    }
}
