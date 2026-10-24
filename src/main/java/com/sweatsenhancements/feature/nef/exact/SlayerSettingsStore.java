/* Persistence bridge for the unmodified NEF Slayer settings model (LGPL-3.0).
 * This class contains only SweatsEnhancements configuration plumbing, not Slayer feature logic. */
package com.sweatsenhancements.feature.nef.exact;
import com.sweatsenhancements.SweatsEnhancements;
import com.google.gson.Gson;

public final class SlayerSettingsStore {
    private static final String KEY="nef.slayer.settings";
    private static SlayerSettings settings=new SlayerSettings();
    private SlayerSettingsStore(){}
    public static SlayerSettings get(){return settings;}
    public static void load(){
        if(SweatsEnhancements.cfg==null)return;
        String text=SweatsEnhancements.cfg.get(KEY,"");
        if(text.isEmpty())return;
        try {
            SlayerSettings loaded=new Gson().fromJson(text,SlayerSettings.class);
            if(loaded!=null)settings=loaded;
        } catch(Exception e){System.out.println("[SweatsEnhancements] NEF Slayer config load failed: "+e);}
    }
    public static void save(){
        if(SweatsEnhancements.cfg==null)return;
        SweatsEnhancements.cfg.set(KEY,new Gson().toJson(settings));
    }
}
