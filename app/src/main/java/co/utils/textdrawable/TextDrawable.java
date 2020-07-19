package co.utils.textdrawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import co.utils.textdrawable.util.TypefaceHelper;

//import android.support.annotation.ColorInt;
//import android.support.annotation.IntDef;
//import android.support.annotation.IntRange;
//import android.support.annotation.NonNull;


/**
 * @author amulya, alvinhkh
 */
public class TextDrawable extends ShapeDrawable {

    public static final int SHAPE_RECT = 0;

    public static final int SHAPE_ROUND_RECT = 1;

    public static final int SHAPE_ROUND = 2;
    
    private static final float SHADE_FACTOR = 0.9f;

    private Bitmap bitmap;

    private final int borderColor;

    private final Paint borderPaint;

    private final int borderThickness;

    private final int fontSize;

    private final int height;

    private final String text;

    private final Paint textPaint;

    private final float radius;

    @TextDrawableShape
    private final int shape;

    private final int width;

    private TextDrawable(Builder builder) {
        super(builder.getShape());
        
        // shape properties
        shape = builder.shape;
        height = builder.height;
        width = builder.width;
        radius = builder.radius;

        // text and color
        text = builder.toUpperCase ? builder.text.toUpperCase() : builder.text;

        // text paint settings
        fontSize = builder.fontSize;
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(builder.textColor);
        textPaint.setFakeBoldText(builder.isBold);
        textPaint.setStrokeWidth(builder.borderThickness);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(builder.font);

        // border paint settings
        borderThickness = builder.borderThickness;
        borderColor = builder.borderColor;
        borderPaint = new Paint();
        if (borderColor == -1) borderPaint.setColor(getDarkerShade(builder.color));
        else borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderThickness);

        // drawable paint setColor
        Paint paint = getPaint();
        paint.setColor(builder.color);

        //custom centre drawable
        if (builder.drawable != null) {
            if (builder.drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) builder.drawable).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(builder.drawable.getIntrinsicWidth(),
                        builder.drawable.getIntrinsicHeight(),
                        builder.drawable.getOpacity() != PixelFormat.OPAQUE ?
                                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                builder.drawable.setBounds(0, 0, builder.drawable.getIntrinsicWidth(),
                        builder.drawable.getIntrinsicHeight());
                builder.drawable.draw(canvas);
            }
        }
    }

    private int getDarkerShade(@ColorInt int color) {
        return Color.rgb((int) (SHADE_FACTOR * Color.red(color)),
                (int) (SHADE_FACTOR * Color.green(color)),
                (int) (SHADE_FACTOR * Color.blue(color)));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Rect r = getBounds();
        // draw border
        if (borderThickness > 0) {
            drawBorder(canvas);
        }
        int count = canvas.save();
        if (bitmap == null) {
            canvas.translate(r.left, r.top);
        }
        // draw text
        int width = this.width < 0 ? r.width() : this.width;
        int height = this.height < 0 ? r.height() : this.height;
        int fontSize = this.fontSize < 0 ? (Math.min(width, height) / 2) : this.fontSize;
        textPaint.setTextSize(fontSize);
        Rect textBounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, width / 2, height / 2 - textBounds.exactCenterY(), textPaint);
        if (bitmap == null) {
            textPaint.setTextSize(fontSize);
            canvas.drawText(text, width / 2, height / 2 - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint);
        } else {
            canvas.drawBitmap(bitmap, (width - bitmap.getWidth()) / 2, (height - bitmap.getHeight()) / 2, null);
        }
        canvas.restoreToCount(count);

    }

    private void drawBorder(Canvas canvas) {
        RectF rect = new RectF(getBounds());
        rect.inset(borderThickness / 2, borderThickness / 2);
        switch (shape) {
            case SHAPE_ROUND_RECT:
                canvas.drawRoundRect(rect, radius, radius, borderPaint);
                break;
            case SHAPE_ROUND:
                canvas.drawOval(rect, borderPaint);
                break;
            case SHAPE_RECT:
            default:
                canvas.drawRect(rect, borderPaint);
                break;
        }
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    public Bitmap getBitmap() {
        Bitmap bitmap;
        if (getIntrinsicWidth() <= 0 || getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, getOpacity() != PixelFormat.OPAQUE ?
                    Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        } else {
            bitmap = Bitmap.createBitmap(getIntrinsicWidth(), getIntrinsicHeight(),
                    getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(bitmap);
        setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        draw(canvas);
        return bitmap;
    }

    public static class Builder {

        private int borderColor;

        private int borderThickness;

        private int color;

        public Drawable drawable;

        private Typeface font;

        private int fontSize;

        private int height;

        private boolean isBold;

        public float radius;

        private int shape;

        private String text;

        public int textColor;

        private boolean toUpperCase;

        private int width;

        public Builder() {
            borderColor = -1;
            borderThickness = 0;
            color = Color.GRAY;
            font = TypefaceHelper.get("sans-serif-light", Typeface.NORMAL);
            fontSize = -1;
            height = -1;
            isBold = false;
            shape = SHAPE_RECT;
            text = "";
            textColor = Color.WHITE;
            toUpperCase = false;
            width = -1;
        }

        public Builder setBold() {
            this.isBold = true;
            return this;
        }

        public Builder setBorder(@IntRange(from = 1, to = Integer.MAX_VALUE) int thickness) {
            this.borderThickness = thickness;
            return this;
        }

        public Builder setBorderColor(@ColorInt int color) {
            this.borderColor = color;
            return this;
        }

        public Builder setColor(@ColorInt int color) {
            this.color = color;
            return this;
        }

        public Builder setDrawable(@NonNull Drawable drawable) {
            this.drawable = drawable;
            return this;
        }

        public Builder setFont(@NonNull Typeface font) {
            this.font = font;
            return this;
        }

        public Builder setFont(@NonNull String name, int style) {
            this.font = TypefaceHelper.get(name, style);
            return this;
        }

        public Builder setFontSize(@IntRange(from = 1, to = Integer.MAX_VALUE) int size) {
            this.fontSize = size;
            return this;
        }

        public Builder setHeight(@IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
            this.height = height;
            return this;
        }

        public Builder setRadius(@IntRange(from = 1, to = Integer.MAX_VALUE) int radius) {
            this.radius = radius;
            return this;
        }

        public Builder setShape(@TextDrawableShape int shape) {
            this.shape = shape;
            return this;
        }

        public Builder setText(@NonNull String text) {
            this.text = text;
            return this;
        }

        public Builder setTextColor(@ColorInt int color) {
            this.textColor = color;
            return this;
        }

        public Builder setWidth(@IntRange(from = 1, to = Integer.MAX_VALUE) int width) {
            this.width = width;
            return this;
        }

        private Shape getShape() {
            switch (shape) {
                case SHAPE_ROUND_RECT:
                    float[] radii = {radius, radius, radius, radius, radius, radius, radius, radius};
                    return new RoundRectShape(radii, null, null);
                case SHAPE_ROUND:
                    return new OvalShape();
                case SHAPE_RECT:
                default:
                    return new RectShape();
            } 
        }

        public TextDrawable build() {
            return new TextDrawable(this);
        }
    }
    
    @IntDef({SHAPE_RECT, SHAPE_ROUND_RECT, SHAPE_ROUND})
    public @interface TextDrawableShape { }
}