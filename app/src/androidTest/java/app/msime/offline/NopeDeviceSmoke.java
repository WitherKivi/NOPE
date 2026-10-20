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
                verifyRecord(app,result);
                java.util.ArrayList<CandidateList.Item> largeList=new java.util.ArrayList<>();
                for(int i=0;i<10000;i++)largeList.add(new CandidateList.Item("候选"+i,"候选",()->{}));
                int[] created={0};android.graphics.Paint candidatePaint=new android.graphics.Paint();candidatePaint.setTextSize(22*density);
                CandidateList lazy=new CandidateList(app,largeList,item->{created[0]++;android.widget.Button button=new android.widget.Button(app);button.setText(item.text());return button;},candidatePaint,Math.round(48*density));
                int listWidth=Math.round(360*density),listHeight=Math.round(220*density);
                for(int pass=0;pass<2;pass++){lazy.measure(View.MeasureSpec.makeMeasureSpec(listWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(listHeight,View.MeasureSpec.EXACTLY));lazy.layout(0,0,listWidth,listHeight);}
                check(created[0]>0&&created[0]<100,"visible candidate rows must not inflate full 10k model: "+created[0]);
                result.putString("candidate_virtualization","10k candidates, visible buttons="+created[0]+" PASS");
                Bitmap settingsLight=Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888),settingsDark=Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888);
                try{
                    SettingsMaterial light=new SettingsMaterial(false,false,1),dark=new SettingsMaterial(true,false,1);light.setBounds(0,0,80,100);dark.setBounds(0,0,80,100);
                    light.draw(new Canvas(settingsLight));dark.draw(new Canvas(settingsDark));
                    check(android.graphics.Color.red(settingsLight.getPixel(40,50))>android.graphics.Color.red(settingsDark.getPixel(40,50))+100,"settings light/dark material");
                    result.putString("settings_material","milky/gray materials rendered PASS");
                }finally{settingsLight.recycle();settingsDark.recycle();}
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
                    if(projected.net!=null){
                        int capWidth=Math.round(36*density),capHeight=Math.round(55*density);
                        Bitmap numbered=Bitmap.createBitmap(capWidth,capHeight,Bitmap.Config.ARGB_8888);
                        Bitmap plain=Bitmap.createBitmap(capWidth,capHeight,Bitmap.Config.ARGB_8888);
                        try{
                            SkinKeyDrawable q=new SkinKeyDrawable(projected,projected.key,false,false,density,"Q","letter");
                            SkinKeyDrawable a=new SkinKeyDrawable(projected,projected.key,false,false,density,"A","letter");
                            q.setBounds(0,0,capWidth,capHeight);a.setBounds(0,0,capWidth,capHeight);
                            q.draw(new Canvas(numbered));a.draw(new Canvas(plain));
                            int difference=0;
                            for(int y=0;y<capHeight*.4f;y++)for(int x=capWidth/3;x<capWidth*2/3;x++)
                                if(numbered.getPixel(x,y)!=plain.getPixel(x,y))difference++;
                            check(difference>0,"NET first-row number must render in cap material");
                            for(KeyboardIcon.Kind kind:new KeyboardIcon.Kind[]{KeyboardIcon.Kind.BRAND,KeyboardIcon.Kind.CLIPBOARD,KeyboardIcon.Kind.EMOJI,KeyboardIcon.Kind.SETTINGS})
                                check(projected.net.icon(app,kind) instanceof android.graphics.drawable.BitmapDrawable,"NET image icon "+kind);
                            result.putString("net_material","shared frost, rounded font, PNG icons and top numeric legend PASS");
                        }finally{numbered.recycle();plain.recycle();}
                    }
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
    private void verifyRecord(Context app,Bundle result){
        float density=app.getResources().getDisplayMetrics().density;
        for(int heightDp:new int[]{36,48,72}){
            KeyboardIcon icon=new KeyboardIcon(app,KeyboardIcon.Kind.BRAND);
            icon.setText("");icon.setTextSize(30);icon.setSingleLine(true);
            icon.setPadding(2,0,2,0);icon.pixelLetters(true);icon.textScale(2);
            icon.retroBrand(true);icon.glyphColor(0xffff00ff);
            icon.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(0xffff00ff));
            icon.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            check(icon.getCompoundDrawables()[0]==null,"S4U record must not enter text layout");
            int width=Math.round(44*density),height=Math.round(heightDp*density);
            android.widget.FrameLayout parent=new android.widget.FrameLayout(app);
            parent.addView(icon,new android.widget.FrameLayout.LayoutParams(width,height));
            parent.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            parent.layout(0,0,width,height);
            Bitmap normal=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            Bitmap scrolled=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            try{
                parent.draw(new Canvas(normal));
                // Parent.draw applies View's content scroll transform, unlike a
                // direct icon.onDraw call. Reproduce stale single-line scroll.
                icon.scrollTo(Math.round(19*density),Math.round(-7*density));
                icon.setPressed(true);parent.draw(new Canvas(scrolled));
                int left=width,right=-1,top=height,bottom=-1,screen=0,disc=0;
                for(int y=0;y<height;y++)for(int x=0;x<width;x++){
                    int pixel=normal.getPixel(x,y);
                    check(pixel==scrolled.getPixel(x,y),"S4U record moved/clipped after scroll at "+heightDp+"dp");
                    if(android.graphics.Color.alpha(pixel)>0){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
                    if(pixel==0xff35483d)screen++;
                    if(pixel==0xffa8aba2)disc++;
                }
                check(screen>0&&disc>0,"S4U full-color disc and screen missing");
                check(left>0&&top>0&&right<width-1&&bottom<height-1,"S4U record clipped");
                check(Math.abs(left+right-(width-1))<=2&&Math.abs(top+bottom-(height-1))<=2,"S4U record not centered");
                boolean[] clicked={false};icon.setOnClickListener(v->clicked[0]=true);icon.performClick();
                check(clicked[0],"S4U record tools action");
                icon.retroBrand(false);check(icon.getCompoundDrawables()[0]!=null,"normal brand must restore");
            }finally{normal.recycle();scrolled.recycle();}
        }
        result.putString("s4u_record","36/48/72dp, centered full-color record, scroll/tint/press and tools action PASS");
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
