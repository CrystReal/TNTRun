package com.updg.tntrun;

import com.updg.CR_API.MQ.senderUpdatesToCenter;
import com.updg.tntrun.Models.TNTPlayer;
import com.updg.tntrun.Models.enums.GameStatus;
import com.updg.tntrun.Threads.TopBarThread;
import com.updg.tntrun.Utils.EconomicSettings;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Alex
 * Date: 17.06.13  20:26
 */
public class Game {
    private HashMap<String, TNTPlayer> players = new HashMap<String, TNTPlayer>();
    private HashMap<String, TNTPlayer> spectators = new HashMap<String, TNTPlayer>();

    private Location lobby;
    private Location spawn;
    private GameStatus status;
    private int minPlayers = 0;
    private int maxPlayers = 12;

    public int tillGameDefault = 15;
    public int tillGame = 15;

    public TNTPlayer winner;
    private int tid;

    private long timeStart = 0;
    private long timeEnd = 0;

    public ArrayList<Location> toRemove = new ArrayList<Location>();

    public Game() {
        this.lobby = TNTRunPlugin.getInstance().stringToLoc(TNTRunPlugin.getInstance().getConfig().getString("lobby"));
        this.spawn = TNTRunPlugin.getInstance().stringToLoc(TNTRunPlugin.getInstance().getConfig().getString("spawn"));
        this.minPlayers = TNTRunPlugin.getInstance().getConfig().getInt("minPlayers");
        this.maxPlayers = TNTRunPlugin.getInstance().getConfig().getInt("maxPlayers");
    }

    public void sendUpdatesToLobby() {
        String s = GameStatus.WAITING.toString();
        if (TNTRunPlugin.game.getMaxPlayers() <= TNTRunPlugin.game.getActivePlayers())
            s = "IN_GAME";
        if (TNTRunPlugin.game.getStatus() == GameStatus.WAITING) {
            if (TNTRunPlugin.game.tillGame < TNTRunPlugin.game.tillGameDefault)
                senderUpdatesToCenter.send(TNTRunPlugin.getInstance().serverId + ":" + s + ":" + "В ОЖИДАНИИ" + ":" + TNTRunPlugin.game.getActivePlayers() + ":" + TNTRunPlugin.game.getMaxPlayers() + ":До игры " + TNTRunPlugin.game.tillGame + " c.");
            else
                senderUpdatesToCenter.send(TNTRunPlugin.getInstance().serverId + ":" + s + ":" + "В ОЖИДАНИИ" + ":" + TNTRunPlugin.game.getActivePlayers() + ":" + TNTRunPlugin.game.getMaxPlayers() + ":Набор игроков");
        } else if (TNTRunPlugin.game.getStatus() == GameStatus.PRE_GAME)
            senderUpdatesToCenter.send(TNTRunPlugin.getInstance().serverId + ":IN_GAME:" + "НАЧАЛО" + ":" + TNTRunPlugin.game.getActivePlayers() + ":" + TNTRunPlugin.game.getMaxPlayers());
        else if (TNTRunPlugin.game.getStatus() == GameStatus.POSTGAME) {
            senderUpdatesToCenter.send(TNTRunPlugin.getInstance().serverId + ":IN_GAME:" + "ИГРА ОКОНЧЕНА" + ":" + TNTRunPlugin.game.getActivePlayers() + ":" + TNTRunPlugin.game.getMaxPlayers() + ":Победил " + TNTRunPlugin.game.winner.getName());
        } else if (TNTRunPlugin.game.getStatus() == GameStatus.INGAME || TNTRunPlugin.game.getStatus() == GameStatus.POSTGAME)
            senderUpdatesToCenter.send(TNTRunPlugin.getInstance().serverId + ":IN_GAME:" + "ИГРА" + ":" + TNTRunPlugin.game.getActivePlayers() + ":" + TNTRunPlugin.game.getMaxPlayers() + ":Бой");
        else if (TNTRunPlugin.game.getStatus() == GameStatus.RELOAD)
            senderUpdatesToCenter.send(TNTRunPlugin.getInstance().serverId + ":DISABLED:" + "ОФФЛАЙН" + ":0:0:");

    }

