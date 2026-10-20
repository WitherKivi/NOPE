package app.msime.offline;

import java.util.ArrayList;
import java.util.List;

/** 只计算词宽与行索引；ListView仅创建屏幕内候选按钮。 */
final class CandidateRows {
    record Cell(int index,int width) { }
    static List<List<Cell>> pack(int[] widths,int available,int gap) {
        available=Math.max(1,available);gap=Math.max(0,gap);
        List<List<Cell>> rows=new ArrayList<>();List<Cell> row=new ArrayList<>();int used=0;
        for(int i=0;i<widths.length;i++) {
            int width=Math.max(1,Math.min(available,widths[i]));
            if(!row.isEmpty()&&used+gap+width>available){rows.add(row);row=new ArrayList<>();used=0;}
            used+=row.isEmpty()?width:gap+width;row.add(new Cell(i,width));
        }
        if(!row.isEmpty())rows.add(row);
        return rows;
    }
    private CandidateRows(){ }
}
