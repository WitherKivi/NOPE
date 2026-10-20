package app.msime.offline;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.JSONObject;

/** Shared stack:0 background,5 underlay,10 keycaps/20 labels,30 overlays;40 feedback lives in non-touch popups. */
final class SkinStack extends FrameLayout {
    private final View background;
    private final LinearLayout content;
    private final Decorations below,above;
    private IosTheme theme;
    private View header,body,keys,footer;
    public SkinStack(Context context){this(context,new LinearLayout(context));}
    SkinStack(Context context,LinearLayout content){
        super(context);this.content=content;
        background=new View(context);below=new Decorations(context,5);above=new Decorations(context,30);
        addView(background);addView(below);addView(content);addView(above);
        below.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);above.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClipChildren(false);
    }
    void skin(IosTheme theme,boolean blur){
        this.theme=theme;background.setBackground(theme.material(blur));
        setBackgroundColor(theme.background);applyBackgroundPerspective();below.invalidate();above.invalidate();
    }
    private void applyBackgroundPerspective(){
        if(theme==null)return;
        // Only the non-interactive cabinet plane is projected. Keyboard/LCD bounds stay standard.
        background.setPivotX(getWidth()/2f);background.setPivotY(getHeight());
        background.setCameraDistance(Math.max(500*getResources().getDisplayMetrics().density,getHeight()*theme.config.cameraFactor));
        background.setRotationX(theme.config.rotationX);
    }
    void regions(View header,View body,View keys,View footer){this.header=header;this.body=body;this.keys=keys;this.footer=footer;below.invalidate();above.invalidate();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        content.measure(widthSpec,heightSpec);setMeasuredDimension(content.getMeasuredWidth(),content.getMeasuredHeight());
        int w=MeasureSpec.makeMeasureSpec(getMeasuredWidth(),MeasureSpec.EXACTLY),h=MeasureSpec.makeMeasureSpec(getMeasuredHeight(),MeasureSpec.EXACTLY);
        background.measure(w,h);below.measure(w,h);above.measure(w,h);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        for(int i=0;i<getChildCount();i++)getChildAt(i).layout(0,0,r-l,b-t);
        applyBackgroundPerspective();
    }
    private RectF bounds(View view){
        RectF area=new RectF(0,0,view.getWidth(),view.getHeight());View current=view;
        while(current!=this&&current.getParent() instanceof View parent){current.getMatrix().mapRect(area);area.offset(current.getLeft()-parent.getScrollX(),current.getTop()-parent.getScrollY());current=parent;}
        return current==this?area:new RectF();
    }
    private void roleViews(View view,String role,List<RectF> found){
        if(view.getVisibility()!=VISIBLE)return;
        if(view instanceof KeyButton key&&key.skinRole().equals(role))found.add(bounds(view));
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)roleViews(group.getChildAt(i),role,found);
    }
    private List<RectF> region(String name){
        if(name.startsWith("key:")){List<RectF> found=new ArrayList<>();roleViews(content,name.substring(4),found);return found;}
        View view=switch(name){case "header"->header;case "body"->body;case "keys"->keys;case "footer"->footer;default->this;};
        if(view==null||view.getVisibility()!=VISIBLE)return List.of();
        return List.of(view==this?new RectF(0,0,getWidth(),getHeight()):bounds(view));
    }
    private int color(String token){return switch(token){case "@foreground"->theme.foreground;case "@key"->theme.key;case "@accent"->theme.accent;case "@shadow"->theme.shadow;case "@outline"->theme.palette.outline;default->Color.parseColor(token);};}
    private final class Decorations extends View {
        private final int z;
        private final Paint paint=new Paint();
        Decorations(Context context,int z){super(context);this.z=z;setClickable(false);setFocusable(false);}
        @Override protected void onDraw(Canvas canvas){
            if(theme==null)return;
            float d=getResources().getDisplayMetrics().density;
            for(JSONObject element:theme.config.layers){
                if(element.optInt("z")!=z)continue;
                paint.setColor(color(element.optString("color","@outline")));paint.setStyle(Paint.Style.FILL);
                for(RectF area:region(element.optString("region","frame"))){
                    int save=canvas.save();
                    if(element.optString("region").equals("frame"))canvas.concat(background.getMatrix());
                    canvas.clipRect(area);
                    float x=area.left+(float)element.optDouble("x",0)*area.width(),y=area.top+(float)element.optDouble("y",0)*area.height();
                    float w=element.has("widthDp")?(float)element.optDouble("widthDp")*d:(float)element.optDouble("width",1)*area.width();
                    float h=element.has("heightDp")?(float)element.optDouble("heightDp")*d:(float)element.optDouble("height",1)*area.height();
                    switch(element.optString("type")){
                        case "rect"->canvas.drawRect(x,y,x+w,y+h,paint);
                        case "rivet"->{canvas.drawRect(x+d,y,x+w-d,y+h,paint);canvas.drawRect(x,y+d,x+w,y+h-d,paint);paint.setColor(theme.key);canvas.drawRect(x+d,y+d,x+w-d,y+h-d,paint);paint.setColor(theme.palette.outline);canvas.drawRect(x+w/2-d/2,y+d,x+w/2+d/2,y+h-d,paint);}
                        case "text"->{paint.setTypeface(theme.palette.id.equals("retro")?RetroTypeface.get(getContext()):Typeface.DEFAULT);paint.setTextSize((float)element.optDouble("textSp",12)*getResources().getDisplayMetrics().scaledDensity);canvas.drawText(element.optString("text"),x,y-paint.ascent(),paint);}
                        case "dots"->{float step=(float)element.optDouble("stepDp",6)*d,p=Math.max(1,Math.round(d));for(float yy=y;yy<y+h;yy+=step)for(float xx=x;xx<x+w;xx+=step)canvas.drawRect(xx,yy,xx+p,yy+p,paint);}
                    }
                    canvas.restoreToCount(save);
                }
            }
        }
    }
}
