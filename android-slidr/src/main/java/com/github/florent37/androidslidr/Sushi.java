package com.github.florent37.androidslidr;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by florentchampigny on 20/04/2017.
 */

public class Sushi extends FrameLayout {

    private static final float DISTANCE_TEXT_BAR = 35;
    private static final float BUBBLE_PADDING_HORIZONTAL = 15;
    private static final float BUBBLE_PADDING_VERTICAL = 3;
    private static final float BUBBLE_MIN_WITH = 0;

    private Settings settings;

    private float max = 1000;
    private float min = 0;
    private float currentValue = 0;

    private float barY;
    private float barWidth;
    private float indicatorX;
    private float barCenterY;
    private Bubble bubble = new Bubble();
    private TextFormatter textFormatter = new EurosTextFormatter();
    private RegionTextFormatter regionTextFormatter = null;

    private int calculatedHieght = 0;

    public Sushi(Context context) {
        this(context, null);
    }

    public Sushi(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Sushi(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);

        this.settings = new Settings(this);
        this.settings.init(context, attrs);
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

    //endregion

    public void update() {
        if (barWidth > 0f) {
            float currentPercent = indicatorX / barWidth;
            currentValue = currentPercent * (max - min) + min;

            updateBubbleWidth();
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
        this.bubble.width = Math.max(BUBBLE_MIN_WITH, this.bubble.width);
    }

    private void updateValues() {

        if (currentValue < min) {
            currentValue = min;
        }

        settings.paddingCorners = settings.barHeight;

        barWidth = getWidth() - this.settings.paddingCorners * 2;

        updateBubbleWidth();
        this.bubble.height = dpToPx(settings.textSizeBubble) + BUBBLE_PADDING_VERTICAL * 2f;

        this.barY = 0;

        if(settings.displayMinMax) {
            barY += DISTANCE_TEXT_BAR;
            float topTextHeight = 0;
            final String tmpTextLeft = formatRegionValue(0, 0);
            final String tmpTextRight = formatRegionValue(1, 0);
            topTextHeight = Math.max(topTextHeight, calculateTextMultilineHeight(tmpTextLeft, settings.paintTextTop));
            topTextHeight = Math.max(topTextHeight, calculateTextMultilineHeight(tmpTextRight, settings.paintTextTop));

            this.barY += topTextHeight + 3;
        } else {
            barY = 15;
        }

        this.barCenterY = barY + settings.barHeight / 2f;

        this.bubble.y = barCenterY - bubble.height / 2f;

        indicatorX = (currentValue - min) / (max - min) * barWidth;

        calculatedHieght = (int) (barCenterY + settings.barHeight);

        calculatedHieght += 10; //padding bottom

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        {

            final float paddingLeft = settings.paddingCorners;
            final float paddingRight = settings.paddingCorners;

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


                //color before indicator
                settings.paintBar.setColor(settings.foregroundColor);

                canvas.drawCircle(centerCircleLeft, barCenterY, radiusCorner, settings.paintBar);
                canvas.drawRect(centerCircleLeft, barY, indicatorCenterX, barY + settings.barHeight, settings.paintBar);
            }


            if (settings.displayMinMax) { //texts top (values)
                final float textY = barY - DISTANCE_TEXT_BAR;
                drawIndicatorsTextAbove(canvas, formatValue(min), settings.paintTextTop, 0 + paddingLeft, textY, Layout.Alignment.ALIGN_CENTER);
                drawIndicatorsTextAbove(canvas, formatValue(max), settings.paintTextTop, canvas.getWidth(), textY, Layout.Alignment.ALIGN_CENTER);
            }

            //bubble
            {

                float bubbleCenterX = indicatorCenterX;
                float trangleCenterX;

                bubble.x = bubbleCenterX - bubble.width / 2f;

                if (bubbleCenterX > canvas.getWidth() - bubble.width / 2f) {
                    bubbleCenterX = canvas.getWidth() - bubble.width / 2f;
                } else if (bubbleCenterX - bubble.width / 2f < 0) {
                    bubbleCenterX = bubble.width / 2f;
                }

                trangleCenterX = (bubbleCenterX + indicatorCenterX) / 2f;

                drawBubble(canvas, bubbleCenterX, trangleCenterX, bubble.getY());
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

    private float calculateBubbleTextWidth() {
        String bubbleText = formatValue(getCurrentValue());
        return settings.paintTextBubble.measureText(bubbleText);
    }

    private void drawBubblePath(Canvas canvas, float triangleCenterX, float height, float width) {
        final Path path = new Path();

        int padding = 3;
        final Rect rect = new Rect(padding, padding, (int) width - padding, (int) (height) - padding);

        final float roundRectHeight = (height) / 2;

        path.moveTo(rect.left + roundRectHeight, rect.top);
        path.lineTo(rect.right - roundRectHeight, rect.top);
        path.quadTo(rect.right, rect.top, rect.right, rect.top + roundRectHeight);
        path.lineTo(rect.right, rect.bottom - roundRectHeight);
        path.quadTo(rect.right, rect.bottom, rect.right - roundRectHeight, rect.bottom);

        path.lineTo(triangleCenterX, height - padding);
        path.lineTo(triangleCenterX, height - padding);
        path.lineTo(triangleCenterX, height - padding);

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

            settings.paintBubble.setStyle(Paint.Style.FILL);
            settings.paintBubble.setColor(settings.foregroundColor);
            drawBubblePath(canvas, triangleCenterX, height, width);

            settings.paintBubble.setStyle(Paint.Style.FILL);
        }

        final String bubbleText = formatValue(getCurrentValue());
        drawText(canvas, bubbleText, BUBBLE_PADDING_HORIZONTAL, bubble.getHeight() / 2f - settings.paintTextBubble.getTextSize() / 2f - BUBBLE_PADDING_VERTICAL, settings.paintTextBubble, Layout.Alignment.ALIGN_NORMAL);

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

    public Settings getSettings() {
        return settings;
    }

    public interface TextFormatter {
        String format(float value);
    }

    public interface RegionTextFormatter {
        String format(int region, float value);
    }

    public static class Settings {
        private Sushi slidr;
        private Paint paintBar;
        private TextPaint paintTextTop;
        private TextPaint paintTextBubble;
        private Paint paintBubble;
        private int colorBackground = Color.parseColor("#cccccc");
        private int textColor = Color.parseColor("#6E6E6E");

        private int textSize = 12;
        private int textSizeBubble = 16;

        private float barHeight = 35;
        private float paddingCorners;
        private int foregroundColor = Color.parseColor("#007E90");


        private boolean displayMinMax = true;

        public Settings(Sushi slidr) {
            this.slidr = slidr;

            paintBar = new Paint();
            paintBar.setAntiAlias(true);
            paintBar.setStrokeWidth(2);
            paintBar.setColor(colorBackground);

            paintTextTop = new TextPaint();
            paintTextTop.setAntiAlias(true);
            paintTextTop.setStyle(Paint.Style.FILL);
            paintTextTop.setColor(textColor);
            paintTextTop.setTextSize(dpToPx(textSize));

            paintTextBubble = new TextPaint();
            paintTextBubble.setAntiAlias(true);
            paintTextBubble.setStyle(Paint.Style.FILL);
            paintTextBubble.setColor(Color.WHITE);
            paintTextBubble.setStrokeWidth(2);
            paintTextBubble.setTextSize(dpToPx(textSizeBubble));

            paintBubble = new Paint();
            paintBubble.setAntiAlias(true);
            paintBubble.setStrokeWidth(3);
        }

        private void init(Context context, AttributeSet attrs) {
            if (attrs != null) {
                final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Sushi);
                setColorBackground(a.getColor(R.styleable.Sushi_sushi_backgroundColor, colorBackground));

                this.barHeight = a.getDimensionPixelOffset(R.styleable.Sushi_sushi_barHeight, (int) barHeight);
                this.foregroundColor = a.getColor(R.styleable.Sushi_sushi_foregroundColor, foregroundColor);

                this.textSize = a.getDimensionPixelOffset(R.styleable.Sushi_sushi_textSize, (int) dpToPx(textSize));
                this.paintTextTop.setTextSize(textSize);

                this.textSizeBubble = a.getDimensionPixelOffset(R.styleable.Sushi_sushi_bubbleTextSize, (int) dpToPx(textSizeBubble));
                this.paintTextBubble.setTextSize(textSizeBubble);

                this.displayMinMax = a.getBoolean(R.styleable.Sushi_sushi_displayMinMax, displayMinMax);

                a.recycle();
            }
        }

        public void setBarHeight(int barHeight) {
            this.barHeight = barHeight;
            slidr.updateValues();
            slidr.update();
        }

        public void setForegroundColor(int foregroundColor) {
            this.foregroundColor = foregroundColor;
            slidr.update();
        }

        public void setColorBackground(int colorBackground) {
            this.colorBackground = colorBackground;
            slidr.update();
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
            this.paintTextTop.setTextSize(textSize);
            slidr.updateValues();
            slidr.update();
        }

        public void setBubbleTextSize(int textSizeBubble) {
            this.textSizeBubble = textSizeBubble;
            this.paintTextBubble.setTextSize(textSizeBubble);
            slidr.updateValues();
            slidr.update();
        }

        private float dpToPx(int size) {
            return size * slidr.getResources().getDisplayMetrics().density;
        }

        public void setDisplayMinMax(boolean displayMinMax) {
            this.displayMinMax = displayMinMax;
            slidr.updateValues();
            slidr.update();
            slidr.requestLayout();
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
            return height;
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
