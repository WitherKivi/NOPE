package app.msime.offline;

import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.List;

/** 原生可回收候选行，长候选换行；不创建完整词库的View树。 */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic view; never inflated from XML.
final class CandidateList extends ListView {
    record Item(String text,String description,Runnable action) { }
    private final List<Item> items;
    private final java.util.function.Function<Item,Button> factory;
    private final int[] widths;
    private final int gap;
    private List<List<CandidateRows.Cell>> rows=List.of();
    private final BaseAdapter adapter=new BaseAdapter(){
        public int getCount(){return rows.size();}
        public Object getItem(int position){return rows.get(position);}
        public long getItemId(int position){return position;}
        public boolean areAllItemsEnabled(){return false;}
        public boolean isEnabled(int position){return false;}
        public View getView(int position,View recycled,ViewGroup parent){
            LinearLayout row=recycled instanceof LinearLayout existing?existing:new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);row.removeAllViews();
            List<CandidateRows.Cell> cells=rows.get(position);
            for(int i=0;i<cells.size();i++){
                CandidateRows.Cell cell=cells.get(i);Button button=factory.apply(items.get(cell.index()));
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(cell.width(),-2);
                if(i>0)params.leftMargin=gap;row.addView(button,params);
            }
            return row;
        }
    };
    CandidateList(Context context,List<Item> items,java.util.function.Function<Item,Button> factory,Paint textPaint,int minimumHeight){
        super(context);this.items=List.copyOf(items);this.factory=factory;
        float d=getResources().getDisplayMetrics().density;gap=Math.round(4*d);widths=new int[items.size()];
        for(int i=0;i<items.size();i++)widths[i]=Math.max(minimumHeight,(int)Math.ceil(textPaint.measureText(items.get(i).text()))+Math.round(32*d));
        setPadding(gap,gap,gap,gap);setClipToPadding(false);setDivider(null);setDividerHeight(gap);
        setSelector(android.R.color.transparent);setItemsCanFocus(true);setAdapter(adapter);
        setContentDescription("全部候选，上下滑动浏览");
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(w!=oldw){rows=CandidateRows.pack(widths,w-getPaddingLeft()-getPaddingRight(),gap);adapter.notifyDataSetChanged();}
    }
}
