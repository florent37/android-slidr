package com.github.florent37.slidr;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by florentchampigny on 20/04/2017.
 */

public class Slidr extends View {

    final int BUBBLE_PADDING_HORIZONTAL = 35;
    final int BUBBLE_PADDING_VERTICAL = 15;

    final int BUBBLE_ARROW_HEIGHT = 20;
    final int BUBBLE_ARROW_WIDTH = 40;

    final int PADDING_STOPOVER_TEXT = 15;
    private Listener listener;
    private GestureDetectorCompat detector;
    private Settings settings;
    private int max = 1000;
    private float currentValue = 0;
    private List<Step> steps = new ArrayList<>();
    private float barY;
    private float barWidth;
    private float indicatorX;
    private int indicatorRadius;
    private float barCenterY;
    private float bubbleHeight;
    private float bubbleWidth;

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

    private void init(Context context, @Nullable AttributeSet attrs) {
        detector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            //some callbacks
        });

        this.settings = new Settings(this);
        this.settings.init(context, attrs);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private float dpToPx(int size) {
        return size * getResources().getDisplayMetrics().density;
    }

    public int getMax() {
        return max;
    }

    //region getters

    public void setMax(int max) {
        this.max = max;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public void addStep(List<Step> steps) {
        this.steps.addAll(steps);
        Collections.sort(steps);
        update();
    }

    public void addStep(Step step) {
        this.steps.add(step);
        Collections.sort(steps);
        update();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleTouch(event);
    }

    boolean handleTouch(MotionEvent event) {
        boolean handledByDetector = this.detector.onTouchEvent(event);
        if (!handledByDetector) {

            final int action = MotionEventCompat.getActionMasked(event);
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    actionUp();
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE: {

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
                break;
            }
        }

        return true;
    }

    //endregion

    void actionUp() {

    }

    public void update() {
        if (barWidth > 0f) {
            float currentPercent = indicatorX / barWidth;
            currentValue = currentPercent * max;
            //Log.d("xStart", currentPercent + " " + currentValue);
            updateBubbleWidth();

            if (listener != null) {
                listener.valueChanged(this, currentValue);
            }
        }
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateValues();
    }

    private void updateBubbleWidth() {
        this.bubbleWidth = calculateBubbleTextWidth() + BUBBLE_PADDING_HORIZONTAL * 2f;
    }

    private void updateValues() {
        barWidth = getWidth() - this.settings.paddingCorners * 2;

        updateBubbleWidth();
        this.bubbleHeight = dpToPx(settings.textSizeBubbleCurrent) + BUBBLE_PADDING_VERTICAL * 2f + BUBBLE_ARROW_HEIGHT;

        if (settings.drawTextOnTop) {
            final float spaceBetweenBubbleAndBar = 50;
            this.barY = bubbleHeight + spaceBetweenBubbleAndBar + (settings.barHeight) / 2f;
        } else {
            this.barY = bubbleHeight;
        }


        this.barCenterY = barY + settings.barHeight / 2f;

        this.indicatorRadius = (int) (settings.barHeight * .9f);

        for (Step step : steps) {
            final float stoppoverPercent = step.value / max;
            step.xStart = stoppoverPercent * barWidth;
        }
    }

    private Step findStepBeforeCustor() {
        for (int i = steps.size() - 1; i >= 0; i--) {
            final Step step = steps.get(i);
            if (currentValue > step.value) {
                return step;
            }
            break;
        }
        return null;
    }

    private Step findStepOfCustor() {
        for (int i = 0; i < steps.size(); ++i) {
            final Step step = steps.get(i);
            if (currentValue < step.value) {
                return step;
            }
            break;
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        {
            final float paddingLeft = settings.paddingCorners;
            final float paddingRight = settings.paddingCorners;

            final Step stepBeforeCustor = findStepOfCustor();
            if (stepBeforeCustor != null) {
                settings.paintIndicator.setColor(stepBeforeCustor.colorBefore);
                settings.paintBubble.setColor(stepBeforeCustor.colorBefore);
            } else {
                if (settings.step_colorizeAfterLast) {
                    settings.paintIndicator.setColor(settings.step_colorAfterLast);
                    settings.paintBubble.setColor(settings.step_colorAfterLast);
                } else {
                    settings.paintIndicator.setColor(settings.colorBackground);
                    settings.paintBubble.setColor(settings.colorBackground);
                }
            }

            final float radiusCorner = settings.barHeight / 2f;

            final float indicatorCenterX = indicatorX + paddingLeft;

            { //background
                final float centerCircleLeft = paddingLeft;
                final float centerCircleRight = getWidth() - paddingRight;

                //grey background
                settings.paintBar.setColor(settings.colorBackground);
                canvas.drawCircle(centerCircleLeft, barCenterY, radiusCorner, settings.paintBar);
                canvas.drawCircle(centerCircleRight, barCenterY, radiusCorner, settings.paintBar);
                canvas.drawRect(centerCircleLeft, barY, centerCircleRight, barY + settings.barHeight, settings.paintBar);

                float lastX = centerCircleLeft;
                boolean first = true;
                for (Step step : steps) {
                    settings.paintBar.setColor(step.colorBefore);
                    if (first) {
                        canvas.drawCircle(centerCircleLeft, barCenterY, radiusCorner, settings.paintBar);
                    }

                    final float x = step.xStart + paddingLeft;
                    canvas.drawRect(lastX, barY, x, barY + settings.barHeight, settings.paintBar);
                    lastX = x;

                    first = false;
                }


                if (settings.step_colorizeAfterLast) {
                    //find the step just below currentValue
                    for (int i = steps.size() - 1; i >= 0; i--) {
                        final Step step = steps.get(i);
                        if (currentValue > step.value) {
                            settings.paintBar.setColor(settings.step_colorAfterLast);
                            canvas.drawRect(step.xStart + paddingLeft, barY, indicatorCenterX, barY + settings.barHeight, settings.paintBar);
                            break;
                        }
                    }
                }
            }


            { //texts top (values)
                if (settings.drawTextOnTop) {
                    final float textY = barY - 30;
                    if (settings.step_centerText) {
                        float lastX = 0;
                        for (Step step : steps) {
                            final float x = paddingLeft + (step.xStart - lastX) / 2f;
                            drawIndicatorsText(canvas, formatValue(step.value), x, textY, Layout.Alignment.ALIGN_CENTER);
                            lastX = x;
                        }
                    } else {
                        drawIndicatorsText(canvas, formatValue(0), 0 + paddingLeft, textY, Layout.Alignment.ALIGN_CENTER);
                        for (Step step : steps) {
                            drawIndicatorsText(canvas, formatValue(step.value), step.xStart + paddingLeft, textY, Layout.Alignment.ALIGN_CENTER);
                        }
                        drawIndicatorsText(canvas, formatValue(max), canvas.getWidth(), textY, Layout.Alignment.ALIGN_CENTER);
                    }
                }
            }


            { //steps
                for (Step step : steps) {
                    canvas.drawLine(step.xStart + paddingLeft, barY - settings.barHeight / 4f, step.xStart + paddingLeft, barY + settings.barHeight + settings.barHeight / 4f, settings.paintStep);

                    final float textY = barY + settings.barHeight + 15;

                    if (settings.drawTextOnBottom) {
                        //drawMultilineText(canvas, maxText, canvas.getWidth() - settings.paintText.measureText(maxText), textY, settings.paintText, Layout.Alignment.ALIGN_OPPOSITE);
                        drawMultilineText(canvas, step.name, step.xStart + paddingLeft, textY, settings.paintText, Layout.Alignment.ALIGN_CENTER);
                    }
                }
            }

            {
                canvas.drawCircle(indicatorCenterX, this.barCenterY, indicatorRadius, settings.paintIndicator);
                settings.paintIndicator.setColor(Color.WHITE);
                canvas.drawCircle(indicatorCenterX, this.barCenterY, indicatorRadius * 0.85f, settings.paintIndicator);
            }

            //bubble
            {
                float bubbleCenterX = indicatorCenterX;
                float trangleCenterX = indicatorCenterX;
                if (bubbleCenterX > canvas.getWidth() - bubbleWidth / 2f) {
                    bubbleCenterX = canvas.getWidth() - bubbleWidth / 2f;
                } else if (bubbleCenterX - bubbleWidth / 2f < 0) {
                    bubbleCenterX = bubbleWidth / 2f;
                }
                drawBubble(canvas, bubbleCenterX, trangleCenterX, 0);
            }
        }

        canvas.restore();
    }

    private String formatValue(float value) {
        return String.format("%d €", (int) value);
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

                final float right = lineX + lineWidth;
                if (right > canvas.getWidth()) {
                    lineX -= (canvas.getWidth() - right);
                }

                canvas.translate(lineX, lineY);
                final StaticLayout staticLayout = new StaticLayout(line, paint, (int) lineWidth, aligment, 1.0f, 0, false);
                staticLayout.draw(canvas);

                lineY += lineHeight;
            }
            canvas.restore();
        }

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

    private void drawIndicatorsText(Canvas canvas, String text, float x, float y, Layout.Alignment alignment) {
        y -= settings.paintText.getTextSize();

        final int width = (int) settings.paintText.measureText(text);
        if (x >= getWidth() - settings.paddingCorners) {
            x = (getWidth() - width - settings.paddingCorners / 2f);
        } else if (x <= 0) {
            x = width / 2f;
        } else {
            x = (x - width / 2f);
        }

        drawText(canvas, text, x, y, settings.paintText, alignment);
    }

    private float calculateBubbleTextWidth() {
        final String bubbleText = formatValue(getCurrentValue());
        return settings.paintBubbleTextCurrent.measureText(bubbleText);
    }

    private void drawBubble(Canvas canvas, float centerX, float triangleCenterX, float y) {
        final float width = this.bubbleWidth;
        final float height = this.bubbleHeight;

        canvas.save();
        {
            canvas.translate(centerX - width / 2f, y);
            triangleCenterX -= (centerX - width / 2f);
            {

                canvas.save();
                {
                    final Path arrowPath = new Path();
                    arrowPath.moveTo(triangleCenterX - BUBBLE_ARROW_WIDTH / 2f, height - BUBBLE_ARROW_HEIGHT);
                    arrowPath.lineTo(triangleCenterX + BUBBLE_ARROW_WIDTH / 2f, height - BUBBLE_ARROW_HEIGHT);
                    arrowPath.lineTo(triangleCenterX, height);
                    arrowPath.close();

                    canvas.drawPath(arrowPath, settings.paintBubble);
                }
                canvas.restore();
                canvas.save();
                {
                    final float roundRectHeight = height - BUBBLE_ARROW_HEIGHT;
                    final float radius = roundRectHeight / 2f;
                    canvas.drawCircle(radius, radius, radius, settings.paintBubble);
                    canvas.drawCircle(width - radius, radius, radius, settings.paintBubble);
                    canvas.drawRect(0 + radius, 0, width - radius, roundRectHeight, settings.paintBubble);
                }
                canvas.restore();
            }

            final String bubbleText = formatValue(getCurrentValue());
            drawText(canvas, bubbleText, BUBBLE_PADDING_HORIZONTAL, BUBBLE_PADDING_VERTICAL - 3, settings.paintBubbleTextCurrent, Layout.Alignment.ALIGN_NORMAL);
        }

        canvas.restore();

    }

    public interface Listener {
        void valueChanged(Slidr slidr, float currentValue);
    }

    public static class Step implements Comparable<Step> {
        private String name;
        private float value;

        private float xStart;
        private int colorBefore;

        public Step(String name, float value, int colorBefore) {
            this.name = name;
            this.value = value;
            this.colorBefore = colorBefore;
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
        private TextPaint paintText;
        private TextPaint paintBubbleTextCurrent;
        private Paint paintBubble;
        private int colorLeft = Color.parseColor("#007E90");
        private int step_colorAfterLast = Color.parseColor("#ed5564");
        private int colorBackground = Color.parseColor("#cccccc");
        private int colorStoppover = Color.BLACK;
        private int textColor = Color.parseColor("#6E6E6E");
        private int textSize = 12;
        private int textSizeBubbleCurrent = 16;
        private float barHeight = 35;
        private float paddingCorners = 60;

        private boolean step_colorizeAfterLast = false;
        private boolean step_centerText = false;
        private boolean drawTextOnTop = true;
        private boolean drawTextOnBottom = true;

        public Settings(Slidr slidr) {
            this.slidr = slidr;

            paintIndicator = new Paint();
            paintIndicator.setAntiAlias(true);
            paintIndicator.setStrokeWidth(2);
            paintIndicator.setColor(colorLeft);

            paintBar = new Paint();
            paintBar.setAntiAlias(true);
            paintBar.setStrokeWidth(2);
            paintBar.setColor(colorBackground);

            paintStep = new Paint();
            paintStep.setAntiAlias(true);
            paintStep.setStrokeWidth(5);
            paintStep.setColor(colorStoppover);

            paintText = new TextPaint();
            paintText.setAntiAlias(true);
            paintText.setStyle(Paint.Style.FILL);
            paintText.setColor(textColor);
            paintText.setTextSize(dpToPx(textSize));

            paintBubbleTextCurrent = new TextPaint();
            paintBubbleTextCurrent.setAntiAlias(true);
            paintBubbleTextCurrent.setStyle(Paint.Style.FILL);
            paintBubbleTextCurrent.setColor(Color.WHITE);
            paintBubbleTextCurrent.setTextSize(dpToPx(textSizeBubbleCurrent));

            paintBubble = new Paint();
            paintBubble.setAntiAlias(true);
            paintBubble.setStrokeWidth(2);
        }

        private void init(Context context, AttributeSet attrs) {
            if (attrs != null) {
                final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Slidr);
                setColorBackground(a.getColor(R.styleable.Slidr_slidr_colorbackground, colorBackground));
                this.step_colorizeAfterLast = a.getBoolean(R.styleable.Slidr_slidr_step_colorizeAfterLast, step_colorizeAfterLast);
                this.step_colorAfterLast = a.getColor(R.styleable.Slidr_slidr_step_colorAfterLast, step_colorAfterLast);
                this.drawTextOnTop = a.getBoolean(R.styleable.Slidr_slidr_drawTextOnTop, drawTextOnTop);
                this.drawTextOnBottom = a.getBoolean(R.styleable.Slidr_slidr_drawTextOnBottom, drawTextOnBottom);
                this.barHeight = a.getDimensionPixelOffset(R.styleable.Slidr_slidr_barHeight, (int) barHeight);
                this.step_centerText = a.getBoolean(R.styleable.Slidr_slidr_step_centerText, step_centerText);
                a.recycle();
            }
        }

        public void setColorBackground(int colorBackground) {
            this.colorBackground = colorBackground;
            slidr.update();
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
            this.paintText.setTextSize(dpToPx(textSize));
            slidr.update();
        }

        private float dpToPx(int size) {
            return size * slidr.getResources().getDisplayMetrics().density;
        }
    }
}
