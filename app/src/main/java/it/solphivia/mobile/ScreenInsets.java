package it.solphivia.mobile;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

final class ScreenInsets {
    static void apply(Activity activity, View page) {
        if(Build.VERSION.SDK_INT<30)return;
        activity.getWindow().setDecorFitsSystemWindows(false);
        activity.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        int left=page.getPaddingLeft(),top=page.getPaddingTop(),right=page.getPaddingRight(),bottom=page.getPaddingBottom();
        page.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+Math.max(bars.bottom,ime.bottom));return insets;
        });
        page.requestApplyInsets();
    }
}
