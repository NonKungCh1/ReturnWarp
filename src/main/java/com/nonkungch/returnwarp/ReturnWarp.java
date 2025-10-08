package nonkungch.returnwarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ReturnWarp extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadSavedLocations();

        getCommand("vs").setExecutor(this);
        getCommand("back").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("✅ ReturnWarp 2.5 Enabled (API 1.21+ Ready)!");
    }

    @Override
    public void onDisable() {
        saveAllLocations();
        getLogger().info("💾 Saved all player locations before shutdown.");
    }

    // =============================
    // เก็บพิกัดก่อนวาร์ปจาก plugin อื่น (ทุกการเทเลพอร์ต)
    // =============================
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();

        if (from == null || to == null) return;

        // หากพิกัดเปลี่ยนจริง จะบันทึก
        if (!from.equals(to)) {
            lastLocation.put(player.getUniqueId(), from);
        }
    }

    // =============================
    // โหลด / บันทึกพิกัด
    // =============================
    private void loadSavedLocations() {
        if (!config.contains("saved-locations")) return;

        for (String uuidStr : config.getConfigurationSection("saved-locations").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String world = config.getString("saved-locations." + uuidStr + ".world");
                double x = config.getDouble("saved-locations." + uuidStr + ".x");
                double y = config.getDouble("saved-locations." + uuidStr + ".y");
                double z = config.getDouble("saved-locations." + uuidStr + ".z");
                float yaw = (float) config.getDouble("saved-locations." + uuidStr + ".yaw");
                float pitch = (float) config.getDouble("saved-locations." + uuidStr + ".pitch");

                World w = Bukkit.getWorld(world);
                if (w != null) {
                    lastLocation.put(uuid, new Location(w, x, y, z, yaw, pitch));
                }
            } catch (Exception ex) {
                getLogger().warning("❌ Error loading saved location for " + uuidStr);
            }
        }
    }

    private void saveAllLocations() {
        for (UUID uuid : lastLocation.keySet()) {
            Location loc = lastLocation.get(uuid);
            if (loc == null || loc.getWorld() == null) continue;

            String path = "saved-locations." + uuid;
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
        }
        saveConfig();
    }

    // =============================
    // GUI เลือกโลก
    // =============================
    private void openWorldGUI(Player player) {
        Inventory gui = Bukkit.createInventory((InventoryHolder) null, 9, "§bเลือกโลกที่จะวาร์ปไป");

        addWorldButton(gui, 2, "§aOverworld", Material.GRASS_BLOCK, "world");
        addWorldButton(gui, 4, "§cNether", Material.NETHERRACK, "world_nether");
        addWorldButton(gui, 6, "§dThe End", Material.END_STONE, "world_the_end");

        player.openInventory(gui);
    }

    private void addWorldButton(Inventory gui, int slot, String name, Material icon, String worldName) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList("§7ชื่อโลก: §f" + worldName));
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("§bเลือกโลกที่จะวาร์ปไป")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getLore() == null) return;

        String worldName = clicked.getItemMeta().getLore().get(0).replace("§7ชื่อโลก: §f", "");
        World target = Bukkit.getWorld(worldName);

        if (target == null) {
            player.sendMessage(color("&cไม่พบโลก &f" + worldName));
            player.closeInventory();
            return;
        }

        lastLocation.put(player.getUniqueId(), player.getLocation());
        player.teleport(target.getSpawnLocation());
        player.sendMessage(color("&aวาร์ปไปยังโลก &f" + worldName + " &aเรียบร้อย!"));
        player.closeInventory();
    }

    // =============================
    // คำสั่ง
    // =============================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("vs")) {
            openWorldGUI(player);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("back")) {
            Location prev = lastLocation.get(player.getUniqueId());
            if (prev == null) {
                player.sendMessage(color("&cไม่มีข้อมูลพิกัดก่อนหน้า!"));
                return true;
            }

            player.teleport(prev);
            player.sendMessage(color("&aกลับมายังพิกัดเดิมของ &f%player_name% &aเรียบร้อย!")
                    .replace("%player_name%", player.getName()));
            return true;
        }

        return false;
    }

    // =============================
    // ฟังก์ชันช่วยเปลี่ยนสี
    // =============================
    private String color(String text) {
        return text.replace("&", "§");
    }
}
