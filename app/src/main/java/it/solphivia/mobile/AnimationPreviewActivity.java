package it.solphivia.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

/** Offline animation viewer for checking every supported state on the actual device. */
public final class AnimationPreviewActivity extends Activity {
    private PetView pet;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(18,18,18,18);
        TextView title=new TextView(this);title.setText("Prova animazioni");title.setTextSize(23);page.addView(title);
        TextView help=new TextView(this);help.setText("Scegli un abito e un’azione. Gli abiti Meteo sono disponibili da ferma; le azioni usano i cicli base o cyborg. Saluto e sguardo tornano a riposo al termine.");page.addView(help);
        Spinner look=new Spinner(this),action=new Spinner(this);
        String[] looks={"Prof","VJ","Codex","Cyborg classico","Meteo pioggia","Meteo sole","Meteo freddo"};
        String[] actions={"idle","look","thinking","listen","talk","sit","sleep","happy","concerned","sad","run","wave","nod","explain","celebrate"};
        look.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,looks));
        action.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,actions));
        page.addView(look);page.addView(action);
        pet=new PetView(this);page.addView(pet,new LinearLayout.LayoutParams(-1,0,1));
        Runnable apply=()->{
            int outfit=look.getSelectedItemPosition();
            pet.setSolphiviaMode(outfit>=4?"meteorina":outfit==1?"vj":outfit==2?"codex":"prof");
            pet.setCyborgSuit(outfit==1 || outfit==3);pet.setClassicCyborgSuit(outfit==3);
            if(outfit==1)pet.setCyborgSuit(true);
            pet.setWeatherLook(outfit==4?"rain":outfit==5?"sun":outfit==6?"cold":"none");
            pet.setAnimation(actions[Math.max(0,action.getSelectedItemPosition())]);
        };
        AdapterView.OnItemSelectedListener listener=new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,android.view.View v,int n,long id){apply.run();}
            public void onNothingSelected(AdapterView<?> p){}
        };look.setOnItemSelectedListener(listener);action.setOnItemSelectedListener(listener);
        Button replay=new Button(this);replay.setText("Ripeti animazione");replay.setOnClickListener(v->apply.run());page.addView(replay);
        Switch reduced=new Switch(this);reduced.setText("Movimenti ridotti");
        reduced.setChecked(getSharedPreferences("pet",0).getBoolean("reduced_animations",false));
        reduced.setOnCheckedChangeListener((button,checked)->{getSharedPreferences("pet",0).edit().putBoolean("reduced_animations",checked).apply();apply.run();});page.addView(reduced);
        ScreenInsets.apply(this,page);setContentView(page);
    }
    @Override protected void onPause(){if(pet!=null)pet.pause();super.onPause();}
    @Override protected void onResume(){super.onResume();if(pet!=null)pet.setAnimation(pet.getSolphiviaAnimation());}
}