    public boolean isAbleToStart() {
        return this.status == GameStatus.WAITING && this.players.size() >= this.minPlayers;
    }

    public void getReady() {
        this.status = GameStatus.WAITING;
        new TopBarThread().start();
        sendUpdatesToLobby();
    }

    public void preGame() {
        if (tid == 0)
            tid = Bukkit.getScheduler().scheduleSyncRepeatingTask(TNTRunPlugin.getInstance(), new Runnable() {
                public void run() {
                    if (Bukkit.getOnlinePlayers().length < getMinPlayers()) {
                        Bukkit.broadcastMessage(TNTRunPlugin.prefix + "Старт игры отменен так как игрок(и) покинули сервер.");
                        sendUpdatesToLobby();
                        Bukkit.getScheduler().cancelTask(tid);
                        tid = 0;
                        tillGame = tillGameDefault;
                    } else if (tillGame > 0) {
                        tillGame--;
                    } else {
                        Bukkit.getScheduler().cancelTask(tid);
                        TNTRunPlugin.game.startGame();
                    }
                }
            }, 0, 20);
    }

    public void startGame() {
        if (status == GameStatus.PRE_GAME)
            return;
        this.status = GameStatus.PRE_GAME;
        for (TNTPlayer p : this.players.values()) {
            p.getBukkitModel().setGameMode(GameMode.SURVIVAL);
            p.getBukkitModel().setFlying(false);
            p.getBukkitModel().setAllowFlight(false);
            p.getBukkitModel().teleport(getSpawn());
            p.getBukkitModel().getInventory().clear();
            p.setWasInGame(true);
        }
        Bukkit.broadcastMessage(TNTRunPlugin.prefix + ChatColor.RED + "Разбегайся! До начала резни всего 10 секунд!");
        this.tillGame = 10;
        tid = Bukkit.getScheduler().scheduleSyncRepeatingTask(TNTRunPlugin.getInstance(), new Runnable() {
            public void run() {
                tillGame--;
                if (tillGame == 0) {
                    Bukkit.broadcastMessage(TNTRunPlugin.prefix + ChatColor.RED + "БОЙ!");
                    timeStart = System.currentTimeMillis() / 1000;
                    status = GameStatus.INGAME;
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(TNTRunPlugin.getInstance(), new Runnable() {
                        public void run() {
                            if (getStatus() == GameStatus.INGAME)
                                for (TNTPlayer p : players.values()) {
                                    if (!p.isSpectator()) {
                                        Block tmp = p.getBukkitModel().getLocation().getBlock();
                                        Location plufloc = tmp.getRelative(BlockFace.DOWN).getLocation();
                                        if (TNTRunPlugin.game.isForDestroy(plufloc)) {
                                            TNTRunPlugin.game.destroyWithLatency(plufloc);
                                        }
                                    }
                                }
                        }
                    }, 0, 1);
                    Bukkit.getScheduler().cancelTask(tid);
                }
            }
        }, 0, 20);
        sendUpdatesToLobby();
    }

    public Location getSpawn() {
        spawn.setPitch(12);
        spawn.setYaw(-90);
        return spawn;
    }

