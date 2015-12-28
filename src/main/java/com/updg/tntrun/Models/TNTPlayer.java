package com.updg.tntrun.Models;

import com.updg.CR_API.Bungee.Bungee;
import com.updg.CR_API.DataServer.DSUtils;
import com.updg.tntrun.TNTRunPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Created by Alex
 * Date: 15.12.13  13:37
 */
public class TNTPlayer {

    private static String sDoubleJump = ChatColor.DARK_GREEN + "Двойной прыжок";

    private int id;
    private String name;

    private Player bukkitModel;
    private TNTPlayerStats stats;

    private double exp = 0;

    private int doubleJumps = 0;

    private boolean wasInGame = false;

    public TNTPlayer(Player p) {
        this.setBukkitModel(p);
        this.name = p.getName();
        this.stats = new TNTPlayerStats();
        this.getIdFromBungee();
    }

    private void getIdFromBungee() {
        Bungee.isLogged(getBukkitModel(), getName());
    }

    public void registerScoreboard() {
        getBukkitModel().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        Scoreboard board = getBukkitModel().getScoreboard();
        Objective obj = board.registerNewObjective("info", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.RESET + "Информация");
        Score score;
        score = obj.getScore(Bukkit.getOfflinePlayer(sDoubleJump));
        if (doubleJumps == -1)
            score.setScore(999);
        else
            score.setScore(doubleJumps);
    }

    private Score getScore(String name) {
        Objective obj = getBukkitModel().getScoreboard().getObjective("info");
        return obj.getScore(Bukkit.getOfflinePlayer(name));
    }

    public Player getBukkitModel() {
        return bukkitModel;
    }

    public void setBukkitModel(Player bukkitModel) {
        this.bukkitModel = bukkitModel;
    }

    public TNTPlayerStats getStats() {
        return stats;
    }

    public void sendMessage(String s) {
        getBukkitModel().sendMessage(s);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setSpectator(boolean b) {
        if (b) {
            this.hidePlayer();
            this.bukkitModel.setAllowFlight(true);
        } else {
            this.showPlayer();
            this.bukkitModel.setAllowFlight(false);
        }
    }

    private void hidePlayer() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(this.bukkitModel);
        }
    }

    private void showPlayer() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(this.bukkitModel);
        }
    }

    public boolean isSpectator() {
        return TNTRunPlugin.game.isSpectator(this);
    }


    public double getExp() {
        String[] out = DSUtils.getExpAndMoney(getBukkitModel());
        this.setExp(Double.parseDouble(out[0]));
        return exp;
    }

    public void setExp(double exp) {
        this.exp = exp;
    }

    public void withdrawExp(double v) {
        String[] out = DSUtils.withdrawPlayerExpAndMoney(getBukkitModel(), v, 0);
        this.setExp(Double.parseDouble(out[0]));
    }

    public void addExp(double v) {
        String[] out = DSUtils.addPlayerExpAndMoney(getBukkitModel(), v, 0);
        this.setExp(Double.parseDouble(out[0]));
    }

    public boolean wasInGame() {
        return wasInGame;
    }

    public void setWasInGame(boolean wasInGame) {
        this.wasInGame = wasInGame;
    }

    public boolean canDoubleJump() {
        return this.doubleJumps > 0 || this.doubleJumps == -1;
    }

    public void useDoubleJump() {
        if (this.doubleJumps > 0) {
            this.doubleJumps--;
            getScore(sDoubleJump).setScore(doubleJumps);
        }
    }
}
