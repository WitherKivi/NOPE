package app.msime.offline;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import org.json.JSONObject;

/** 用户主动选择的纯JSON配置写入应用私有目录；不执行代码、不读取外部路径。 */
final class SkinFiles {
    static String install(Context context,Uri uri)throws Exception{
        JSONObject json;try(InputStream input=context.getContentResolver().openInputStream(uri)){
            if(input==null)throw new IOException("无法读取配置文件");json=new JSONObject(SkinConfig.read(input));
        }
        SkinConfig.validate(json);String id=json.getString("id");File target=SkinConfig.file(context,id);
        if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("无法保存皮肤");
        File part=new File(target.getPath()+".part");
        try(FileOutputStream out=new FileOutputStream(part)){out.write(json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}
        java.nio.file.Files.move(part.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        context.getSharedPreferences("offline",Context.MODE_PRIVATE).edit().putString("skin",id).putLong("skin_revision",android.os.SystemClock.elapsedRealtime()).apply();
        return SkinPalette.NAMES[SkinPalette.index(id)];
    }
    static void reset(Context context)throws IOException{
        var preferences=context.getSharedPreferences("offline",Context.MODE_PRIVATE);String id=preferences.getString("skin","system");File target=SkinConfig.file(context,id);
        if(target.exists()&&!target.delete())throw new IOException("无法恢复皮肤");
        preferences.edit().putLong("skin_revision",android.os.SystemClock.elapsedRealtime()).apply();
    }
    private SkinFiles(){ }
}
