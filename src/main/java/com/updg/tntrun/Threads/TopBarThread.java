package com.updg.tntrun.Threads;

import com.updg.tntrun.TNTRunPlugin;
import com.updg.tntrun.Models.enums.GameStatus;
import  com.updg.CR_API.Utils.StringUtil;
import me.confuser.barapi.BarAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Created by Alex
 * Date: 06.12.13  16:14
 */
public class TopBarThread extends Thread implements Runnable {
    public void run() {
        while (true) {
            try {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (TNTRunPlugin.game.getStatus() == GameStatus.WAITING) {
                        if (TNTRunPlugin.game.tillGame != 15)
                            BarAPI.setMessage(p, ChatColor.GREEN + "До игры" + StringUtil.plural(TNTRunPlugin.game.tillGame, " осталась " + TNTRunPlugin.game.tillGame + " секунда", " осталось " + TNTRunPlugin.game.tillGame + " секунды", " осталось " + TNTRunPlugin.game.tillGame + " секунд") + ".", (float) TNTRunPlugin.game.tillGame / ((float) TNTRunPlugin.game.tillGameDefault / 100F));
                        else if (TNTRunPlugin.game.getActivePlayers() < TNTRunPlugin.game.getMinPlayers())
                            BarAPI.setMessage(p, ChatColor.GREEN + "Ожидаем игроков.", (float) TNTRunPlugin.game.getActivePlayers() * ((float) TNTRunPlugin.game.getMinPlayers() / 100F));
                        else
                            BarAPI.setMessage(p, ChatColor.GREEN + "Ожидаем игроков.", 100F);
                    }
                    if (TNTRunPlugin.game.getStatus() == GameStatus.PRE_GAME) {
                        BarAPI.setMessage(p, ChatColor.RED + "Разбегайся! До резни" + StringUtil.plural(TNTRunPlugin.game.tillGame, " осталась " + TNTRunPlugin.game.tillGame + " секунда", " осталось " + TNTRunPlugin.game.tillGame + " секунды", " осталось " + TNTRunPlugin.game.tillGame + " секунд") + ".", (float) TNTRunPlugin.game.tillGame / (10F / 100F));
                    }
                    if (TNTRunPlugin.game.getStatus() == GameStatus.INGAME) {
                        BarAPI.setMessage(p, ChatColor.GREEN + "Бой", (float) TNTRunPlugin.game.getActivePlayers() * ((float) TNTRunPlugin.game.getMinPlayers() / 100F));
                    }
                    if (TNTRunPlugin.game.getStatus() == GameStatus.POSTGAME) {
                        BarAPI.setMessage(p, ChatColor.AQUA + "Победил " + TNTRunPlugin.game.winner.getName(), 100F);
                    }
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
