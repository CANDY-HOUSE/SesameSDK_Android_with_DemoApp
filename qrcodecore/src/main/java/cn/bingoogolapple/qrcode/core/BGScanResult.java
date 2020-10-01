package cn.bingoogolapple.qrcode.core;

import android.graphics.PointF;

/**
 * 作者:王浩
 * 创建时间:2018/6/15
 * 描述:
 */
public class BGScanResult {
    String result;
    PointF[] resultPoints;

    public BGScanResult(String result) {
        this.result = result;
    }

    public BGScanResult(String result, PointF[] resultPoints) {
        this.result = result;
        this.resultPoints = resultPoints;
    }
}
