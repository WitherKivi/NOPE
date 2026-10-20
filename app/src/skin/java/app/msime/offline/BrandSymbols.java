package app.msime.offline;

import android.graphics.*;

/** 皮肤专属标志；保留默认品牌和 S4U/NET. 的独立入口。 */
final class BrandSymbols {
    static boolean draw(Canvas canvas,Paint paint,Path path,String skin){
        path.reset();paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        if(skin.equals("cloud")){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.65f);
            path.moveTo(5,18);path.cubicTo(1,18,1,11,6,11);path.cubicTo(5,5,13,3,16,9);
            path.cubicTo(20,6,24,12,21,16);path.cubicTo(20,20,16,18,13,19);path.cubicTo(10,18,8,20,5,18);
            canvas.drawPath(path,paint);
            paint.setStrokeWidth(.7f);path.reset();path.moveTo(4,19.5f);path.cubicTo(8,21,17,19,20,19);canvas.drawPath(path,paint);return true;
        }
        if(skin.equals("metal")){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);
            canvas.drawRoundRect(2,2,8,8,3,3,paint);canvas.drawRoundRect(16,2,22,8,3,3,paint);
            canvas.drawRoundRect(2,16,8,22,3,3,paint);canvas.drawRoundRect(16,16,22,22,3,3,paint);
            canvas.drawLine(8,5,8,19,paint);canvas.drawLine(16,5,16,19,paint);
            canvas.drawLine(5,8,19,8,paint);canvas.drawLine(5,16,19,16,paint);return true;
        }
        if(skin.equals("ios")){
            paint.setStyle(Paint.Style.FILL);
            path.moveTo(12,6);path.cubicTo(7,5,9,10,5,14);path.cubicTo(0,22,13,25,19,21);
            path.cubicTo(24,17,17,12,17,9);path.cubicTo(17,6,15,5,12,6);path.close();
            Path bite=new Path();bite.addCircle(20,11,3.8f,Path.Direction.CW);path.op(bite,Path.Op.DIFFERENCE);canvas.drawPath(path,paint);
            path.reset();path.moveTo(12,5);path.cubicTo(12,1,18,0,19,1);path.cubicTo(18,5,15,5,12,5);canvas.drawPath(path,paint);return true;
        }
        return false;
    }
    private BrandSymbols(){ }
}
