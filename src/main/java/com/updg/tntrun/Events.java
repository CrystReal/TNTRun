package com.updg.tntrun;

import com.updg.CR_API.Events.BungeeReturnIdEvent;
import com.updg.CR_API.Events.LobbyUpdateCheckEvent;
import com.updg.tntrun.Models.TNTPlayer;
import com.updg.tntrun.Models.enums.GameStatus;
import com.updg.CR_API.Utils.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Alex
 * Date: 17.06.13  19:46
 */
public class Events implements Listener {

    int tid = 0;
    int count = 10;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        Player user = event.getPlayer();
        TNTPlayer p = TNTRunPlugin.game.getPlayer(user.getName());
        if (p == null) {
            p = new TNTPlayer(user);
            if (TNTRunPlugin.game.getStatus() == GameStatus.WAITING) {
                if (TNTRunPlugin.game.getActivePlayers() < TNTRunPlugin.game.getMaxPlayers())
                    TNTRunPlugin.game.addPlayer(p);
                else
                    TNTRunPlugin.game.addSpectator(p);
            } else {
                TNTRunPlugin.game.addSpectator(p);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        e.getPlayer().getInventory().clear();
        final TNTPlayer p = TNTRunPlugin.game.getPlayer(e.getPlayer().getName());
        e.getPlayer().teleport(TNTRunPlugin.game.getLobby());
        e.getPlayer().setGameMode(GameMode.ADVENTURE);
        e.getPlayer().setAllowFlight(true);
        if (TNTRunPlugin.game.getStatus() != GameStatus.WAITING) {
            p.sendMessage(TNTRunPlugin.prefix + "Игра уже началась.");
        } else {
            if (p.isSpectator()) {
                p.sendMessage(TNTRunPlugin.prefix + "В игре нет свободных мест. Вы зашли как налюбдающий!");
            } else {
                e.setJoinMessage(TNTRunPlugin.prefix + e.getPlayer().getName() + " вошел на арену. " + Bukkit.getOnlinePlayers().length + "/" + TNTRunPlugin.game.getMinPlayers());
                if (TNTRunPlugin.game.isAbleToStart()) {
                    TNTRunPlugin.game.preGame();
                } else {
                    TNTRunPlugin.game.sendUpdatesToLobby();
                    e.getPlayer().sendMessage(TNTRunPlugin.prefix + "Игра начнется когда наберется " + TNTRunPlugin.game.getMinPlayers() + " " + StringUtil.plural(TNTRunPlugin.game.getMinPlayers(), "игрок", "игрока", "игроков"));
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        if (TNTRunPlugin.game.getPlayers().containsKey(e.getPlayer().getName())) {
            TNTRunPlugin.game.killPlayer(e.getPlayer());
        }
        TNTPlayer p = TNTRunPlugin.game.getPlayer(e.getPlayer().getName());
        if (p != null && TNTRunPlugin.game.getStatus() == GameStatus.WAITING) {
            TNTRunPlugin.game.removePlayer(p);
            TNTRunPlugin.game.removeSpectator(p);
        }
        TNTRunPlugin.game.sendUpdatesToLobby();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();

            if (!p.isDead()) {
                if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    TNTRunPlugin.game.killPlayer(p);
                }
            } else {
                if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    p.teleport(TNTRunPlugin.game.getLobby());
                }
            }
            e.setCancelled(true);
            p.setHealth(p.getMaxHealth());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.setDeathMessage(null);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
       /* if (TNTRunPlugin.game.getStatus() == GameStatus.INGAME) {
            TNTPlayer p = TNTRunPlugin.game.getPlayer(e.getPlayer().getName());
            if (!p.isSpectator() && hasChangedBlockCoordinates(e.getFrom(), e.getTo())) {
                Location plufloc = e.getPlayer().getLocation().clone().add(0.0D, -1.0D, 0.0D);
                if (TNTRunPlugin.game.isForDestroy(plufloc)) {
                    TNTRunPlugin.game.destroyWithLatency(plufloc);
                }
            }
        }   */
    }

    private boolean hasChangedBlockCoordinates(final Location fromLoc, final Location toLoc) {
        return !(fromLoc.getWorld().equals(toLoc.getWorld())
                && fromLoc.getBlockX() == toLoc.getBlockX()
                && fromLoc.getBlockY() == toLoc.getBlockY()
                && fromLoc.getBlockZ() == toLoc.getBlockZ());
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onChangeHunger(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
    }

    @EventHandler
    public void onGetID(BungeeReturnIdEvent e) {
        TNTRunPlugin.game.getPlayer(e.getUsername()).setId(e.getId());
    }

    @EventHandler
    public void onNeedUpdate(LobbyUpdateCheckEvent e) {
        TNTRunPlugin.game.sendUpdatesToLobby();
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        TNTPlayer player = TNTRunPlugin.game.getPlayer(event.getPlayer().getName());
        if (player.getBukkitModel().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            if (player.canDoubleJump()) {
                player.getBukkitModel().setAllowFlight(false);
                player.getBukkitModel().setFlying(false);
                player.getBukkitModel().setVelocity(player.getBukkitModel().getLocation().getDirection().multiply(1.6).setY(1));
                player.getBukkitModel().setAllowFlight(true);
                player.useDoubleJump();
            }
        }
    }
}
