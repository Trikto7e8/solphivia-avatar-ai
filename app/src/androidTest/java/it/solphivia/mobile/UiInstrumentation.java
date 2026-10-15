package it.solphivia.mobile;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.io.*;

/** Renders our synthetic test installation only, never the personal app's views. */
public final class UiInstrumentation extends Instrumentation {
    @Override public void callActivityOnCreate(Activity activity,Bundle state){super.callActivityOnCreate(activity,state);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            Context context=getTargetContext();RevisionChecks.check(context.getPackageName().endsWith(".verification"),"isolated UI only");
            for(Class<?> type:new Class<?>[]{MainActivity.class,VoiceSettingsActivity.class,AnimationPreviewActivity.class,MemoryActivity.class}){
                Activity screen=startActivitySync(new Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();android.os.SystemClock.sleep(1500);
                runOnMainSync(()->{
                    try{
                        View root=screen.getWindow().getDecorView();Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
                        try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),type.getSimpleName()+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
                    }catch(Exception e){throw new AssertionError(e);}
                    screen.finish();
                });waitForIdleSync();
            }
            result.putString("stream","PASS: rendered Main, Voice, Animation and Memory screens with synthetic data\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
}
