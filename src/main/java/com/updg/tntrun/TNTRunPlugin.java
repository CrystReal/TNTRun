package com.updg.tntrun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.updg.CR_API.MQ.senderStatsToCenter;
import com.updg.tntrun.DataServerStats.gameStats;
import com.updg.tntrun.DataServerStats.playerStats;
import com.updg.tntrun.Models.TNTPlayer;
import com.updg.tntrun.Models.enums.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alex
 * Date: 17.06.13 18:07
 */
public class TNTRunPlugin extends JavaPlugin {
    public static String prefix = ChatColor.BOLD + "" + ChatColor.DARK_AQUA + "[TNT Run] " + ChatColor.RESET;
    public static String prefixMoney = ChatColor.BOLD + "" + ChatColor.GOLD;
    public static Game game;
    private static TNTRunPlugin instance;
    public int serverId = 0;
    public long destroyLatency;
    public List<Integer> levels = new ArrayList<Integer>();

    public static TNTRunPlugin getInstance() {
        return instance;
    }

    public void onEnable() {
        TNTRunPlugin.instance = this;
        this.serverId = getConfig().getInt("serverId", 0);
        this.destroyLatency = getConfig().getLong("destroyLatency", 20);
        this.levels = getConfig().getIntegerList("levels");

        getServer().getPluginManager().registerEvents(new Events(), this);
        game = new Game();
        game.getReady();
    }

    public void onDisable() {
        game.setStatus(GameStatus.RELOAD);
        game.sendUpdatesToLobby();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String CommandLabel, String[] args) {
        TNTPlayer p;
        if (sender instanceof Player) {
            p = game.getPlayer(sender.getName());
        } else {
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("spectate")) {
            if (p.getBukkitModel().hasPermission("dk.spectate")) {
                if (p.isSpectator() || (game.getStatus() == GameStatus.INGAME || game.getStatus() == GameStatus.POSTGAME)) {
                    p.sendMessage("Нельзя менять статус во время игры.");
                    return false;
                }
                if (p.isSpectator() && game.getActivePlayers() >= game.getMaxPlayers()) {
                    p.sendMessage("Ошибка смены статуса. Сервер полный.");
                    return false;
                }
                if (p.isSpectator()) {
                    p.setSpectator(false);
                    game.removeSpectator(p);
                    game.addPlayer(p);
                    p.sendMessage("Теперь ты обычный игрок");
                } else {
                    p.setSpectator(true);
                    game.removePlayer(p);
                    game.addSpectator(p);
                    p.sendMessage("Теперь ты наблюдающий");
                }
            } else {
                p.sendMessage(ChatColor.RED + "Не достаточно прав");
            }
        }
        return false;
    }

    public Location stringToLoc(String string) {
        String[] loc = string.split("\\|");
        World world = Bukkit.getWorld(loc[0]);
        Double x = Double.parseDouble(loc[1]);
        Double y = Double.parseDouble(loc[2]);
        Double z = Double.parseDouble(loc[3]);

        return new Location(world, x, y, z);
    }

    public void sendStats() {
        gameStats game = new gameStats();
        game.setServerId(this.serverId);
        game.setWinner(TNTRunPlugin.game.winner.getId());
        game.setStart(TNTRunPlugin.game.getTimeStart());
        game.setEnd(TNTRunPlugin.game.getTimeEnd());
        List<playerStats> players = new ArrayList<playerStats>();
        playerStats tmpPlayer;
        for (TNTPlayer p : TNTRunPlugin.game.getActivePlayersArray()) {
            tmpPlayer = new playerStats();
            tmpPlayer.setPlayerId(p.getId());
            tmpPlayer.setIsWinner(p.getId() == game.getWinner());
            tmpPlayer.setTimeInGame(p.getStats().getInGameTime());
            tmpPlayer.setPosition(p.getStats().getPosition());
            players.add(tmpPlayer);
        }
        for (TNTPlayer p : TNTRunPlugin.game.getSpectatorsArray()) {
            if (p.wasInGame()) {
                tmpPlayer = new playerStats();
                tmpPlayer.setPlayerId(p.getId());
                tmpPlayer.setIsWinner(p.getId() == game.getWinner());
                tmpPlayer.setTimeInGame(p.getStats().getInGameTime());
                tmpPlayer.setPosition(p.getStats().getPosition());
                players.add(tmpPlayer);
            }
        }
        game.setPlayers(players);
        try {
            String stat = new ObjectMapper().writeValueAsString(game);
            senderStatsToCenter.send("tntrun", stat);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
