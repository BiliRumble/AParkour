package me.davidml16.aparkour.gui;

import me.davidml16.aparkour.Main;
import me.davidml16.aparkour.conversation.RewardMenu;
import me.davidml16.aparkour.data.Pair;
import me.davidml16.aparkour.data.Parkour;
import me.davidml16.aparkour.data.Reward;
import me.davidml16.aparkour.managers.ColorManager;
import me.davidml16.aparkour.utils.ItemBuilder;
import me.davidml16.aparkour.utils.Sounds;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class Rewards_GUI implements Listener {

    private HashMap<UUID, Pair> opened;
    private HashMap<String, Inventory> guis;
    private List<Integer> borders;

    private Main main;

    public Rewards_GUI(Main main) {
        this.main = main;
        this.opened = new HashMap<UUID, Pair>();
        this.guis = new HashMap<String, Inventory>();
        this.borders = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 40, 42, 43, 44);
        this.main.getServer().getPluginManager().registerEvents(this, this.main);
    }

    public HashMap<UUID, Pair> getOpened() {
        return opened;
    }

    public HashMap<String, Inventory> getGuis() {
        return guis;
    }

    public void loadGUI() {
        for (File file : Objects.requireNonNull(new File(main.getDataFolder(), "parkours").listFiles())) {
            loadGUI(file.getName().toLowerCase().replace(".yml", ""));
        }
    }

    public void loadGUI(String id) {
        if (guis.containsKey(id)) return;

        Inventory gui = Bukkit.createInventory(null, 45, main.getLanguageHandler().getMessage("GUIs.Rewards.title").replaceAll("%parkour%", id));

        ItemStack edge = new ItemBuilder(Material.STAINED_GLASS_PANE, 1).setDurability((short) 7).setName("").toItemStack();
        ItemStack newReward = new ItemBuilder(Material.DOUBLE_PLANT, 1).setName(ColorManager.translate("&aCreate new reward")).toItemStack();
        ItemStack back = new ItemBuilder(Material.ARROW, 1).setName(ColorManager.translate("&aBack to config")).toItemStack();

        for (Integer i : borders) {
            gui.setItem(i, edge);
        }

        gui.setItem(39, newReward);
        gui.setItem(41, back);

        guis.put(id, gui);
    }

    public void reloadAllGUI() {
        for(String id : main.getParkourHandler().getParkours().keySet()) {
            reloadGUI(id);
        }
    }

    public void reloadGUI(String id) {
        for(UUID uuid : opened.keySet()) {
            if(opened.get(uuid).getParkour().equals(id)) {
                Player p = Bukkit.getPlayer(uuid);
                openPage(p, id, opened.get(uuid).getPage());
            }
        }
    }

    private void openPage(Player p, String id, int page) {
        List<Reward> rewards = main.getParkourHandler().getParkourById(id).getRewards();

        if(page > 0 && rewards.size() < (page * 21) + 1) {
            openPage(p, id, page - 1);
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, main.getLanguageHandler().getMessage("GUIs.Rewards.title").replaceAll("%parkour%", id));
        gui.setContents(guis.get(id).getContents());

        for (int i = 10; i <= 16; i++)
            gui.setItem(i, null);
        for (int i = 19; i <= 25; i++)
            gui.setItem(i, null);
        for (int i = 28; i <= 34; i++)
            gui.setItem(i, null);

        if (page > 0) {
            gui.setItem(18, new ItemBuilder(Material.ENDER_PEARL, 1).setName(ColorManager.translate("&aPrevious page")).toItemStack());
        } else {
            gui.setItem(18, new ItemBuilder(Material.STAINED_GLASS_PANE, 1).setDurability((short) 7).setName("").toItemStack());
        }

        if (rewards.size() > (page + 1) * 21) {
            gui.setItem(26, new ItemBuilder(Material.ENDER_PEARL, 1).setName(ColorManager.translate("&aNext page")).toItemStack());
        } else {
            gui.setItem(26, new ItemBuilder(Material.STAINED_GLASS_PANE, 1).setDurability((short) 7).setName("").toItemStack());
        }

        if (rewards.size() > 21) rewards = rewards.subList(page * 21, ((page * 21) + 21) > rewards.size() ? rewards.size() : (page * 21) + 21);

        if(rewards.size() > 0) {
            for (Reward reward : rewards) {
                gui.addItem(new ItemBuilder(Material.GOLD_NUGGET, 1)
                        .setName(ColorManager.translate("&a" + reward.getId()))
                        .setLore(
                                "",
                                ColorManager.translate(" &7First time: &6" + reward.isFirstTime() + " "),
                                ColorManager.translate(" &7Permission: &6" + reward.getPermission() + " "),
                                ColorManager.translate(" &7Command: &6" + reward.getCommand() + " "),
                                ColorManager.translate(" &7Chance: &6" + reward.getChance() + "% "),
                                "",
                                ColorManager.translate("&eClick to remove! ")).toItemStack());
            }
        } else {
            gui.setItem(22, new ItemBuilder(Material.STAINED_GLASS_PANE, 1).setDurability((short) 14).setName(ColorManager.translate("&cAny rewards selected")).setLore(
                    "",
                    ColorManager.translate(" &7You dont have any "),
                    ColorManager.translate(" &7reward selected. "),
                    ""
            ).toItemStack());
        }

        if (!opened.containsKey(p.getUniqueId())) {
            p.openInventory(gui);
        } else {
            p.getOpenInventory().getTopInventory().setContents(gui.getContents());
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> opened.put(p.getUniqueId(), new Pair(id, page)), 1L);
    }

    public void open(Player p, String id) {
        p.updateInventory();
        openPage(p, id, 0);

        Sounds.playSound(p, p.getLocation(), Sounds.MySound.CLICK, 10, 2);
        Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> opened.put(p.getUniqueId(), new Pair(id, 0)), 1L);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        if (opened.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            String id = opened.get(p.getUniqueId()).getParkour();
            Parkour parkour = main.getParkourHandler().getParkourById(opened.get(p.getUniqueId()).getParkour());
            if (slot == 18 && e.getCurrentItem().getType() == Material.ENDER_PEARL) {
                Sounds.playSound(p, p.getLocation(), Sounds.MySound.CLICK, 10, 2);
                openPage(p, id, opened.get(p.getUniqueId()).getPage() - 1);
            } else if (slot == 26 && e.getCurrentItem().getType() == Material.ENDER_PEARL) {
                Sounds.playSound(p, p.getLocation(), Sounds.MySound.CLICK, 10, 2);
                openPage(p, id, opened.get(p.getUniqueId()).getPage() + 1);
            } else if (slot == 39) {
                p.closeInventory();
                new RewardMenu(main).getConversation(p, parkour).begin();
                Sounds.playSound(p, p.getLocation(), Sounds.MySound.ANVIL_USE, 100, 3);
            } else if (slot == 41) {
                main.getConfigGUI().open(p, parkour.getId());
            } else if ((slot >= 10 && slot <= 16) || (slot >= 19 && slot <= 25) || (slot >= 28 && slot <= 34)) {
                if (e.getCurrentItem().getType() == Material.AIR) return;

                if (parkour.getRewards().size() == 0) return;

                String rewardID = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                for(Reward reward : parkour.getRewards()) {
                    if(reward.getId().equalsIgnoreCase(rewardID)) {
                        parkour.getRewards().remove(reward);
                        p.sendMessage(ColorManager.translate(main.getLanguageHandler().getPrefix()
                                + " &aYou removed reward &e" + rewardID + " &afrom rewards of parkour &e" + parkour.getId()));
                        reloadGUI(parkour.getId());
                        Sounds.playSound(p, p.getLocation(), Sounds.MySound.CLICK, 10, 2);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void InventoryCloseEvent(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (opened.containsKey(p.getUniqueId())) {
            main.getParkourHandler().getParkours().get(opened.get(p.getUniqueId()).getParkour()).saveParkour();
            opened.remove(p.getUniqueId());
        }
    }

}
