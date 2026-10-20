package app.msime.offline;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import app.msime.android.NativeClient;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** 真实ARM64 JNI与Android Button绘制回归；仅在临时cache状态中键入固定测试词。 */
public final class NopeDeviceSmoke extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    private void check(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            Context app=getTargetContext();
            for(String id:SkinPalette.IDS)for(boolean dark:new boolean[]{false,true}){
                SkinConfig config=SkinConfig.load(app,id,dark);check(config.palette(SkinPalette.of(id,dark))!=null,"skin config "+id);
            }
            result.putString("skin_contract","all9 skins, light/dark, config parsed PASS");
            CountDownLatch prepared=new CountDownLatch(1);
            String[] ready=new String[2];
            EngineInstall.prepare(app,(options,error)->{ready[0]=options;ready[1]=error;prepared.countDown();});
            check(prepared.await(120,TimeUnit.SECONDS),"resource installation timed out");
            check(ready[0]!=null,"bootstrap failed: "+ready[1]);
            java.io.File temporary=new java.io.File(app.getCacheDir(),"device-smoke-"+System.nanoTime());
            check(temporary.mkdirs(),"temporary state root");
            try{
                JSONObject options=EngineInstall.value(NativeClient.prepareHost(new JSONObject()
                    .put("resources",new java.io.File(app.getFilesDir(),"resources-v031").getAbsolutePath())
                    .put("state_root",temporary.getAbsolutePath()).toString()));
                options.put("preferences",EngineInstall.value(NativeClient.defaultPreferences()));
                options.getJSONObject("preferences").put("learning",false).put("cloud_candidates",false);
                result.putString("chinese",exercise(options,"quanpin","nihao","你好"));
                result.putString("japanese",exercise(options,"japanese","nihon","日本"));
            }finally{remove(temporary);}
            Throwable[] drawingFailure=new Throwable[1];
            runOnMainSync(()->{try{
                float density=app.getResources().getDisplayMetrics().density;
                String previousSkin=app.getSharedPreferences("offline",Context.MODE_PRIVATE).getString("skin","system");
                try{
                  for(String projectedId:new String[]{"retro","lavender"}){
                    app.getSharedPreferences("offline",Context.MODE_PRIVATE).edit().putString("skin",projectedId).commit();
                    android.widget.LinearLayout content=new android.widget.LinearLayout(app);content.setOrientation(android.widget.LinearLayout.VERTICAL);
                    View contentKey=new View(app);content.addView(contentKey,new android.widget.LinearLayout.LayoutParams(126,193));
                    IosTheme projected=new IosTheme(app);SkinStack stack=new SkinStack(app,content);stack.skin(projected,false);
                    stack.measure(View.MeasureSpec.makeMeasureSpec(126,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(193,View.MeasureSpec.EXACTLY));stack.layout(0,0,126,193);
                    check(stack.getMatrix().isIdentity()&&content.getMatrix().isIdentity()&&contentKey.getMatrix().isIdentity(),"keyboard geometry must not tilt");
                    check(stack.getChildAt(0).getRotationX()==projected.config.rotationX,"only background plane tilts");
                    check(contentKey.getWidth()==126&&contentKey.getHeight()==193,"standard key dimensions unchanged");
                    result.putString("geometry_"+projectedId,"background-only projection; keyboard identity and standard size PASS");
                  }
                }finally{app.getSharedPreferences("offline",Context.MODE_PRIVATE).edit().putString("skin",previousSkin).commit();}
                for(String label:new String[]{"Q","A","Z","0"}){
                    KeyButton key=new KeyButton(app);key.setAllCaps(false);key.setText(label);key.setTextSize(26);
                    key.setTextColor(0xff49372a);key.setSingleLine(true);key.setIncludeFontPadding(false);
                    key.setGravity(android.view.Gravity.CENTER);key.setPadding(0,0,0,0);
                    key.pixelLetters(true);key.pressFeedback(true,true);
                    int width=Math.round(36*density),height=Math.round(55*density);
                    key.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                    key.layout(0,0,width,height);
                    key.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));key.layout(0,0,width,height);
                    Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(0xffeee2c9);key.setBackgroundColor(0xffeee2c9);key.draw(new Canvas(bitmap));
                    Bitmap direct=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);PixelGlyphs.draw(new Canvas(direct),label,width/2f,height/2f,width,height/2f,0xff49372a);
                    int directInk=0;for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(direct.getPixel(x,y)==0xff49372a)directInk++;
                    result.putString("direct_"+label,"pixels="+directInk+", layout="+(key.getLayout()==null?"null":key.getLayout().getLineCount())+", willNotDraw="+key.willNotDraw());direct.recycle();
                    int ink=0;for(int y=height/4;y<height*3/4;y++)for(int x=width/5;x<width*4/5;x++)if(bitmap.getPixel(x,y)==0xff49372a)ink++;
                    try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(app.getCacheDir(),"smoke-glyph-"+label+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
                    result.putString("glyph_"+label,"ink="+ink+", size="+key.getTextSize()+", color="+Integer.toHexString(key.getCurrentTextColor())+", scroll="+key.getScrollX()+","+key.getScrollY()+", text="+key.getText().getClass());
                    bitmap.recycle();check(ink>20,"S4U letter missing: "+label+" "+result.getString("glyph_"+label));
                }
            }catch(Throwable error){drawingFailure[0]=error;}});
            if(drawingFailure[0]!=null)throw drawingFailure[0];
            result.putString("pixel_button_glyphs","Q/A/Z/0 actual Android draw PASS");
            result.putString("result","PASS");finish(android.app.Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("result","FAIL");result.putString("failure",android.util.Log.getStackTraceString(error));finish(android.app.Activity.RESULT_CANCELED,result);}
    }
    private String exercise(JSONObject base,String scheme,String input,String wanted)throws Exception{
        JSONObject options=new JSONObject(base.toString());options.getJSONObject("preferences").put("scheme",scheme);
        JSONObject created=EngineInstall.value(NativeClient.create(options.toString()));long session=created.getLong("session");
        try{
            check(new JSONObject(NativeClient.setPrivateSession(session,true)).getBoolean("ok"),"private test session");
            EngineInstall.value(NativeClient.focus(session,true));
            for(char letter:input.toCharArray())EngineInstall.value(NativeClient.character(session,letter,false));
            JSONObject view=EngineInstall.value(NativeClient.view(session));
            JSONObject all=EngineInstall.value(NativeClient.allCandidates(session));
            check(all.toString().contains(wanted),scheme+" missing "+wanted+": "+all);
            return input+" → "+wanted+" PASS";
        }finally{check(new JSONObject(NativeClient.destroy(session)).getBoolean("ok"),"destroy test session");}
    }
    private void remove(java.io.File path)throws java.io.IOException{
        // This test only calls remove with its freshly allocated cache/device-smoke root.
        java.io.File[] children=path.listFiles();if(children!=null)for(java.io.File child:children)remove(child);
        if(path.exists()&&!path.delete())throw new java.io.IOException("test cleanup failed");
    }
}
