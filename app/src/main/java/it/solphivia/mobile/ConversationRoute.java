package it.solphivia.mobile;

import android.content.Context;

/** Existing ChatGPT history stays intact; a new connection starts with a separate history. */
final class ConversationRoute {
    static String historyKey(Context context) {
        if(DemoAccess.enabled(context))return "history_demo_"+context.getSharedPreferences("demo",0).getString("conversation","initial");
        if(ModelConnection.active(context)) {
            try{return "history_personal_"+ModelConnection.read(context).revision;}
            catch(Exception e){return "history_unavailable";}
        }
        return "history";
    }
    private ConversationRoute(){}
}
