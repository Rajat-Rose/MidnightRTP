package net.midnight.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MidnightRTP extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, BukkitTask> warmupTasks = new HashMap<>();
    private final Map<UUID, Location> startLocations = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute RTP!");
            return true;
        }

        startRTPProcess(player);
        return true;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> performAsyncRTP(player, false), 10L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (warmupTasks.containsKey(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
                warmupTasks.get(uuid).cancel();
                warmupTasks.remove(uuid);
                startLocations.remove(uuid);
                player.sendMessage("§cRTP cancelled because you moved!");
            }
        }
    }

    private void startRTPProcess(Player player) {
        UUID uuid = player.getUniqueId();
        if (warmupTasks.containsKey(uuid)) {
            player.sendMessage("§eRTP is already in progress!");
            return;
        }

        startLocations.put(uuid, player.getLocation().clone());
        player.sendMessage("§eTeleporting in 5 seconds... Do not move!");

        BukkitTask task = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    warmupTasks.remove(uuid);
                    startLocations.remove(uuid);
                    return;
                }

                if (countdown > 0) {
                    player.sendMessage("§eRTP in " + countdown + "s... Stay still!");
                    countdown--;
                } else {
                    cancel();
                    warmupTasks.remove(uuid);
                    startLocations.remove(uuid);
                    performAsyncRTP(player, true);
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        warmupTasks.put(uuid, task);
    }

    private void performAsyncRTP(Player player, boolean applyEffects) {
        World world = player.getWorld();
        int minX = -1500, maxX = 1500;
        int minZ = -1500, maxZ = 1500;

        int randomX = random.nextInt(maxX - minX + 1) + minX;
        int randomZ = random.nextInt(maxZ - minZ + 1) + minZ;

        int chunkX = randomX >> 4;
        int chunkZ = randomZ >> 4;

        // Async Chunk Loading to prevent main thread freezing
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            int y = world.getHighestBlockYAt(randomX, randomZ);
            Location safeLoc = new Location(world, randomX + 0.5, y + 1, randomZ + 0.5);

            Material type = safeLoc.clone().add(0, -1, 0).getBlock().getType();
            if (type == Material.LAVA || type == Material.WATER) {
                performAsyncRTP(player, applyEffects);
                return;
            }

            player.teleportAsync(safeLoc).thenAccept(success -> {
                if (success) {
                    player.sendMessage("§aTeleported successfully!");
                    if (applyEffects) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));      // 3s
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));       // 3s
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));  // 5s
                    }
                }
            });
        });
    }
}
