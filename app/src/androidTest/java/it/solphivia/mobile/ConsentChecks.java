package it.solphivia.mobile;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.*;

final class ConsentChecks {
    static void run(Instrumentation test)throws Exception{
        Context context=test.getTargetContext();
        AtomicInteger accepted=new AtomicInteger(),cancelled=new AtomicInteger();
        AtomicReference<String> sent=new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor=test.addMonitor(MemoryConsentActivity.class.getName(),null,false);
        MemoryConsentActivity.request(context,"user: test only","memory test",value->{accepted.incrementAndGet();sent.set(value);},cancelled::incrementAndGet);
        Activity first=monitor.waitForActivityWithTimeout(5000);RevisionChecks.check(first!=null,"memory consent visible");
        test.runOnMainSync(first::onBackPressed);test.waitForIdleSync();
        RevisionChecks.check(cancelled.get()==1&&accepted.get()==0,"cancel does not grant context");
        test.removeMonitor(monitor);
        monitor=test.addMonitor(MemoryConsentActivity.class.getName(),null,false);
        MemoryConsentActivity.request(context,"user: second test","memory test",value->{accepted.incrementAndGet();sent.set(value);},cancelled::incrementAndGet);
        Activity second=monitor.waitForActivityWithTimeout(5000);RevisionChecks.check(second!=null,"second memory consent visible");
        test.runOnMainSync(()->{
            Button no=find(second.getWindow().getDecorView(),"Invia senza memoria");RevisionChecks.check(no!=null,"skip memory action");no.performClick();no.performClick();
        });test.waitForIdleSync();
        RevisionChecks.check(accepted.get()==1&&"".equals(sent.get())&&cancelled.get()==1,"single-use approval and no-memory send: accepted="+accepted.get()+", cancelled="+cancelled.get()+", text="+sent.get());
        test.removeMonitor(monitor);
    }
    private static Button find(View v,String title){if(v instanceof Button&&((Button)v).getText().toString().equals(title))return (Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button result=find(g.getChildAt(i),title);if(result!=null)return result;}}return null;}
}
