package cn.bingoogolapple.qrcode.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import co.candyhouse.app.R;

public class ScanBoxView extends View {
    private int mMoveStepDistance;
    private int mAnimDelayTime;

    private Rect mFramingRect;
    private float mScanLineTop;
    private float mScanLineLeft;
    private Paint mPaint;
    private TextPaint mTipPaint;

    private int mMaskColor;
    private int mCornerColor;
    private int mCornerLength;
    private int mCornerSize;
    private int mRectWidth;
    private int mRectHeight;
    private int mBarcodeRectHeight;
    private int mTopOffset;
    private int mScanLineSize;
    private int mScanLineColor;
    private int mScanLineMargin;
    private boolean mIsShowDefaultScanLineDrawable;
    private Drawable mCustomScanLineDrawable;
    private Bitmap mScanLineBitmap;
    private int mBorderSize;
    private int mBorderColor;
    private int mAnimTime;
    private float mVerticalBias;
    private int mCornerDisplayType;
    private int mToolbarHeight;
    private boolean mIsBarcode;
    private String mQRCodeTipText;
    private String mBarCodeTipText;
    private String mTipText;
    private int mTipTextSize;
    private int mTipTextColor;
    private boolean mIsTipTextBelowRect;
    private int mTipTextMargin;
    private boolean mIsShowTipTextAsSingleLine;
    private int mTipBackgroundColor;
    private boolean mIsShowTipBackground;
    private boolean mIsScanLineReverse;
    private boolean mIsShowDefaultGridScanLineDrawable;
    private Drawable mCustomGridScanLineDrawable;
    private Bitmap mGridScanLineBitmap;
    private float mGridScanLineBottom;
    private float mGridScanLineRight;

    private Bitmap mOriginQRCodeScanLineBitmap;
    private Bitmap mOriginBarCodeScanLineBitmap;
    private Bitmap mOriginQRCodeGridScanLineBitmap;
    private Bitmap mOriginBarCodeGridScanLineBitmap;


    private float mHalfCornerSize;
    private StaticLayout mTipTextSl;
    private int mTipBackgroundRadius;

    private boolean mIsOnlyDecodeScanBoxArea;
    private boolean mIsShowLocationPoint;
    private boolean mIsAutoZoom;

    private QRCodeView mQRCodeView;

    public ScanBoxView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mMaskColor = Color.parseColor("#33FFFFFF");
        mCornerColor = Color.WHITE;
        mCornerLength = BGQRCodeUtil.dp2px(context, 20);
        mCornerSize = BGQRCodeUtil.dp2px(context, 3);
        mScanLineSize = BGQRCodeUtil.dp2px(context, 1);
        mScanLineColor = Color.WHITE;
//        mTopOffset = BGQRCodeUtil.dp2px(context, 90);
        mRectWidth = BGQRCodeUtil.dp2px(context, 230);
        mBarcodeRectHeight = BGQRCodeUtil.dp2px(context, 140);
        mScanLineMargin = 0;
        mIsShowDefaultScanLineDrawable = false;
        mCustomScanLineDrawable = null;
        mScanLineBitmap = null;
        mBorderSize = BGQRCodeUtil.dp2px(context, 1);
        mBorderColor = Color.WHITE;
        mAnimTime = 1000;
        mVerticalBias = -1;
        mCornerDisplayType = 1;
        mToolbarHeight = 0;
        mIsBarcode = false;
        mMoveStepDistance = BGQRCodeUtil.dp2px(context, 3);
        mTipText = null;
        mTipTextSize = BGQRCodeUtil.sp2px(context, 14);
        mTipTextColor = Color.WHITE;
        mIsTipTextBelowRect = false;
        mTipTextMargin = BGQRCodeUtil.dp2px(context, 20);
        mIsShowTipTextAsSingleLine = false;
        mTipBackgroundColor = Color.parseColor("#00000000");
        mIsShowTipBackground = false;
        mIsScanLineReverse = false;
        mIsShowDefaultGridScanLineDrawable = false;

        mTipPaint = new TextPaint();
        mTipPaint.setAntiAlias(true);

        mTipBackgroundRadius = BGQRCodeUtil.dp2px(context, 4);

