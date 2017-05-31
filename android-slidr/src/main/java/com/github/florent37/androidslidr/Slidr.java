package com.github.florent37.androidslidr;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by florentchampigny on 20/04/2017.
 */

public class Slidr extends FrameLayout {

    private static final float DISTANCE_TEXT_BAR = 20;
    private static final float BUBBLE_PADDING_HORIZONTAL = 35;
    private static final float BUBBLE_PADDING_VERTICAL = 20;

    private static final float BUBBLE_ARROW_HEIGHT = 20;
    private static final float BUBBLE_ARROW_WIDTH = 40;
    boolean moving = false;
    private Listener listener;
    private GestureDetectorCompat detector;
    private Settings settings;
    private float max = 1000;
    private float min = 0;
    private float currentValue = 0;
    private float oldValue = Float.MIN_VALUE;
    private List<Step> steps = new ArrayList<>();
    private float barY;
    private float barWidth;
    private float indicatorX;
    private int indicatorRadius;
    private float barCenterY;
    private Bubble bubble = new Bubble();
    private TextFormatter textFormatter = new EurosTextFormatter();
    private RegionTextFormatter regionTextFormatter = null;

    private String textMax = "";
    private String textMin = "";
    private int calculatedHieght = 0;
    private boolean isEditing = false;
    private String textEditing = "";
    private EditText editText;
    private EditListener editListener;

    public Slidr(Context context) {
        this(context, null);
    }

