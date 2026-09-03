package co.candyhouse.app.util.qrcode.core;

import android.graphics.PointF;

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