        mIsOnlyDecodeScanBoxArea = false;
        mIsShowLocationPoint = false;
        mIsAutoZoom = false;
    }

    void init(QRCodeView qrCodeView, AttributeSet attrs) {
        mQRCodeView = qrCodeView;

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.QRCodeView);
        final int count = typedArray.getIndexCount();
        for (int i = 0; i < count; i++) {
            initCustomAttr(typedArray.getIndex(i), typedArray);
        }
        typedArray.recycle();

        afterInitCustomAttrs();
    }

    private void initCustomAttr(int attr, TypedArray typedArray) {
        if (attr == R.styleable.QRCodeView_qrcv_topOffset) {
            mTopOffset = typedArray.getDimensionPixelSize(attr, mTopOffset);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerSize) {
            mCornerSize = typedArray.getDimensionPixelSize(attr, mCornerSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerLength) {
            mCornerLength = typedArray.getDimensionPixelSize(attr, mCornerLength);
        } else if (attr == R.styleable.QRCodeView_qrcv_scanLineSize) {
            mScanLineSize = typedArray.getDimensionPixelSize(attr, mScanLineSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_rectWidth) {
            mRectWidth = typedArray.getDimensionPixelSize(attr, mRectWidth);
        } else if (attr == R.styleable.QRCodeView_qrcv_maskColor) {
            mMaskColor = typedArray.getColor(attr, mMaskColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerColor) {
            mCornerColor = typedArray.getColor(attr, mCornerColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_scanLineColor) {
            mScanLineColor = typedArray.getColor(attr, mScanLineColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_scanLineMargin) {
            mScanLineMargin = typedArray.getDimensionPixelSize(attr, mScanLineMargin);
        } else if (attr == R.styleable.QRCodeView_qrcv_isShowDefaultScanLineDrawable) {
            mIsShowDefaultScanLineDrawable = typedArray.getBoolean(attr, mIsShowDefaultScanLineDrawable);
        } else if (attr == R.styleable.QRCodeView_qrcv_customScanLineDrawable) {
            mCustomScanLineDrawable = typedArray.getDrawable(attr);
        } else if (attr == R.styleable.QRCodeView_qrcv_borderSize) {
            mBorderSize = typedArray.getDimensionPixelSize(attr, mBorderSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_borderColor) {
            mBorderColor = typedArray.getColor(attr, mBorderColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_animTime) {
            mAnimTime = typedArray.getInteger(attr, mAnimTime);
        } else if (attr == R.styleable.QRCodeView_qrcv_verticalBias) {
            mVerticalBias = typedArray.getFloat(attr, mVerticalBias);
        } else if (attr == R.styleable.QRCodeView_qrcv_cornerDisplayType) {
            mCornerDisplayType = typedArray.getInteger(attr, mCornerDisplayType);
        } else if (attr == R.styleable.QRCodeView_qrcv_toolbarHeight) {
            mToolbarHeight = typedArray.getDimensionPixelSize(attr, mToolbarHeight);
        } else if (attr == R.styleable.QRCodeView_qrcv_barcodeRectHeight) {
            mBarcodeRectHeight = typedArray.getDimensionPixelSize(attr, mBarcodeRectHeight);
        } else if (attr == R.styleable.QRCodeView_qrcv_isBarcode) {
            mIsBarcode = typedArray.getBoolean(attr, mIsBarcode);
        } else if (attr == R.styleable.QRCodeView_qrcv_barCodeTipText) {
            mBarCodeTipText = typedArray.getString(attr);
        } else if (attr == R.styleable.QRCodeView_qrcv_qrCodeTipText) {
            mQRCodeTipText = typedArray.getString(attr);
        } else if (attr == R.styleable.QRCodeView_qrcv_tipTextSize) {
            mTipTextSize = typedArray.getDimensionPixelSize(attr, mTipTextSize);
        } else if (attr == R.styleable.QRCodeView_qrcv_tipTextColor) {
            mTipTextColor = typedArray.getColor(attr, mTipTextColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_isTipTextBelowRect) {
            mIsTipTextBelowRect = typedArray.getBoolean(attr, mIsTipTextBelowRect);
        } else if (attr == R.styleable.QRCodeView_qrcv_tipTextMargin) {
            mTipTextMargin = typedArray.getDimensionPixelSize(attr, mTipTextMargin);
        } else if (attr == R.styleable.QRCodeView_qrcv_isShowTipTextAsSingleLine) {
            mIsShowTipTextAsSingleLine = typedArray.getBoolean(attr, mIsShowTipTextAsSingleLine);
        } else if (attr == R.styleable.QRCodeView_qrcv_isShowTipBackground) {
            mIsShowTipBackground = typedArray.getBoolean(attr, mIsShowTipBackground);
        } else if (attr == R.styleable.QRCodeView_qrcv_tipBackgroundColor) {
            mTipBackgroundColor = typedArray.getColor(attr, mTipBackgroundColor);
        } else if (attr == R.styleable.QRCodeView_qrcv_isScanLineReverse) {
            mIsScanLineReverse = typedArray.getBoolean(attr, mIsScanLineReverse);
        } else if (attr == R.styleable.QRCodeView_qrcv_isShowDefaultGridScanLineDrawable) {
            mIsShowDefaultGridScanLineDrawable = typedArray.getBoolean(attr, mIsShowDefaultGridScanLineDrawable);
        } else if (attr == R.styleable.QRCodeView_qrcv_customGridScanLineDrawable) {
            mCustomGridScanLineDrawable = typedArray.getDrawable(attr);
        } else if (attr == R.styleable.QRCodeView_qrcv_isOnlyDecodeScanBoxArea) {
            mIsOnlyDecodeScanBoxArea = typedArray.getBoolean(attr, mIsOnlyDecodeScanBoxArea);
        } else if (attr == R.styleable.QRCodeView_qrcv_isShowLocationPoint) {
            mIsShowLocationPoint = typedArray.getBoolean(attr, mIsShowLocationPoint);
        } else if (attr == R.styleable.QRCodeView_qrcv_isAutoZoom) {
            mIsAutoZoom = typedArray.getBoolean(attr, mIsAutoZoom);
        }
    }

    private void afterInitCustomAttrs() {
        if (mCustomGridScanLineDrawable != null) {
            mOriginQRCodeGridScanLineBitmap = ((BitmapDrawable) mCustomGridScanLineDrawable).getBitmap();
        }
        if (mOriginQRCodeGridScanLineBitmap == null) {
            mOriginQRCodeGridScanLineBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.qrcode_default_grid_scan_line);
            mOriginQRCodeGridScanLineBitmap = BGQRCodeUtil.makeTintBitmap(mOriginQRCodeGridScanLineBitmap, mScanLineColor);
        }
        mOriginBarCodeGridScanLineBitmap = BGQRCodeUtil.adjustPhotoRotation(mOriginQRCodeGridScanLineBitmap, 90);
        mOriginBarCodeGridScanLineBitmap = BGQRCodeUtil.adjustPhotoRotation(mOriginBarCodeGridScanLineBitmap, 90);
        mOriginBarCodeGridScanLineBitmap = BGQRCodeUtil.adjustPhotoRotation(mOriginBarCodeGridScanLineBitmap, 90);


        if (mCustomScanLineDrawable != null) {
            mOriginQRCodeScanLineBitmap = ((BitmapDrawable) mCustomScanLineDrawable).getBitmap();
        }
        if (mOriginQRCodeScanLineBitmap == null) {
            mOriginQRCodeScanLineBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.qrcode_default_scan_line);
            mOriginQRCodeScanLineBitmap = BGQRCodeUtil.makeTintBitmap(mOriginQRCodeScanLineBitmap, mScanLineColor);
        }
        mOriginBarCodeScanLineBitmap = BGQRCodeUtil.adjustPhotoRotation(mOriginQRCodeScanLineBitmap, 90);

        mTopOffset += mToolbarHeight;
        mHalfCornerSize = 1.0f * mCornerSize / 2;

        mTipPaint.setTextSize(mTipTextSize);
        mTipPaint.setColor(mTipTextColor);

        setIsBarcode(mIsBarcode);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mFramingRect == null) {
            return;
        }

        drawMask(canvas);

        drawBorderLine(canvas);

        drawCornerLine(canvas);

        drawScanLine(canvas);

        drawTipText(canvas);

        moveScanLine();
    }

    /**
     * 画遮罩层
     */
    private void drawMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (mMaskColor != Color.TRANSPARENT) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mMaskColor);
            canvas.drawRect(0, 0, width, mFramingRect.top, mPaint);
            canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, mPaint);
            canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, mPaint);
            canvas.drawRect(0, mFramingRect.bottom + 1, width, height, mPaint);
        }
    }

    /**
     * 画边框线
     */
    private void drawBorderLine(Canvas canvas) {
        if (mBorderSize > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mBorderColor);
            mPaint.setStrokeWidth(mBorderSize);
            canvas.drawRect(mFramingRect, mPaint);
        }
    }

    /**
     * 画四个直角的线
     */
    private void drawCornerLine(Canvas canvas) {
        if (mHalfCornerSize > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mCornerColor);
            mPaint.setStrokeWidth(mCornerSize);
            if (mCornerDisplayType == 1) {
                canvas.drawLine(mFramingRect.left - mHalfCornerSize, mFramingRect.top, mFramingRect.left - mHalfCornerSize + mCornerLength, mFramingRect.top,
                        mPaint);
                canvas.drawLine(mFramingRect.left, mFramingRect.top - mHalfCornerSize, mFramingRect.left, mFramingRect.top - mHalfCornerSize + mCornerLength,
                        mPaint);
                canvas.drawLine(mFramingRect.right + mHalfCornerSize, mFramingRect.top, mFramingRect.right + mHalfCornerSize - mCornerLength, mFramingRect.top,
                        mPaint);
                canvas.drawLine(mFramingRect.right, mFramingRect.top - mHalfCornerSize, mFramingRect.right, mFramingRect.top - mHalfCornerSize + mCornerLength,
                        mPaint);

                canvas.drawLine(mFramingRect.left - mHalfCornerSize, mFramingRect.bottom, mFramingRect.left - mHalfCornerSize + mCornerLength,
                        mFramingRect.bottom, mPaint);
                canvas.drawLine(mFramingRect.left, mFramingRect.bottom + mHalfCornerSize, mFramingRect.left,
                        mFramingRect.bottom + mHalfCornerSize - mCornerLength, mPaint);
                canvas.drawLine(mFramingRect.right + mHalfCornerSize, mFramingRect.bottom, mFramingRect.right + mHalfCornerSize - mCornerLength,
                        mFramingRect.bottom, mPaint);
                canvas.drawLine(mFramingRect.right, mFramingRect.bottom + mHalfCornerSize, mFramingRect.right,
                        mFramingRect.bottom + mHalfCornerSize - mCornerLength, mPaint);
            } else if (mCornerDisplayType == 2) {
                canvas.drawLine(mFramingRect.left, mFramingRect.top + mHalfCornerSize, mFramingRect.left + mCornerLength, mFramingRect.top + mHalfCornerSize,
                        mPaint);
                canvas.drawLine(mFramingRect.left + mHalfCornerSize, mFramingRect.top, mFramingRect.left + mHalfCornerSize, mFramingRect.top + mCornerLength,
                        mPaint);
                canvas.drawLine(mFramingRect.right, mFramingRect.top + mHalfCornerSize, mFramingRect.right - mCornerLength, mFramingRect.top + mHalfCornerSize,
                        mPaint);
                canvas.drawLine(mFramingRect.right - mHalfCornerSize, mFramingRect.top, mFramingRect.right - mHalfCornerSize, mFramingRect.top + mCornerLength,
                        mPaint);

                canvas.drawLine(mFramingRect.left, mFramingRect.bottom - mHalfCornerSize, mFramingRect.left + mCornerLength,
                        mFramingRect.bottom - mHalfCornerSize, mPaint);
                canvas.drawLine(mFramingRect.left + mHalfCornerSize, mFramingRect.bottom, mFramingRect.left + mHalfCornerSize,
                        mFramingRect.bottom - mCornerLength, mPaint);
                canvas.drawLine(mFramingRect.right, mFramingRect.bottom - mHalfCornerSize, mFramingRect.right - mCornerLength,
                        mFramingRect.bottom - mHalfCornerSize, mPaint);
                canvas.drawLine(mFramingRect.right - mHalfCornerSize, mFramingRect.bottom, mFramingRect.right - mHalfCornerSize,
                        mFramingRect.bottom - mCornerLength, mPaint);
            }
        }
    }

    /**
     * 画扫描线
     */
    private void drawScanLine(Canvas canvas) {
        if (mIsBarcode) {
            if (mGridScanLineBitmap != null) {
                RectF dstGridRectF = new RectF(mFramingRect.left + mHalfCornerSize + 0.5f, mFramingRect.top + mHalfCornerSize + mScanLineMargin,
                        mGridScanLineRight, mFramingRect.bottom - mHalfCornerSize - mScanLineMargin);

                Rect srcGridRect = new Rect((int) (mGridScanLineBitmap.getWidth() - dstGridRectF.width()), 0, mGridScanLineBitmap.getWidth(),
                        mGridScanLineBitmap.getHeight());

                if (srcGridRect.left < 0) {
                    srcGridRect.left = 0;
                    dstGridRectF.left = dstGridRectF.right - srcGridRect.width();
                }

                canvas.drawBitmap(mGridScanLineBitmap, srcGridRect, dstGridRectF, mPaint);
            } else if (mScanLineBitmap != null) {
                RectF lineRect = new RectF(mScanLineLeft, mFramingRect.top + mHalfCornerSize + mScanLineMargin, mScanLineLeft + mScanLineBitmap.getWidth(),
                        mFramingRect.bottom - mHalfCornerSize - mScanLineMargin);
                canvas.drawBitmap(mScanLineBitmap, null, lineRect, mPaint);
            } else {
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mScanLineColor);
                canvas.drawRect(mScanLineLeft, mFramingRect.top + mHalfCornerSize + mScanLineMargin, mScanLineLeft + mScanLineSize,
                        mFramingRect.bottom - mHalfCornerSize - mScanLineMargin, mPaint);
            }
        } else {
            if (mGridScanLineBitmap != null) {
                RectF dstGridRectF = new RectF(mFramingRect.left + mHalfCornerSize + mScanLineMargin, mFramingRect.top + mHalfCornerSize + 0.5f,
                        mFramingRect.right - mHalfCornerSize - mScanLineMargin, mGridScanLineBottom);

                Rect srcRect = new Rect(0, (int) (mGridScanLineBitmap.getHeight() - dstGridRectF.height()), mGridScanLineBitmap.getWidth(),
                        mGridScanLineBitmap.getHeight());

                if (srcRect.top < 0) {
                    srcRect.top = 0;
                    dstGridRectF.top = dstGridRectF.bottom - srcRect.height();
                }

                canvas.drawBitmap(mGridScanLineBitmap, srcRect, dstGridRectF, mPaint);
            } else if (mScanLineBitmap != null) {
                RectF lineRect = new RectF(mFramingRect.left + mHalfCornerSize + mScanLineMargin, mScanLineTop,
                        mFramingRect.right - mHalfCornerSize - mScanLineMargin, mScanLineTop + mScanLineBitmap.getHeight());
                canvas.drawBitmap(mScanLineBitmap, null, lineRect, mPaint);
            } else {
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mScanLineColor);
                canvas.drawRect(mFramingRect.left + mHalfCornerSize + mScanLineMargin, mScanLineTop, mFramingRect.right - mHalfCornerSize - mScanLineMargin,
                        mScanLineTop + mScanLineSize, mPaint);
            }
        }
    }

    /**
     * 画提示文本
     */
    private void drawTipText(Canvas canvas) {
        if (TextUtils.isEmpty(mTipText) || mTipTextSl == null) {
            return;
        }

        if (mIsTipTextBelowRect) {
            if (mIsShowTipBackground) {
                mPaint.setColor(mTipBackgroundColor);
                mPaint.setStyle(Paint.Style.FILL);
                if (mIsShowTipTextAsSingleLine) {
                    Rect tipRect = new Rect();
                    mTipPaint.getTextBounds(mTipText, 0, mTipText.length(), tipRect);
                    float left = (canvas.getWidth() - tipRect.width()) / 2 - mTipBackgroundRadius;
                    canvas.drawRoundRect(
                            new RectF(left, mFramingRect.bottom + mTipTextMargin - mTipBackgroundRadius, left + tipRect.width() + 2 * mTipBackgroundRadius,
                                    mFramingRect.bottom + mTipTextMargin + mTipTextSl.getHeight() + mTipBackgroundRadius), mTipBackgroundRadius,
                            mTipBackgroundRadius, mPaint);
                } else {
                    canvas.drawRoundRect(new RectF(mFramingRect.left, mFramingRect.bottom + mTipTextMargin - mTipBackgroundRadius, mFramingRect.right,
                                    mFramingRect.bottom + mTipTextMargin + mTipTextSl.getHeight() + mTipBackgroundRadius), mTipBackgroundRadius,
                            mTipBackgroundRadius,
                            mPaint);
                }
            }

            canvas.save();
            if (mIsShowTipTextAsSingleLine) {
                canvas.translate(0, mFramingRect.bottom + mTipTextMargin);
            } else {
                canvas.translate(mFramingRect.left + mTipBackgroundRadius, mFramingRect.bottom + mTipTextMargin);
            }
            mTipTextSl.draw(canvas);
            canvas.restore();
        } else {
            if (mIsShowTipBackground) {
                mPaint.setColor(mTipBackgroundColor);
                mPaint.setStyle(Paint.Style.FILL);

                if (mIsShowTipTextAsSingleLine) {
                    Rect tipRect = new Rect();
                    mTipPaint.getTextBounds(mTipText, 0, mTipText.length(), tipRect);
                    float left = (canvas.getWidth() - tipRect.width()) / 2 - mTipBackgroundRadius;
                    canvas.drawRoundRect(new RectF(left, mFramingRect.top - mTipTextMargin - mTipTextSl.getHeight() - mTipBackgroundRadius,
                                    left + tipRect.width() + 2 * mTipBackgroundRadius, mFramingRect.top - mTipTextMargin + mTipBackgroundRadius),
                            mTipBackgroundRadius,
                            mTipBackgroundRadius, mPaint);
                } else {
                    canvas.drawRoundRect(
                            new RectF(mFramingRect.left, mFramingRect.top - mTipTextMargin - mTipTextSl.getHeight() - mTipBackgroundRadius, mFramingRect.right,
                                    mFramingRect.top - mTipTextMargin + mTipBackgroundRadius), mTipBackgroundRadius, mTipBackgroundRadius, mPaint);
                }
            }

            canvas.save();
            if (mIsShowTipTextAsSingleLine) {
                canvas.translate(0, mFramingRect.top - mTipTextMargin - mTipTextSl.getHeight());
            } else {
                canvas.translate(mFramingRect.left + mTipBackgroundRadius, mFramingRect.top - mTipTextMargin - mTipTextSl.getHeight());
            }
            mTipTextSl.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * 移动扫描线的位置
     */
    private void moveScanLine() {
        if (mIsBarcode) {
            if (mGridScanLineBitmap == null) {
                // 处理非网格扫描图片的情况
                mScanLineLeft += mMoveStepDistance;
                int scanLineSize = mScanLineSize;
                if (mScanLineBitmap != null) {
                    scanLineSize = mScanLineBitmap.getWidth();
                }

                if (mIsScanLineReverse) {
                    if (mScanLineLeft + scanLineSize > mFramingRect.right - mHalfCornerSize || mScanLineLeft < mFramingRect.left + mHalfCornerSize) {
                        mMoveStepDistance = -mMoveStepDistance;
                    }
                } else {
                    if (mScanLineLeft + scanLineSize > mFramingRect.right - mHalfCornerSize) {
                        mScanLineLeft = mFramingRect.left + mHalfCornerSize + 0.5f;
                    }
                }
            } else {
                // 处理网格扫描图片的情况
                mGridScanLineRight += mMoveStepDistance;
                if (mGridScanLineRight > mFramingRect.right - mHalfCornerSize) {
                    mGridScanLineRight = mFramingRect.left + mHalfCornerSize + 0.5f;
                }
            }
        } else {
            if (mGridScanLineBitmap == null) {
                // 处理非网格扫描图片的情况
                mScanLineTop += mMoveStepDistance;
                int scanLineSize = mScanLineSize;
                if (mScanLineBitmap != null) {
                    scanLineSize = mScanLineBitmap.getHeight();
                }

                if (mIsScanLineReverse) {
                    if (mScanLineTop + scanLineSize > mFramingRect.bottom - mHalfCornerSize || mScanLineTop < mFramingRect.top + mHalfCornerSize) {
                        mMoveStepDistance = -mMoveStepDistance;
                    }
                } else {
                    if (mScanLineTop + scanLineSize > mFramingRect.bottom - mHalfCornerSize) {
                        mScanLineTop = mFramingRect.top + mHalfCornerSize + 0.5f;
                    }
                }
            } else {
                // 处理网格扫描图片的情况
                mGridScanLineBottom += mMoveStepDistance;
                if (mGridScanLineBottom > mFramingRect.bottom - mHalfCornerSize) {
                    mGridScanLineBottom = mFramingRect.top + mHalfCornerSize + 0.5f;
                }
            }

        }
        postInvalidateDelayed(mAnimDelayTime, mFramingRect.left, mFramingRect.top, mFramingRect.right, mFramingRect.bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calFramingRect();
    }

    private void calFramingRect() {
        int leftOffset = (getWidth() - mRectWidth) / 2;
        mFramingRect = new Rect(leftOffset, mTopOffset, leftOffset + mRectWidth, mTopOffset + mRectHeight);

        if (mIsBarcode) {
            mGridScanLineRight = mScanLineLeft = mFramingRect.left + mHalfCornerSize + 0.5f;
        } else {
            mGridScanLineBottom = mScanLineTop = mFramingRect.top + mHalfCornerSize + 0.5f;
        }

        if (mQRCodeView != null && isOnlyDecodeScanBoxArea()) {
            mQRCodeView.onScanBoxRectChanged(new Rect(mFramingRect));
        }
    }

    public Rect getScanBoxAreaRect(int previewHeight) {
        if (mIsOnlyDecodeScanBoxArea && getVisibility() == View.VISIBLE) {
            Rect rect = new Rect(mFramingRect);
            float ratio = 1.0f * previewHeight / getMeasuredHeight();

            float centerX = rect.exactCenterX() * ratio;
            float centerY = rect.exactCenterY() * ratio;

            float halfWidth = rect.width() / 2f;
            float halfHeight = rect.height() / 2f;
            float newHalfWidth = halfWidth * ratio;
            float newHalfHeight = halfHeight * ratio;

            rect.left = (int) (centerX - newHalfWidth);
            rect.right = (int) (centerX + newHalfWidth);
            rect.top = (int) (centerY - newHalfHeight);
            rect.bottom = (int) (centerY + newHalfHeight);
            return rect;
        } else {
            return null;
        }
    }

    public void setIsBarcode(boolean isBarcode) {
        mIsBarcode = isBarcode;
        refreshScanBox();
    }

    private void refreshScanBox() {
        if (mCustomGridScanLineDrawable != null || mIsShowDefaultGridScanLineDrawable) {
            if (mIsBarcode) {
                mGridScanLineBitmap = mOriginBarCodeGridScanLineBitmap;
            } else {
                mGridScanLineBitmap = mOriginQRCodeGridScanLineBitmap;
            }
        } else if (mCustomScanLineDrawable != null || mIsShowDefaultScanLineDrawable) {
            if (mIsBarcode) {
                mScanLineBitmap = mOriginBarCodeScanLineBitmap;
            } else {
                mScanLineBitmap = mOriginQRCodeScanLineBitmap;
            }
        }

        if (mIsBarcode) {
            mTipText = mBarCodeTipText;
            mRectHeight = mBarcodeRectHeight;
            mAnimDelayTime = (int) ((1.0f * mAnimTime * mMoveStepDistance) / mRectWidth);
        } else {
            mTipText = mQRCodeTipText;
            mRectHeight = mRectWidth;
            mAnimDelayTime = (int) ((1.0f * mAnimTime * mMoveStepDistance) / mRectHeight);
        }

        if (!TextUtils.isEmpty(mTipText)) {
            if (mIsShowTipTextAsSingleLine) {
                mTipTextSl = new StaticLayout(mTipText, mTipPaint, BGQRCodeUtil.getScreenResolution(getContext()).x, Layout.Alignment.ALIGN_CENTER, 1.0f, 0,
                        true);
            } else {
                mTipTextSl = new StaticLayout(mTipText, mTipPaint, mRectWidth - 2 * mTipBackgroundRadius, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, true);
            }
        }

        if (mVerticalBias != -1) {
            int screenHeight = BGQRCodeUtil.getScreenResolution(getContext()).y - BGQRCodeUtil.getStatusBarHeight(getContext());
            if (mToolbarHeight == 0) {
                mTopOffset =  + (int) ((screenHeight ) / mVerticalBias - mRectHeight / 2);
            } else {
                mTopOffset = mToolbarHeight + (int) ((screenHeight - mToolbarHeight) / mVerticalBias - mRectHeight / 2);
            }
        }

        calFramingRect();

        postInvalidate();
    }

    public boolean getIsBarcode() {
        return mIsBarcode;
    }

    public int getMaskColor() {
        return mMaskColor;
    }

    public void setMaskColor(int maskColor) {
        mMaskColor = maskColor;
        refreshScanBox();
    }

    public int getCornerColor() {
        return mCornerColor;
    }

    public void setCornerColor(int cornerColor) {
        mCornerColor = cornerColor;
        refreshScanBox();
    }

    public int getCornerLength() {
        return mCornerLength;
    }

    public void setCornerLength(int cornerLength) {
        mCornerLength = cornerLength;
        refreshScanBox();
    }

    public int getCornerSize() {
        return mCornerSize;
    }

    public void setCornerSize(int cornerSize) {
        mCornerSize = cornerSize;
        refreshScanBox();
    }

    public int getRectWidth() {
        return mRectWidth;
    }

    public void setRectWidth(int rectWidth) {
        mRectWidth = rectWidth;
        refreshScanBox();
    }

    public int getRectHeight() {
        return mRectHeight;
    }

    public void setRectHeight(int rectHeight) {
        mRectHeight = rectHeight;
        refreshScanBox();
    }

    public int getBarcodeRectHeight() {
        return mBarcodeRectHeight;
    }

    public void setBarcodeRectHeight(int barcodeRectHeight) {
        mBarcodeRectHeight = barcodeRectHeight;
        refreshScanBox();
    }

    public int getTopOffset() {
        return mTopOffset;
    }

    public void setTopOffset(int topOffset) {
        mTopOffset = topOffset;
        refreshScanBox();
    }

    public int getScanLineSize() {
        return mScanLineSize;
    }

    public void setScanLineSize(int scanLineSize) {
        mScanLineSize = scanLineSize;
        refreshScanBox();
    }

    public int getScanLineColor() {
        return mScanLineColor;
    }

    public void setScanLineColor(int scanLineColor) {
        mScanLineColor = scanLineColor;
        refreshScanBox();
    }

    public int getScanLineMargin() {
        return mScanLineMargin;
    }

    public void setScanLineMargin(int scanLineMargin) {
        mScanLineMargin = scanLineMargin;
        refreshScanBox();
    }

    public boolean isShowDefaultScanLineDrawable() {
        return mIsShowDefaultScanLineDrawable;
    }

    public void setShowDefaultScanLineDrawable(boolean showDefaultScanLineDrawable) {
        mIsShowDefaultScanLineDrawable = showDefaultScanLineDrawable;
        refreshScanBox();
    }

    public Drawable getCustomScanLineDrawable() {
        return mCustomScanLineDrawable;
    }

    public void setCustomScanLineDrawable(Drawable customScanLineDrawable) {
        mCustomScanLineDrawable = customScanLineDrawable;
        refreshScanBox();
    }

    public Bitmap getScanLineBitmap() {
        return mScanLineBitmap;
    }

    public void setScanLineBitmap(Bitmap scanLineBitmap) {
        mScanLineBitmap = scanLineBitmap;
        refreshScanBox();
    }

    public int getBorderSize() {
        return mBorderSize;
    }

    public void setBorderSize(int borderSize) {
        mBorderSize = borderSize;
        refreshScanBox();
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        mBorderColor = borderColor;
        refreshScanBox();
    }

    public int getAnimTime() {
        return mAnimTime;
    }

    public void setAnimTime(int animTime) {
        mAnimTime = animTime;
        refreshScanBox();
    }

    public float getVerticalBias() {
        return mVerticalBias;
    }

    public void setVerticalBias(float verticalBias) {
        mVerticalBias = verticalBias;
        refreshScanBox();
    }

    public int getToolbarHeight() {
        return mToolbarHeight;
    }

    public void setToolbarHeight(int toolbarHeight) {
        mToolbarHeight = toolbarHeight;
        refreshScanBox();
    }

    public String getQRCodeTipText() {
        return mQRCodeTipText;
    }

    public void setQRCodeTipText(String qrCodeTipText) {
        mQRCodeTipText = qrCodeTipText;
        refreshScanBox();
    }

    public String getBarCodeTipText() {
        return mBarCodeTipText;
    }

    public void setBarCodeTipText(String barCodeTipText) {
        mBarCodeTipText = barCodeTipText;
        refreshScanBox();
    }

    public String getTipText() {
        return mTipText;
    }

    public void setTipText(String tipText) {
        if (mIsBarcode) {
            mBarCodeTipText = tipText;
        } else {
            mQRCodeTipText = tipText;
        }
        refreshScanBox();
    }

    public int getTipTextColor() {
        return mTipTextColor;
    }

    public void setTipTextColor(int tipTextColor) {
        mTipTextColor = tipTextColor;
        mTipPaint.setColor(mTipTextColor);
        refreshScanBox();
    }

    public int getTipTextSize() {
        return mTipTextSize;
    }

    public void setTipTextSize(int tipTextSize) {
        mTipTextSize = tipTextSize;
        mTipPaint.setTextSize(mTipTextSize);
        refreshScanBox();
    }

    public boolean isTipTextBelowRect() {
        return mIsTipTextBelowRect;
    }

    public void setTipTextBelowRect(boolean tipTextBelowRect) {
        mIsTipTextBelowRect = tipTextBelowRect;
        refreshScanBox();
    }

    public int getTipTextMargin() {
        return mTipTextMargin;
    }

    public void setTipTextMargin(int tipTextMargin) {
        mTipTextMargin = tipTextMargin;
        refreshScanBox();
    }

    public boolean isShowTipTextAsSingleLine() {
        return mIsShowTipTextAsSingleLine;
    }

    public void setShowTipTextAsSingleLine(boolean showTipTextAsSingleLine) {
        mIsShowTipTextAsSingleLine = showTipTextAsSingleLine;
        refreshScanBox();
    }

    public boolean isShowTipBackground() {
        return mIsShowTipBackground;
    }

    public void setShowTipBackground(boolean showTipBackground) {
        mIsShowTipBackground = showTipBackground;
        refreshScanBox();
    }

    public int getTipBackgroundColor() {
        return mTipBackgroundColor;
    }

    public void setTipBackgroundColor(int tipBackgroundColor) {
        mTipBackgroundColor = tipBackgroundColor;
        refreshScanBox();
    }

    public boolean isScanLineReverse() {
        return mIsScanLineReverse;
    }

    public void setScanLineReverse(boolean scanLineReverse) {
        mIsScanLineReverse = scanLineReverse;
        refreshScanBox();
    }

    public boolean isShowDefaultGridScanLineDrawable() {
        return mIsShowDefaultGridScanLineDrawable;
    }

    public void setShowDefaultGridScanLineDrawable(boolean showDefaultGridScanLineDrawable) {
        mIsShowDefaultGridScanLineDrawable = showDefaultGridScanLineDrawable;
        refreshScanBox();
    }

    public float getHalfCornerSize() {
        return mHalfCornerSize;
    }

    public void setHalfCornerSize(float halfCornerSize) {
        mHalfCornerSize = halfCornerSize;
        refreshScanBox();
    }

    public StaticLayout getTipTextSl() {
        return mTipTextSl;
    }

    public void setTipTextSl(StaticLayout tipTextSl) {
        mTipTextSl = tipTextSl;
        refreshScanBox();
    }

    public int getTipBackgroundRadius() {
        return mTipBackgroundRadius;
    }

    public void setTipBackgroundRadius(int tipBackgroundRadius) {
        mTipBackgroundRadius = tipBackgroundRadius;
        refreshScanBox();
    }

    public boolean isOnlyDecodeScanBoxArea() {
        return mIsOnlyDecodeScanBoxArea;
    }

    public void setOnlyDecodeScanBoxArea(boolean onlyDecodeScanBoxArea) {
        mIsOnlyDecodeScanBoxArea = onlyDecodeScanBoxArea;
        calFramingRect();
    }

    public boolean isShowLocationPoint() {
        return mIsShowLocationPoint;
    }

    public void setShowLocationPoint(boolean showLocationPoint) {
        mIsShowLocationPoint = showLocationPoint;
    }

    public boolean isAutoZoom() {
        return mIsAutoZoom;
    }

    public void setAutoZoom(boolean autoZoom) {
        mIsAutoZoom = autoZoom;
    }
}