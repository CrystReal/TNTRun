package com.updg.tntrun.Utils;

import com.updg.tntrun.TNTRunPlugin;

import java.util.logging.Level;

/**
 * Created by Alex
 * Date: 15.12.13  13:17
 */
public class L {
    public static void $(String str) {
        TNTRunPlugin.getInstance().getLogger().log(Level.INFO, str);
    }

    public static void $(Level l, String str) {
        TNTRunPlugin.getInstance().getLogger().log(l, str);
    }
}
