package app.msime.offline;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

final class ClipboardSettings {
    static String summary(Context context) {
        long value=context.getSharedPreferences("offline",Context.MODE_PRIVATE).getLong("clipboard_retention",0);
        return value==0?"不自动清除":value/60000+" 分钟后清除未置顶条目";
    }
    static void show(Context context, Runnable updated) {
        new AlertDialog.Builder(context).setTitle("剪贴板保留时间")
            .setItems(new String[]{"不自动清除","15 分钟","1 小时","24 小时","7 天","自定义分钟数"},(dialog,index)->{
                if(index==5) {
                    EditText minutes=new EditText(context); minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    minutes.setHint("1–525600 分钟");
                    AlertDialog custom=new AlertDialog.Builder(context).setTitle("自定义保留时间").setView(minutes)
                        .setPositiveButton("保存",null).setNegativeButton("取消",null).create();
                    custom.setOnShowListener(d->custom.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                        try {save(context,ClipboardRetention.minutes(minutes.getText().toString()),updated);custom.dismiss();}
                        catch(IllegalArgumentException error) {minutes.setError("请输入 1–525600 分钟");}
                    })); custom.show();
                } else save(context,new long[]{0,15,60,1440,10080}[index]*60000,updated);
            }).setNegativeButton("取消",null).show();
    }
    private static void save(Context context,long value,Runnable updated) {
        context.getSharedPreferences("offline",Context.MODE_PRIVATE).edit().putLong("clipboard_retention",value).apply(); updated.run();
    }
}