    public Slidr(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Slidr(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void onClick(MotionEvent e) {
        if (bubble.clicked(e)) {
            onBubbleClicked();
        }
    }

    private void closeEditText() {
        editText.clearFocus();

        removeView(editText);

        isEditing = false;
        if (TextUtils.isEmpty(textEditing)) {
            textEditing = String.valueOf(currentValue);
        }
        Float value;
        try {
            value = Float.valueOf(textEditing);
        } catch (Exception e) {
            e.printStackTrace();
            value = min;
        }


        value = Math.min(value, max);
        value = Math.max(value, min);
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(currentValue, value);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentValue(((float) animation.getAnimatedValue()));
                postInvalidate();
            }
        });
        valueAnimator.setInterpolator(new AccelerateInterpolator());
        valueAnimator.start();
        editText = null;
        postInvalidate();
    }

    private void editBubbleEditPosition() {
        if (isEditing) {
            editText.setX(Math.min(bubble.getX(), getWidth() - editText.getWidth()));
            editText.setY(bubble.getY());

            final ViewGroup.LayoutParams params = editText.getLayoutParams();
            params.width = (int) bubble.width;
            params.height = (int) bubble.getHeight();
            editText.setLayoutParams(params);

            editText.animate().alpha(1f);
        }
    }

    private void onBubbleClicked() {
        if (settings.editOnBubbleClick) {
            isEditing = true;
            editText = new EditText(getContext());
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            editText.setSelectAllOnFocus(true);

            editText.setSingleLine(true);
            editText.setGravity(Gravity.CENTER);
            //editText.setRawInputType(Configuration.KEYBOARD_12KEY);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);

            editText.setTextColor(settings.paintIndicator.getColor());
            editText.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            editText.setPadding(0, 0, 0, 0);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, dpToPx(settings.textSizeBubbleCurrent));

            textEditing = String.valueOf((int) currentValue);
            editText.setText(textEditing);

            final ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.width = (int) bubble.width;
            params.height = (int) bubble.getHeight();
            editText.setLayoutParams(params);

            editText.post(new Runnable() {
                @Override
                public void run() {
                    final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            addView(editText);
            editText.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    editBubbleEditPosition();
                    editText.getViewTreeObserver().removeOnPreDrawListener(this);
                    return false;
                }
            });


            editText.requestFocus();
            editText.requestFocusFromTouch();
            editBubbleEditPosition();

            if (editListener != null) {
                editListener.onEditStarted(editText);
            }

            editText.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                        closeEditText();
                        return true;
                    }
                    return false;
                }
            });

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    textEditing = editText.getText().toString();
                    updateBubbleWidth();
                    invalidate();
                    editBubbleEditPosition();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            postInvalidate();
        }
        if (listener != null) {
            listener.bubbleClicked(this);
        }
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);

        detector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            //some callbacks

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                onClick(e);
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onContextClick(MotionEvent e) {
                return super.onContextClick(e);
            }
        });

        this.settings = new Settings(this);
        this.settings.init(context, attrs);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    //region getters

    private float dpToPx(int size) {
        return size * getResources().getDisplayMetrics().density;
    }

    private float pxToDp(int size) {
        return size / getResources().getDisplayMetrics().density;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
        updateValues();
        update();
    }

    public void setMin(float min) {
        this.min = min;
        updateValues();
        update();
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(float value) {
        this.currentValue = value;
        updateValues();
        update();
    }

    public void setEditListener(EditListener editListener) {
        this.editListener = editListener;
    }

    public void addStep(List<Step> steps) {
        this.steps.addAll(steps);
        Collections.sort(steps);
        update();
    }

    //endregion

    public void addStep(Step step) {
        this.steps.add(step);
        Collections.sort(steps);
        update();
    }

    public void clearSteps() {
        this.steps.clear();
        update();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleTouch(event);
    }

    boolean handleTouch(MotionEvent event) {
        if (isEditing) {
            return false;
        }
        boolean handledByDetector = this.detector.onTouchEvent(event);
        if (!handledByDetector) {

            final int action = MotionEventCompat.getActionMasked(event);
            switch (action) {
                case ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    actionUp();
                    moving = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    final float evY = event.getY();
                    if (evY <= barY || evY >= (barY + barWidth)) {
                        return true;
                    } else {
                        moving = true;
                    }
                case MotionEvent.ACTION_MOVE: {
                    if (moving) {
                        float evX = event.getX();

                        evX = evX - settings.paddingCorners;
                        if (evX < 0) {
                            evX = 0;
                        }
                        if (evX > barWidth) {
                            evX = barWidth;
                        }
                        this.indicatorX = evX;

                        update();
                    }
                }
                break;
            }
        }

        return true;
    }

    void actionUp() {

    }

    public void update() {
        if (barWidth > 0f) {
            float currentPercent = indicatorX / barWidth;
            currentValue = currentPercent * (max - min) + min;

            if (listener != null && oldValue != currentValue) {
                oldValue = currentValue;
                listener.valueChanged(Slidr.this, currentValue);
            }

            updateBubbleWidth();
            editBubbleEditPosition();
        }
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateValues();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updateValues();
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(calculatedHieght, MeasureSpec.EXACTLY));
    }

    private void updateBubbleWidth() {
        this.bubble.width = calculateBubbleTextWidth() + BUBBLE_PADDING_HORIZONTAL * 2f;
        this.bubble.width = Math.max(150, this.bubble.width);
    }

    private boolean isRegions() {
        return settings.modeRegion || steps.isEmpty();
    }

    private void updateValues() {

        if (currentValue < min) {
            currentValue = min;
        }

        settings.paddingCorners = settings.barHeight;

        barWidth = getWidth() - this.settings.paddingCorners * 2;

        if (settings.drawBubble) {
            updateBubbleWidth();
            this.bubble.height = dpToPx(settings.textSizeBubbleCurrent) + BUBBLE_PADDING_VERTICAL * 2f + BUBBLE_ARROW_HEIGHT;
        } else {
            this.bubble.height = 0;
        }

        this.barY = 0;
        if (settings.drawTextOnTop) {
            barY += DISTANCE_TEXT_BAR;
            if (isRegions()) {
                float topTextHeight = 0;
                final String tmpTextLeft = formatRegionValue(0, 0);
                final String tmpTextRight = formatRegionValue(1, 0);
                topTextHeight = Math.max(topTextHeight, calculateTextMultilineHeight(tmpTextLeft, settings.paintTextTop));
                topTextHeight = Math.max(topTextHeight, calculateTextMultilineHeight(tmpTextRight, settings.paintTextTop));

                this.barY += topTextHeight + 3;
            } else {
                float topTextHeight = 0;

                for (Step step : steps) {
                    topTextHeight = Math.max(
                            topTextHeight,
                            calculateTextMultilineHeight(formatValue(step.value), settings.paintTextBottom)
                    );
                }
                this.barY += topTextHeight;
            }
        } else {
            if (settings.drawBubble) {
                this.barY -= BUBBLE_ARROW_HEIGHT / 1.5f;
            }
        }

        this.barY += bubble.height;

        this.barCenterY = barY + settings.barHeight / 2f;

        if (settings.indicatorInside) {
            this.indicatorRadius = (int) (settings.barHeight * .5f);
        } else {
            this.indicatorRadius = (int) (settings.barHeight * .9f);
        }

        for (Step step : steps) {
            final float stoppoverPercent = step.value / (max - min);
            step.xStart = stoppoverPercent * barWidth;
        }

        indicatorX = (currentValue - min) / (max - min) * barWidth;

        calculatedHieght = (int) (barCenterY + indicatorRadius);

        float bottomTextHeight = 0;
        if (!TextUtils.isEmpty(textMax)) {
            bottomTextHeight = Math.max(
                    calculateTextMultilineHeight(textMax, settings.paintTextBottom),
                    calculateTextMultilineHeight(textMin, settings.paintTextBottom)
            );
        }
        for (Step step : steps) {
            bottomTextHeight = Math.max(
                    bottomTextHeight,
                    calculateTextMultilineHeight(step.name, settings.paintTextBottom)
            );
        }

        calculatedHieght += bottomTextHeight;

        calculatedHieght += 10; //padding bottom

    }

    private Step findStepBeforeCustor() {
        for (int i = steps.size() - 1; i >= 0; i--) {
            final Step step = steps.get(i);
            if ((currentValue - min) >= step.value) {
                return step;
            }
            break;
        }
        return null;
    }

    private Step findStepOfCustor() {
        for (int i = 0; i < steps.size(); ++i) {
            final Step step = steps.get(i);
            if ((currentValue - min) <= step.value) {
                return step;
            }
            break;
        }
        return null;
    }

    public void setTextMax(String textMax) {
        this.textMax = textMax;
        postInvalidate();
    }

    public void setTextMin(String textMin) {
        this.textMin = textMin;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        {

            final float paddingLeft = settings.paddingCorners;
            final float paddingRight = settings.paddingCorners;


            if (isRegions()) {
                if (steps.isEmpty()) {
                    settings.paintIndicator.setColor(settings.regionColorLeft);
                    settings.paintBubble.setColor(settings.regionColorLeft);
                } else {
                    settings.paintIndicator.setColor(settings.regionColorRight);
                    settings.paintBubble.setColor(settings.regionColorRight);
                }
            } else {
                final Step stepBeforeCustor = findStepOfCustor();
                if (stepBeforeCustor != null) {
                    settings.paintIndicator.setColor(stepBeforeCustor.colorBefore);
                    settings.paintBubble.setColor(stepBeforeCustor.colorBefore);
                } else {
                    if (settings.step_colorizeAfterLast) {
                        final Step beforeCustor = findStepBeforeCustor();
                        if (beforeCustor != null) {
                            settings.paintIndicator.setColor(beforeCustor.colorAfter);
                            settings.paintBubble.setColor(beforeCustor.colorAfter);
                        }
                    } else {
                        settings.paintIndicator.setColor(settings.colorBackground);
                        settings.paintBubble.setColor(settings.colorBackground);
                    }
                }
            }

            final float radiusCorner = settings.barHeight / 2f;

            final float indicatorCenterX = indicatorX + paddingLeft;

            { //background
                final float centerCircleLeft = paddingLeft;
                final float centerCircleRight = getWidth() - paddingRight;

                //grey background
                if (isRegions()) {
                    if (steps.isEmpty()) {
                        settings.paintBar.setColor(settings.colorBackground);
                    } else {
                        settings.paintBar.setColor(settings.regionColorRight);
                    }
                } else {
                    settings.paintBar.setColor(settings.colorBackground);
                }
                canvas.drawCircle(centerCircleLeft, barCenterY, radiusCorner, settings.paintBar);
                canvas.drawCircle(centerCircleRight, barCenterY, radiusCorner, settings.paintBar);
                canvas.drawRect(centerCircleLeft, barY, centerCircleRight, barY + settings.barHeight, settings.paintBar);

                if (isRegions()) {
                    settings.paintBar.setColor(settings.regionColorLeft);

                    canvas.drawCircle(centerCircleLeft, barCenterY, radiusCorner, settings.paintBar);
                    canvas.drawRect(centerCircleLeft, barY, indicatorCenterX, barY + settings.barHeight, settings.paintBar);
                } else {
                    float lastX = centerCircleLeft;
                    boolean first = true;
                    for (Step step : steps) {
                        settings.paintBar.setColor(step.colorBefore);
                        if (first) {
                            canvas.drawCircle(centerCircleLeft, barCenterY, radiusCorner, settings.paintBar);
                        }

                        final float x = step.xStart + paddingLeft;
                        if (!settings.step_colorizeOnlyBeforeIndicator) {
                            canvas.drawRect(lastX, barY, x, barY + settings.barHeight, settings.paintBar);
                        } else {
                            canvas.drawRect(lastX, barY, Math.min(x, indicatorCenterX), barY + settings.barHeight, settings.paintBar);
                        }
                        lastX = x;

                        first = false;
                    }


                    if (settings.step_colorizeAfterLast) {
                        //find the step just below currentValue
                        for (int i = steps.size() - 1; i >= 0; i--) {
                            final Step step = steps.get(i);
                            if ((currentValue - min) > step.value) {
                                settings.paintBar.setColor(step.colorAfter);
                                canvas.drawRect(step.xStart + paddingLeft, barY, indicatorCenterX, barY + settings.barHeight, settings.paintBar);
                                break;
                            }
                        }
                    }
                }
            }


            { //texts top (values)
                if (settings.drawTextOnTop) {
                    final float textY = barY - DISTANCE_TEXT_BAR;
                    if (isRegions()) {
                        float leftValue;
                        float rightValue;

                        if (settings.regions_centerText) {
                            leftValue = currentValue;
                            rightValue = max - leftValue;
                        } else {
                            leftValue = min;
                            rightValue = max;
                        }

                        if (settings.regions_textFollowRegionColor) {
                            settings.paintTextTop.setColor(settings.regionColorLeft);
                        }

                        float textX;
                        if (settings.regions_centerText) {
                            textX = (indicatorCenterX - paddingLeft) / 2f + paddingLeft;
                        } else {
                            textX = paddingLeft;
                        }

                        drawIndicatorsTextAbove(canvas, formatRegionValue(0, leftValue), settings.paintTextTop, textX, textY, Layout.Alignment.ALIGN_CENTER);

                        if (settings.regions_textFollowRegionColor) {
                            settings.paintTextTop.setColor(settings.regionColorRight);
                        }

                        if (settings.regions_centerText) {
                            textX = indicatorCenterX + (barWidth - indicatorCenterX - paddingLeft) / 2f + paddingLeft;
                        } else {
                            textX = paddingLeft + barWidth;
                        }
                        drawIndicatorsTextAbove(canvas, formatRegionValue(1, rightValue), settings.paintTextTop, textX, textY, Layout.Alignment.ALIGN_CENTER);
                    } else {
                        drawIndicatorsTextAbove(canvas, formatValue(min), settings.paintTextTop, 0 + paddingLeft, textY, Layout.Alignment.ALIGN_CENTER);
                        for (Step step : steps) {
                            drawIndicatorsTextAbove(canvas, formatValue(step.value), settings.paintTextTop, step.xStart + paddingLeft, textY, Layout.Alignment.ALIGN_CENTER);
                        }
                        drawIndicatorsTextAbove(canvas, formatValue(max), settings.paintTextTop, canvas.getWidth(), textY, Layout.Alignment.ALIGN_CENTER);
                    }
                }
            }


            { //steps + bottom text
                final float bottomTextY = barY + settings.barHeight + 15;

                for (Step step : steps) {
                    if (settings.step_drawLines) {
                        canvas.drawLine(step.xStart + paddingLeft, barY - settings.barHeight / 4f, step.xStart + paddingLeft, barY + settings.barHeight + settings.barHeight / 4f, settings.paintStep);
                    }

                    if (settings.drawTextOnBottom) {
                        //drawMultilineText(canvas, maxText, canvas.getWidth() - settings.paintText.measureText(maxText), textY, settings.paintText, Layout.Alignment.ALIGN_OPPOSITE);
                        drawMultilineText(canvas, step.name, step.xStart + paddingLeft, bottomTextY, settings.paintTextBottom, Layout.Alignment.ALIGN_CENTER);
                    }
                }

                if (settings.drawTextOnBottom) {
                    if (!TextUtils.isEmpty(textMax)) {
                        drawMultilineText(canvas, textMax, canvas.getWidth(), bottomTextY, settings.paintTextBottom, Layout.Alignment.ALIGN_CENTER);
                    }

                    if (!TextUtils.isEmpty(textMin)) {
                        drawMultilineText(canvas, textMin, 0, bottomTextY, settings.paintTextBottom, Layout.Alignment.ALIGN_CENTER);
                    }
                }
            }

            //indicator
            {
                final int color = settings.paintIndicator.getColor();
                canvas.drawCircle(indicatorCenterX, this.barCenterY, indicatorRadius, settings.paintIndicator);
                settings.paintIndicator.setColor(Color.WHITE);
                canvas.drawCircle(indicatorCenterX, this.barCenterY, indicatorRadius * 0.85f, settings.paintIndicator);
                settings.paintIndicator.setColor(color);
            }

            //bubble
            {
                if (settings.drawBubble) {
                    float bubbleCenterX = indicatorCenterX;
                    float trangleCenterX;

                    bubble.x = bubbleCenterX - bubble.width / 2f;
                    bubble.y = 0;

                    if (bubbleCenterX > canvas.getWidth() - bubble.width / 2f) {
                        bubbleCenterX = canvas.getWidth() - bubble.width / 2f;
                    } else if (bubbleCenterX - bubble.width / 2f < 0) {
                        bubbleCenterX = bubble.width / 2f;
                    }

                    trangleCenterX = (bubbleCenterX + indicatorCenterX) / 2f;

                    drawBubble(canvas, bubbleCenterX, trangleCenterX, 0);
                }
            }
        }

        canvas.restore();
    }

    private String formatValue(float value) {
        return textFormatter.format(value);
    }

    private String formatRegionValue(int region, float value) {
        if (regionTextFormatter != null) {
            return regionTextFormatter.format(region, value);
        } else {
            return formatValue(value);
        }
    }

    private void drawText(Canvas canvas, String text, float x, float y, TextPaint paint, Layout.Alignment aligment) {
        canvas.save();
        {
            canvas.translate(x, y);
            final StaticLayout staticLayout = new StaticLayout(text, paint, (int) paint.measureText(text), aligment, 1.0f, 0, false);
            staticLayout.draw(canvas);
        }
        canvas.restore();
    }

    private void drawMultilineText(Canvas canvas, String text, float x, float y, TextPaint paint, Layout.Alignment aligment) {
        final float lineHeight = paint.getTextSize();
        float lineY = y;
        for (CharSequence line : text.split("\n")) {
            canvas.save();
            {
                final float lineWidth = (int) paint.measureText(line.toString());
                float lineX = x;
                if (aligment == Layout.Alignment.ALIGN_CENTER) {
                    lineX -= lineWidth / 2f;
                }
                if (lineX < 0) {
                    lineX = 0;
                }

                final float right = lineX + lineWidth;
                if (right > canvas.getWidth()) {
                    lineX = canvas.getWidth() - lineWidth - settings.paddingCorners;
                }

                canvas.translate(lineX, lineY);
                final StaticLayout staticLayout = new StaticLayout(line, paint, (int) lineWidth, aligment, 1.0f, 0, false);
                staticLayout.draw(canvas);

                lineY += lineHeight;
            }
            canvas.restore();
        }

    }

    private void drawIndicatorsTextAbove(Canvas canvas, String text, TextPaint paintText, float x, float y, Layout.Alignment alignment) {

        final float textHeight = calculateTextMultilineHeight(text, paintText);
        y -= textHeight;

        final int width = (int) paintText.measureText(text);
        if (x >= getWidth() - settings.paddingCorners) {
            x = (getWidth() - width - settings.paddingCorners / 2f);
        } else if (x <= 0) {
            x = width / 2f;
        } else {
            x = (x - width / 2f);
        }

        if (x < 0) {
            x = 0;
        }

        if (x + width > getWidth()) {
            x = getWidth() - width;
        }

        drawText(canvas, text, x, y, paintText, alignment);
    }

    private float calculateTextMultilineHeight(String text, TextPaint textPaint) {
        return text.split("\n").length * textPaint.getTextSize();
    }

    /*
    private float calculateTextMultilineWidth(String text, TextPaint textPaint) {
        int maxLength = -1;
        CharSequence max = null;
        for (CharSequence line : text.split("\n")) {
            final int lineLength = line.length();
            if (lineLength > maxLength) {
                maxLength = lineLength;
                max = line;
            }
        }
        return textPaint.measureText(max.toString());
    }
    */

    private float calculateBubbleTextWidth() {
        String bubbleText = formatValue(getCurrentValue());
        if (isEditing) {
            bubbleText = textEditing;
        }
        return settings.paintBubbleTextCurrent.measureText(bubbleText);
    }

    private void drawBubblePath(Canvas canvas, float triangleCenterX, float height, float width) {
        final Path path = new Path();

        int padding = 3;
        final Rect rect = new Rect(padding, padding, (int) width - padding, (int) (height - BUBBLE_ARROW_HEIGHT) - padding);

        final float roundRectHeight = (height - BUBBLE_ARROW_HEIGHT) / 2;

        path.moveTo(rect.left + roundRectHeight, rect.top);
        path.lineTo(rect.right - roundRectHeight, rect.top);
        path.quadTo(rect.right, rect.top, rect.right, rect.top + roundRectHeight);
        path.lineTo(rect.right, rect.bottom - roundRectHeight);
        path.quadTo(rect.right, rect.bottom, rect.right - roundRectHeight, rect.bottom);

        path.lineTo(triangleCenterX + BUBBLE_ARROW_WIDTH / 2f, height - BUBBLE_ARROW_HEIGHT - padding);
        path.lineTo(triangleCenterX, height - padding);
        path.lineTo(triangleCenterX - BUBBLE_ARROW_WIDTH / 2f, height - BUBBLE_ARROW_HEIGHT - padding);

        path.lineTo(rect.left + roundRectHeight, rect.bottom);
        path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - roundRectHeight);
        path.lineTo(rect.left, rect.top + roundRectHeight);
        path.quadTo(rect.left, rect.top, rect.left + roundRectHeight, rect.top);
        path.close();

        canvas.drawPath(path, settings.paintBubble);
    }

    private void drawBubble(Canvas canvas, float centerX, float triangleCenterX, float y) {
        final float width = this.bubble.width;
        final float height = this.bubble.height;

        canvas.save();
        {
            canvas.translate(centerX - width / 2f, y);
            triangleCenterX -= (centerX - width / 2f);

            if (!isEditing) {
                drawBubblePath(canvas, triangleCenterX, height, width);
            } else {
                final int savedColor = settings.paintBubble.getColor();

                settings.paintBubble.setColor(settings.bubbleColorEditing);
                settings.paintBubble.setStyle(Paint.Style.FILL);
                drawBubblePath(canvas, triangleCenterX, height, width);

                settings.paintBubble.setStyle(Paint.Style.STROKE);
                settings.paintBubble.setColor(settings.paintIndicator.getColor());
                drawBubblePath(canvas, triangleCenterX, height, width);

                settings.paintBubble.setStyle(Paint.Style.FILL);
                settings.paintBubble.setColor(savedColor);
            }

            if (!isEditing) {
                final String bubbleText = formatValue(getCurrentValue());
                drawText(canvas, bubbleText, BUBBLE_PADDING_HORIZONTAL, BUBBLE_PADDING_VERTICAL - 3, settings.paintBubbleTextCurrent, Layout.Alignment.ALIGN_NORMAL);
            }
        }

        canvas.restore();

    }

    public void setTextFormatter(TextFormatter textFormatter) {
        this.textFormatter = textFormatter;
        update();
    }

    public void setRegionTextFormatter(RegionTextFormatter regionTextFormatter) {
        this.regionTextFormatter = regionTextFormatter;
        update();
    }

    public interface EditListener {
        void onEditStarted(EditText editText);
    }

    public interface Listener {
        void valueChanged(Slidr slidr, float currentValue);

        void bubbleClicked(Slidr slidr);
    }

    public interface TextFormatter {
        String format(float value);
    }

    public interface RegionTextFormatter {
        String format(int region, float value);
    }

    public static class Step implements Comparable<Step> {
        private String name;
        private float value;

        private float xStart;
        private int colorBefore;
        private int colorAfter = Color.parseColor("#ed5564");

        public Step(String name, float value, int colorBefore) {
            this.name = name;
            this.value = value;
            this.colorBefore = colorBefore;
        }

        public Step(String name, float value, int colorBefore, int colorAfter) {
            this(name, value, colorBefore);
            this.colorAfter = colorAfter;
        }

        @Override
        public int compareTo(@NonNull Step o) {
            return Float.compare(value, o.value);
        }
    }

    public static class Settings {
        private Slidr slidr;
        private Paint paintBar;
        private Paint paintIndicator;
        private Paint paintStep;
        private TextPaint paintTextTop;
        private TextPaint paintTextBottom;
        private TextPaint paintBubbleTextCurrent;
        private Paint paintBubble;
        private int colorBackground = Color.parseColor("#cccccc");
        private int colorStoppover = Color.BLACK;
        private int textColor = Color.parseColor("#6E6E6E");
        private int textTopSize = 12;
        private int textBottomSize = 12;
        private int textSizeBubbleCurrent = 16;
        private float barHeight = 35;
        private float paddingCorners;
        private boolean step_colorizeAfterLast = false;
        private boolean step_drawLines = true;
        private boolean step_colorizeOnlyBeforeIndicator = true;
        private boolean drawTextOnTop = true;
        private boolean drawTextOnBottom = true;
        private boolean drawBubble = true;
        private boolean modeRegion = false;
        private boolean indicatorInside = false;
        private boolean regions_textFollowRegionColor = false;
        private boolean regions_centerText = true;
        private int regionColorLeft = Color.parseColor("#007E90");
        private int regionColorRight = Color.parseColor("#ed5564");
        private boolean editOnBubbleClick = true;
        private int bubbleColorEditing = Color.WHITE;

        public Settings(Slidr slidr) {
            this.slidr = slidr;

            paintIndicator = new Paint();
            paintIndicator.setAntiAlias(true);
            paintIndicator.setStrokeWidth(2);

            paintBar = new Paint();
            paintBar.setAntiAlias(true);
            paintBar.setStrokeWidth(2);
            paintBar.setColor(colorBackground);

            paintStep = new Paint();
            paintStep.setAntiAlias(true);
            paintStep.setStrokeWidth(5);
            paintStep.setColor(colorStoppover);

            paintTextTop = new TextPaint();
            paintTextTop.setAntiAlias(true);
            paintTextTop.setStyle(Paint.Style.FILL);
            paintTextTop.setColor(textColor);
            paintTextTop.setTextSize(textTopSize);

            paintTextBottom = new TextPaint();
            paintTextBottom.setAntiAlias(true);
            paintTextBottom.setStyle(Paint.Style.FILL);
            paintTextBottom.setColor(textColor);
            paintTextBottom.setTextSize(textBottomSize);

            paintBubbleTextCurrent = new TextPaint();
            paintBubbleTextCurrent.setAntiAlias(true);
            paintBubbleTextCurrent.setStyle(Paint.Style.FILL);
            paintBubbleTextCurrent.setColor(Color.WHITE);
            paintBubbleTextCurrent.setStrokeWidth(2);
            paintBubbleTextCurrent.setTextSize(dpToPx(textSizeBubbleCurrent));

            paintBubble = new Paint();
            paintBubble.setAntiAlias(true);
            paintBubble.setStrokeWidth(3);
        }

        private void init(Context context, AttributeSet attrs) {
            if (attrs != null) {
                final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Slidr);
                setColorBackground(a.getColor(R.styleable.Slidr_slidr_backgroundColor, colorBackground));

                this.step_colorizeAfterLast = a.getBoolean(R.styleable.Slidr_slidr_step_colorizeAfterLast, step_colorizeAfterLast);
                this.step_drawLines = a.getBoolean(R.styleable.Slidr_slidr_step_drawLine, step_drawLines);
                this.step_colorizeOnlyBeforeIndicator = a.getBoolean(R.styleable.Slidr_slidr_step_colorizeOnlyBeforeIndicator, step_colorizeOnlyBeforeIndicator);

                this.drawTextOnTop = a.getBoolean(R.styleable.Slidr_slidr_textTop_visible, drawTextOnTop);
                setTextTopSize(a.getDimensionPixelSize(R.styleable.Slidr_slidr_textTop_size, (int) dpToPx(textTopSize)));
                this.drawTextOnBottom = a.getBoolean(R.styleable.Slidr_slidr_textBottom_visible, drawTextOnBottom);
                setTextBottomSize(a.getDimensionPixelSize(R.styleable.Slidr_slidr_textBottom_size, (int) dpToPx(textBottomSize)));

                this.barHeight = a.getDimensionPixelOffset(R.styleable.Slidr_slidr_barHeight, (int) barHeight);
                this.drawBubble = a.getBoolean(R.styleable.Slidr_slidr_draw_bubble, drawBubble);
                this.modeRegion = a.getBoolean(R.styleable.Slidr_slidr_regions, modeRegion);

                this.regionColorLeft = a.getColor(R.styleable.Slidr_slidr_region_leftColor, regionColorLeft);
                this.regionColorRight = a.getColor(R.styleable.Slidr_slidr_region_rightColor, regionColorRight);

                this.indicatorInside = a.getBoolean(R.styleable.Slidr_slidr_indicator_inside, indicatorInside);
                this.regions_textFollowRegionColor = a.getBoolean(R.styleable.Slidr_slidr_regions_textFollowRegionColor, regions_textFollowRegionColor);
                this.regions_centerText = a.getBoolean(R.styleable.Slidr_slidr_regions_centerText, regions_centerText);

                this.editOnBubbleClick = a.getBoolean(R.styleable.Slidr_slidr_edditable, editOnBubbleClick);

                a.recycle();
            }
        }

        public void setStep_colorizeAfterLast(boolean step_colorizeAfterLast) {
            this.step_colorizeAfterLast = step_colorizeAfterLast;
            slidr.update();
        }

        public void setDrawTextOnTop(boolean drawTextOnTop) {
            this.drawTextOnTop = drawTextOnTop;
            slidr.update();
        }

        public void setDrawTextOnBottom(boolean drawTextOnBottom) {
            this.drawTextOnBottom = drawTextOnBottom;
            slidr.update();
        }

        public void setDrawBubble(boolean drawBubble) {
            this.drawBubble = drawBubble;
            slidr.update();
        }

        public void setModeRegion(boolean modeRegion) {
            this.modeRegion = modeRegion;
            slidr.update();
        }

        public void setRegionColorLeft(int regionColorLeft) {
            this.regionColorLeft = regionColorLeft;
            slidr.update();
        }

        public void setRegionColorRight(int regionColorRight) {
            this.regionColorRight = regionColorRight;
            slidr.update();
        }

        public void setColorBackground(int colorBackground) {
            this.colorBackground = colorBackground;
            slidr.update();
        }

        public void setTextTopSize(int textSize) {
            this.textTopSize = textSize;
            this.paintTextTop.setTextSize(textSize);
            slidr.update();
        }

        public void setTextBottomSize(int textSize) {
            this.textBottomSize = textSize;
            this.paintTextBottom.setTextSize(textSize);
            slidr.update();
        }

        private float dpToPx(int size) {
            return size * slidr.getResources().getDisplayMetrics().density;
        }

    }

    private static class TouchView extends FrameLayout {

        private final Rect viewRect;
        private final Callback callback;

        public TouchView(Context context, Rect viewRect, Callback callback) {
            super(context);
            this.viewRect = viewRect;
            this.setBackgroundColor(Color.TRANSPARENT);
            this.callback = callback;
        }

        public void add(View view) {
            addView(view);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            if (x >= viewRect.left
                    && x <= viewRect.right
                    && y >= viewRect.top
                    && y <= viewRect.bottom) {
                return false;
                //return compteurChampEditable.onTouchEvent(event);
            } else if (event.getAction() == ACTION_UP) {
                //((ViewGroup) getParent()).removeView(this);
                callback.onClicked();
            }
            return true;
        }

        public interface Callback {
            void onClicked();
        }
    }

    private class Bubble {
        private float height;
        private float width;
        private float x;
        private float y;

        public boolean clicked(MotionEvent e) {
            return e.getX() >= x && e.getX() <= x + width
                    && e.getY() >= y && e.getY() < y + height;
        }

        public float getHeight() {
            return height - BUBBLE_ARROW_HEIGHT;
        }

        public float getX() {
            return Math.max(x, 0);
        }

        public float getY() {
            return Math.max(y, 0);
        }
    }

    public class EurosTextFormatter implements TextFormatter {

        @Override
        public String format(float value) {
            return String.format("%d €", (int) value);
        }
    }
}
