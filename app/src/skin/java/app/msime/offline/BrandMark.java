package app.msime.offline;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** 方正N/ope标志：保留上下结构，使用等宽直线与直角字腔。 */
final class BrandMark {
    private static final Path UPPER=new Path(),O=new Path(),P=new Path(),E=new Path();
    static{
        UPPER.moveTo(7,12);UPPER.lineTo(7,2);UPPER.lineTo(17,12);UPPER.lineTo(17,2);
        O.moveTo(2.5f,17);O.lineTo(7.5f,17);O.lineTo(7.5f,22);O.lineTo(2.5f,22);O.close();
        P.moveTo(10,23);P.lineTo(10,17);P.lineTo(14.5f,17);P.lineTo(14.5f,20.5f);P.lineTo(10,20.5f);
        E.moveTo(22,22);E.lineTo(17,22);E.lineTo(17,17);E.lineTo(22,17);E.lineTo(22,19.5f);E.lineTo(17,19.5f);
    }
    static void draw(Canvas canvas,Paint paint,int color){
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.SQUARE);paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(2);canvas.drawPath(UPPER,paint);
        paint.setStrokeWidth(1.35f);canvas.drawPath(O,paint);canvas.drawPath(P,paint);canvas.drawPath(E,paint);
    }
    private BrandMark(){ }
}
