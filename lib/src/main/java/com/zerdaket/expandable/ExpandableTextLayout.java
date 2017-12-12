package com.zerdaket.expandable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by zerdaket on 2017/12/7.
 */

public class ExpandableTextLayout extends LinearLayout {

    private final static int DEFAULT_COLLAPSED_LINES = 3;
    private final static int DEFAULT_ANIMATION_DURATION = 200;

    private final FrameLayout mContentLayout;
    private final RelativeLayout mToggleLayout;

    private TextView mContentTextView;

    private TextView mToggleTextView;
    private CharSequence mExpandedText;
    private CharSequence mCollapsedText;

    private ValueAnimator mExpandedAnimator;
    private ValueAnimator mCollapsedAnimator;
    private ValueAnimator mValueAnimator;
    private int[] mExpandedValueRange = {0, 0};
    private int[] mCollapsedValueRange = {0, 0};

    private int mAnimationDuration;
    private int mCollapsedLines;
    private int mContentRealHeight;
    private int mContentCollapsedHeight;
    private int mLayoutHeightWithoutContent;

    private boolean mIsCollapsed;

    public ExpandableTextLayout(Context context) {
        this(context, null);
    }

    public ExpandableTextLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableTextLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextLayout);

        mCollapsedLines = ta.getInteger(R.styleable.ExpandableTextLayout_collapsedLines, DEFAULT_COLLAPSED_LINES);
        mAnimationDuration = ta.getInteger(R.styleable.ExpandableTextLayout_duration, DEFAULT_ANIMATION_DURATION);
        mExpandedText = ta.getString(R.styleable.ExpandableTextLayout_expandedText);
        mCollapsedText = ta.getString(R.styleable.ExpandableTextLayout_collapsedText);

        ta.recycle();

        if (TextUtils.isEmpty(mExpandedText)) {
            mExpandedText = getContext().getString(R.string.expanded_text);
        }
        if (TextUtils.isEmpty(mCollapsedText)) {
            mCollapsedText = getContext().getString(R.string.collapsed_text);
        }

        mIsCollapsed = true;

        mContentLayout = new FrameLayout(getContext());
        mContentLayout.setAddStatesFromChildren(true);
        addView(mContentLayout, 0);

        mToggleLayout = new RelativeLayout(getContext());
        mToggleLayout.setAddStatesFromChildren(true);
        LayoutParams toggleParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        toggleParams.gravity = Gravity.END;
        mToggleLayout.setLayoutParams(toggleParams);

        mToggleTextView = new TextView(getContext());
        LayoutParams toggleTextParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        toggleTextParams.gravity = Gravity.CENTER_HORIZONTAL;
        mToggleTextView.setLayoutParams(toggleTextParams);
        mToggleLayout.addView(mToggleTextView);

        mToggleLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                clickToggle();
            }
        });
        //add mToggleLayout at the bottom of ExpandableTextLayout.
        addView(mToggleLayout, -1);
    }

    private void clickToggle() {
        if (mIsCollapsed) {
            mValueAnimator =
                    getExpandedAnimator(mContentLayout.getHeight(), mContentRealHeight);
            mToggleTextView.setText(mCollapsedText);
        } else {
            mValueAnimator =
                    getCollapsedAnimator(mContentLayout.getHeight(), mContentCollapsedHeight);
            mToggleTextView.setText(mExpandedText);
        }
        mValueAnimator.setDuration(mAnimationDuration);
        mValueAnimator.start();
    }

    @Override
    public void setOrientation(int orientation) {
        if (orientation == LinearLayout.HORIZONTAL) {
            throw new IllegalArgumentException("ExpandableTextLayout only supports Vertical Orientation.");
        }
        super.setOrientation(orientation);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof TextView) {
            TextView textView = (TextView) child;
            setTextView(textView);
            mContentLayout.addView(textView);
            // Now use the TextView's LayoutParams as our own.
            mContentLayout.setLayoutParams(params);
        } else {
            super.addView(child, index, params);
        }
    }

    private void setTextView(TextView textView) {
        if (mContentTextView != null) {
            throw new IllegalArgumentException("We already have an TextView, can only have one.");
        }
        mContentTextView = textView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!isAnimatorRunning()) {
            updateLayout();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void updateLayout() {
        if (mContentTextView == null || mContentTextView.getLineCount() <= mCollapsedLines) {
            mToggleLayout.setVisibility(GONE);
            return;
        }
        if (mIsCollapsed) {
            mContentTextView.setMaxLines(mCollapsedLines);
            mToggleTextView.setText(mExpandedText);
        } else {
            mContentTextView.setMaxLines(Integer.MAX_VALUE);
            mToggleTextView.setText(mCollapsedText);
        }
        mToggleLayout.setVisibility(VISIBLE);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isAnimatorRunning()) {
            return;
        }
        mLayoutHeightWithoutContent = getHeight() - mContentLayout.getHeight();
        mContentRealHeight = getTextLineHeight(mContentTextView);
        mContentCollapsedHeight = getTextLineHeight(mContentTextView, mCollapsedLines);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isAnimatorRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
    }

    static class SavedState extends BaseSavedState {

        boolean isCollapsed;

        SavedState(Parcel source) {
            super(source);
            isCollapsed = (source.readInt() == 1);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isCollapsed ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isCollapsed = mIsCollapsed;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mIsCollapsed = ss.isCollapsed;
        requestLayout();
    }

    private boolean isAnimatorRunning() {
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    private int getTextLineHeight(@NonNull TextView textView) {
        return getTextLineHeight(textView, textView.getLineCount());
    }

    private int getTextLineHeight(@NonNull TextView textView, int lines) {
        if (lines < 0 || lines > textView.getLineCount()) {
            throw new IndexOutOfBoundsException("CollapsedLines is out of range.");
        }
        int textHeight = textView.getLayout().getLineTop(lines);
        int padding = textView.getCompoundPaddingTop() + textView.getCompoundPaddingBottom();
        return textHeight + padding;
    }

    private ValueAnimator getExpandedAnimator(int startValue, int endValue) {
        if (mExpandedAnimator != null
                && mExpandedValueRange[0] == startValue
                && mExpandedValueRange[1] == endValue) {
            return mExpandedAnimator;
        }
        mExpandedAnimator = ValueAnimator.ofInt(startValue, endValue);
        setupAnimatorListener(mExpandedAnimator);
        mExpandedValueRange[0] = startValue;
        mExpandedValueRange[1] = endValue;
        return mExpandedAnimator;
    }

    private ValueAnimator getCollapsedAnimator(int startValue, int endValue) {
        if (mCollapsedAnimator != null
                && mCollapsedValueRange[0] == startValue
                && mCollapsedValueRange[1] == endValue) {
            return mCollapsedAnimator;
        }
        mCollapsedAnimator = ValueAnimator.ofInt(startValue, endValue);
        setupAnimatorListener(mCollapsedAnimator);
        mCollapsedValueRange[0] = startValue;
        mCollapsedValueRange[1] = endValue;
        return mCollapsedAnimator;
    }

    private void setupAnimatorListener(@NonNull ValueAnimator valueAnimator) {
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = (int) valueAnimator.getAnimatedValue();
                mContentTextView.setMaxHeight(value);
                getLayoutParams().height = value + mLayoutHeightWithoutContent;
                requestLayout();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsCollapsed = !mIsCollapsed;
            }
        });
    }

    public boolean isCollapsed() {
        return mIsCollapsed;
    }

    public CharSequence getExpandedText() {
        return mExpandedText;
    }

    public void setExpandedText(CharSequence expandedText) {
        mExpandedText = expandedText;
    }

    public CharSequence getCollapsedText() {
        return mCollapsedText;
    }

    public void setCollapsedText(CharSequence collapsedText) {
        mCollapsedText = collapsedText;
    }

    public int getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        mAnimationDuration = animationDuration;
    }

    public int getCollapsedLines() {
        return mCollapsedLines;
    }

    public void setCollapsedLines(int collapsedLines) {
        mCollapsedLines = collapsedLines;
    }
}
