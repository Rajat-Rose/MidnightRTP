package net.midnight.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class MidnightRTP extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private final int MAX_RADIUS = 2500; // Radius -2500 to +2500

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MidnightRTP v1.0 has been successfully enabled!");
    }

    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            teleportToSafeLocation(player);
            player.sendMessage("§a[Midnight] Welcome! Teleported to a random safe location.");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // 2-tick delay to ensure smooth respawn logic
        Bukkit.getScheduler().runTaskLater(this, () -> {
            teleportToSafeLocation(player);
            player.sendMessage("§e[Midnight] Respawned at a new random location!");
        }, 2L);
    }

    public void teleportToSafeLocation(Player player) {
        World world = player.getWorld();

        // Safe location search loop (Up to 15 attempts)
        for (int attempt = 0; attempt < 15; attempt++) {
            int x = random.nextInt(MAX_RADIUS * 2) - MAX_RADIUS;
            int z = random.nextInt(MAX_RADIUS * 2) - MAX_RADIUS;

            Block highestBlock = world.getHighestBlockAt(x, z);
            Material type = highestBlock.getType();

            // Ignore ocean, lava or air blocks
            if (type.isAir() || type == Material.WATER || type == Material.LAVA) {
                continue;
            }

            Location targetLoc = highestBlock.getLocation().add(0.5, 1.0, 0.5);
            player.teleport(targetLoc);
            return;
        }

        // Fallback to spawn location if no safe spot found in 15 tries
        player.teleport(world.getSpawnLocation());
    }
}
