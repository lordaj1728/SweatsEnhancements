package net.minecraft.launchwrapper;

import java.util.HashMap;
import java.util.Map;

public class Launch {
    public static Map<String, Object> blackboard = new HashMap<String, Object>();
    static { blackboard.put("fml.deobfuscatedEnvironment", Boolean.FALSE); }
    public static void main(String[] args) { }
}
