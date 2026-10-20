package app.msime.offline;

import android.content.Context;
import android.graphics.Color;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 统一皮肤v1：数据控制色板、按键区域、透视与装饰；不包含脚本和网络。 */
final class SkinConfig {
    static final int MAX_BYTES=65536;
    private final JSONObject document,variant;
    final List<JSONObject> layers=new ArrayList<>();
    final float rotationX,cameraFactor;
    SkinConfig(JSONObject document,boolean dark)throws JSONException{
        validate(document);this.document=document;
        JSONObject variants=document.optJSONObject("variants");
        JSONObject chosen=variants==null?null:variants.optJSONObject(dark?"dark":"light");
        variant=chosen==null?new JSONObject():chosen;
        JSONObject perspective=document.optJSONObject("perspective");
        rotationX=perspective==null?0:(float)perspective.optDouble("rotationX",0);
        cameraFactor=perspective==null?2:(float)perspective.optDouble("cameraHeightFactor",2);
        JSONArray array=document.optJSONArray("layers");
        if(array!=null)for(int i=0;i<array.length();i++)layers.add(array.getJSONObject(i));
        layers.sort(Comparator.comparingInt(layer->layer.optInt("order",0)));
    }
    static SkinConfig load(Context context,String id,boolean dark){
        try{
            File override=file(context,id);
            if(override.isFile())try(InputStream in=new FileInputStream(override)){return new SkinConfig(new JSONObject(read(in)),dark);}
        }catch(Exception error){android.util.Log.w("NopeSkin","Ignoring invalid override",error);}
        try(InputStream in=context.getAssets().open("skins/"+id+".json")){return new SkinConfig(new JSONObject(read(in)),dark);}
        catch(Exception error){throw new IllegalStateException("Bundled skin contract is invalid: "+id,error);}
    }
    SkinPalette palette(SkinPalette base){
        Map<String,Integer> colors=new HashMap<>();JSONObject palette=variant.optJSONObject("palette");
        if(palette!=null)for(String key:names(palette))colors.put(key,Color.parseColor(palette.optString(key)));
        return base.configured(colors,variant.optInt("radiusDp",base.radius),variant.optInt("depthDp",base.depth));
    }
    int keyColor(String role,String token,int fallback){JSONObject keys=document.optJSONObject("keys"),style=keys==null?null:keys.optJSONObject(role);return style==null||!style.has(token)?fallback:Color.parseColor(style.optString(token));}
    int keySize(String role,String token,int fallback){JSONObject keys=document.optJSONObject("keys"),style=keys==null?null:keys.optJSONObject(role);return style==null?fallback:style.optInt(token,fallback);}
    private static java.util.List<String> names(JSONObject json){java.util.List<String> values=new ArrayList<>();java.util.Iterator<String> iterator=json.keys();while(iterator.hasNext())values.add(iterator.next());return values;}
    String keyFont(String role){JSONObject keys=document.optJSONObject("keys"),style=keys==null?null:keys.optJSONObject(role);return style==null?"":style.optString("font","");}
    float keyScale(String role){JSONObject keys=document.optJSONObject("keys"),style=keys==null?null:keys.optJSONObject(role);return style==null?1:(float)style.optDouble("textScale",1);}
    static File file(Context context,String id){if(!Arrays.asList(SkinPalette.IDS).contains(id))throw new IllegalArgumentException("未知皮肤ID");return new File(new File(context.getFilesDir(),"skins"),id+".json");}
    static String read(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int size;
        while((size=in.read(buffer))!=-1){if(out.size()+size>MAX_BYTES)throw new IOException("皮肤配置最大64KB");out.write(buffer,0,size);}
        return out.toString(StandardCharsets.UTF_8.name());
    }
    static void validate(JSONObject json)throws JSONException{
        if(json.getInt("version")!=1||!Arrays.asList(SkinPalette.IDS).contains(json.getString("id")))throw new IllegalArgumentException("不支持的皮肤版本或ID");
        JSONObject variants=json.optJSONObject("variants");
        if(variants!=null)for(String branch:names(variants)){
            JSONObject variant=variants.getJSONObject(branch),colors=variant.optJSONObject("palette");
            if(colors!=null)for(String color:names(colors))Color.parseColor(colors.getString(color));
            range(variant,"radiusDp",0,32);range(variant,"depthDp",0,12);
        }
        JSONObject keys=json.optJSONObject("keys");
        if(keys!=null)for(String role:names(keys)){
            if(!Set.of("letter","number","function","space","mode","candidate","toolbar","other").contains(role))throw new IllegalArgumentException("未知按键区域");
            JSONObject key=keys.getJSONObject(role);for(String color:new String[]{"fill","ink"})if(key.has(color))Color.parseColor(key.getString(color));
            range(key,"radiusDp",0,32);range(key,"depthDp",0,12);range(key,"textScale",.6,1.8);
            if(key.has("font")&&!Set.of("system","pixel").contains(key.getString("font")))throw new IllegalArgumentException("不支持的字体");
        }
        JSONObject perspective=json.optJSONObject("perspective");if(perspective!=null){
            if(perspective.has("target")&&!perspective.getString("target").equals("background"))throw new IllegalArgumentException("透视只能用于背景，不能改变键盘尺寸");
            range(perspective,"rotationX",-12,12);range(perspective,"cameraHeightFactor",1.5,8);}
        JSONArray layers=json.optJSONArray("layers");if(layers==null)return;
        if(layers.length()>64)throw new IllegalArgumentException("最多64个装饰元素");
        Set<String> ids=new HashSet<>();
        for(int i=0;i<layers.length();i++){
            JSONObject layer=layers.getJSONObject(i);
            if(!ids.add(layer.getString("id")))throw new IllegalArgumentException("装饰ID重复");
            if(layer.getInt("z")!=5&&layer.getInt("z")!=30)throw new IllegalArgumentException("装饰层级须为5或30");
            if(!Set.of("frame","header","body","keys","footer","key:letter","key:number","key:function","key:space","key:mode","key:candidate","key:toolbar","key:other").contains(layer.getString("region")))throw new IllegalArgumentException("未知装饰区域");
            if(!Set.of("rect","text","dots","rivet").contains(layer.getString("type")))throw new IllegalArgumentException("未知装饰类型");
            range(layer,"x",0,1);range(layer,"y",0,1);range(layer,"width",0,1);range(layer,"height",0,1);
            range(layer,"widthDp",0,512);range(layer,"heightDp",0,512);range(layer,"textSp",6,48);range(layer,"stepDp",3,64);
            if(layer.optString("text").length()>80)throw new IllegalArgumentException("装饰文字过长");
            if(layer.has("color")){String color=layer.getString("color");
                if(color.startsWith("@")&&!Set.of("@foreground","@key","@accent","@shadow","@outline").contains(color))throw new IllegalArgumentException("未知颜色变量");
                if(!color.startsWith("@"))Color.parseColor(color);
            }
        }
    }
    private static void range(JSONObject value,String key,double min,double max)throws JSONException{if(value.has(key)){double n=value.getDouble(key);if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException(key+"超出范围");}}
}