    public void endGame() {
        if (this.status == GameStatus.INGAME && this.players.size() < 2) {
            this.timeEnd = System.currentTimeMillis() / 1000L;
            for (TNTPlayer p : this.players.values()) {
                p.sendMessage(TNTRunPlugin.prefix + "Ты выиграл бой!");
                p.sendMessage(TNTRunPlugin.prefixMoney + "+" + EconomicSettings.win + " опыта.");
                p.getStats().setInGameTime(System.currentTimeMillis() / 1000L - this.timeStart);
                winner = p;
                winner.addExp(EconomicSettings.win);
            }
            for (Player p1 : Bukkit.getOnlinePlayers()) {
                if (winner != null && !p1.getName().equals(winner.getName())) {
                    p1.sendMessage(TNTRunPlugin.prefix + "Игрок " + winner.getName() + " выиграл!");
                }
            }
        } else {
            for (TNTPlayer p : this.players.values()) {
                p.sendMessage(TNTRunPlugin.prefix + "Игра остановлена системой.");
            }
        }
        this.status = GameStatus.POSTGAME;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(TNTRunPlugin.prefix + "Сервер перезагрузится через 15 секунд.");
            p.getInventory().clear();
        }
        new Thread(
                new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage(TNTRunPlugin.prefix + "Сервер перезагрузится через 10 секунд.");
                            }
                            Thread.sleep(5000);
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage(TNTRunPlugin.prefix + "Сервер перезагрузится через 5 секунд.");
                            }
                            Thread.sleep(5000);
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.sendMessage(TNTRunPlugin.prefix + "Сервер перезагружается.");
                            }
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        TNTRunPlugin.getInstance().sendStats();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        sendUpdatesToLobby();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
                    }
                }).start();
    }

    public void killPlayer(Player p) {
        if ((this.status == GameStatus.INGAME || this.status == GameStatus.PRE_GAME) && this.players.containsKey(p.getName())) {
            TNTPlayer pl = this.players.get(p.getName());
            for (Player p1 : Bukkit.getOnlinePlayers()) {
                if (!p1.getName().equals(p.getName())) {
                    p1.sendMessage(TNTRunPlugin.prefix + p.getName() + " выбыл.");
                }
            }
            pl.sendMessage(TNTRunPlugin.prefix + "Ты погиб в бою.");
            pl.getStats().setPosition(this.players.size());
            pl.getStats().setInGameTime(System.currentTimeMillis() / 1000L - this.timeStart);
            pl.getBukkitModel().closeInventory();
            pl.getBukkitModel().getInventory().clear();
            this.players.remove(pl.getName());
            this.spectators.put(pl.getName(), pl);
            pl.setSpectator(true);
            pl.getBukkitModel().teleport(getLobby());

            for (TNTPlayer p1 : this.players.values()) {
                p1.addExp(EconomicSettings.anotherDie);
                p1.sendMessage(TNTRunPlugin.prefixMoney + "+" + EconomicSettings.anotherDie + " опыта.");
            }

            if (this.players.size() < 2) {
                TNTRunPlugin.game.endGame();
            }
        } else {
            p.teleport(getLobby());
        }
        sendUpdatesToLobby();
    }

    public Location getLobby() {
        return this.lobby;
    }

    public GameStatus getStatus() {
        return status;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getActivePlayers() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public TNTPlayer getPlayer(String name) {
        if (this.players.containsKey(name))
            return this.players.get(name);
        if (this.spectators.containsKey(name))
            return this.spectators.get(name);
        return null;
    }

    public void addSpectator(TNTPlayer p) {
        this.spectators.put(p.getName(), p);
    }

    public boolean isSpectator(TNTPlayer p) {
        return this.spectators.containsKey(p.getName());
    }

    public void addPlayer(TNTPlayer p) {
        this.players.put(p.getName(), p);
    }

    public HashMap<String, TNTPlayer> getPlayers() {
        return players;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public Collection<TNTPlayer> getActivePlayersArray() {
        return this.players.values();
    }

    public Collection<TNTPlayer> getSpectatorsArray() {
        return this.spectators.values();
    }

    public void removeSpectator(TNTPlayer p) {
        if (p.isSpectator())
            this.spectators.remove(p.getName());
    }

    public void removePlayer(TNTPlayer p) {
        if (this.players.containsKey(p.getName()))
            this.players.remove(p.getName());
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public boolean isForDestroy(Location loc) {
        return (loc.getBlock().getType() == Material.GRAVEL || loc.getBlock().getType() == Material.SAND);
    }

    public void destroyWithLatency(final Location plufloc) {
        if (!toRemove.contains(plufloc)) {
            Bukkit.getScheduler().runTaskLater(TNTRunPlugin.getInstance(), new Runnable() {
                public void run() {
                    toRemove.remove(plufloc);
                    plufloc.getBlock().setType(Material.AIR);
                    plufloc.getBlock().getRelative(BlockFace.DOWN).setType(Material.AIR);
                }
            }, TNTRunPlugin.getInstance().destroyLatency);
        }
    }
}
