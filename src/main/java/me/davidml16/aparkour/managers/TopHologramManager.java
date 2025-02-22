package me.davidml16.aparkour.managers;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import me.davidml16.aparkour.Main;
import me.davidml16.aparkour.data.LeaderboardEntry;
import me.davidml16.aparkour.data.Parkour;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class TopHologramManager {

    private final HashMap<String, Hologram> holoHeader;
    private final HashMap<String, Hologram> holoBody;
    private final HashMap<String, TextLine> holoFooter;

    private int timeLeft;
    private int reloadInterval;

    private final Main main;

    public TopHologramManager(Main main, int reloadInterval) {
        this.main = main;
        this.reloadInterval = reloadInterval;
        this.holoHeader = new HashMap<>();
        this.holoBody = new HashMap<>();
        this.holoFooter = new HashMap<>();
    }

    public HashMap<String, Hologram> getHoloHeader() {
        return holoHeader;
    }

    public HashMap<String, Hologram> getHoloBody() {
        return holoBody;
    }

    public HashMap<String, TextLine> getHoloFooter() {
        return holoFooter;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setReloadInterval(int reloadInterval) {
        this.reloadInterval = reloadInterval;
    }

    public void restartTimeLeft() {
        this.timeLeft = reloadInterval;
    }

    public void loadTopHolograms() {
        holoBody.clear();
        holoHeader.clear();
        holoFooter.clear();
        if (main.isHologramsEnabled()) {
            for (String parkour : main.getParkourHandler().getParkours().keySet()) {
                loadTopHologram(parkour);
            }
        }
    }

    public void loadTopHologram(String id) {
        if (main.isHologramsEnabled()) {
            Parkour parkour = main.getParkourHandler().getParkours().get(id);

            main.getDatabaseHandler().getParkourBestTimes(parkour.getId(), 10).thenAccept(leaderboard -> {
                main.getLeaderboardHandler().addLeaderboard(parkour.getId(), leaderboard);

                Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                    if (parkour.getTopHologram() != null) {
                        Hologram header = HologramsAPI.createHologram(main,
                                parkour.getTopHologram().clone().add(0.5D, 4.5D, 0.5D));
                        header.appendTextLine(main.getLanguageHandler()
                                .getMessage("Holograms.Top.Header.Line1").replaceAll("%parkour%", ColorManager.translate(parkour.getName())));
                        header.appendTextLine(main.getLanguageHandler()
                                .getMessage("Holograms.Top.Header.Line2").replaceAll("%parkour%", ColorManager.translate(parkour.getName())));

                        Hologram body = HologramsAPI.createHologram(main,
                                parkour.getTopHologram().clone().add(0.5D, 3.75D, 0.5D));

                        Hologram footer = HologramsAPI.createHologram(main,
                                parkour.getTopHologram().clone().add(0.5D, 1D, 0.5D));
                        footer.appendTextLine(main.getLanguageHandler()
                                .getMessage("Holograms.Top.Footer.Line")
                                .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.HologramUpdate"), timeLeft * 1000)));

                        if(leaderboard != null) {
                            int i = 0;
                            for (LeaderboardEntry entry : leaderboard) {
                                String line = main.getLanguageHandler()
                                        .getMessage("Holograms.Top.Body.Line")
                                        .replaceAll("%player%", main.getPlayerDataHandler().getPlayerName(body.getWorld(), entry.getName()))
                                        .replaceAll("%position%", Integer.toString(i + 1))
                                        .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.ParkourTimer"), entry.getTime()));

                                body.appendTextLine(line);
                                i++;
                            }
                            for (int j = i; j < 10; j++) {
                                body.appendTextLine(main.getLanguageHandler()
                                        .getMessage("Holograms.Top.Body.NoTime").replaceAll("%position%", Integer.toString(j + 1)));
                            }
                        } else {
                            for (int i = 0; i < 10; i++) {
                                body.appendTextLine(main.getLanguageHandler()
                                        .getMessage("Holograms.Top.Body.NoTime").replaceAll("%position%", Integer.toString(i + 1)));
                            }
                        }

                        holoHeader.put(id, header);
                        holoBody.put(id, body);
                        holoFooter.put(id, (TextLine) footer.getLine(0));
                    }
                }, 20L);
            });
        }
    }

    public void reloadTopHolograms() {
        if (main.isHologramsEnabled()) {
            if (timeLeft <= 0) {
                for (Parkour parkour : main.getParkourHandler().getParkours().values()) {

                    if(parkour.getTopHologram() != null) {
                        if (holoFooter.containsKey(parkour.getId())) {
                            holoFooter.get(parkour.getId()).setText(main.getLanguageHandler().getMessage("Holograms.Top.Footer.Updating"));
                        }
                    }

                    main.getDatabaseHandler().getParkourBestTimes(parkour.getId(), 10).thenAccept(leaderboard -> {
                        main.getLeaderboardHandler().addLeaderboard(parkour.getId(), leaderboard);

                        // 异步执行
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // Top不为空
                                if(parkour.getTopHologram() != null) {
                                    // 不知道
                                    if (holoBody.containsKey(parkour.getId()) && holoFooter.containsKey(parkour.getId())) {
                                        // 定义全息图
                                        Hologram body = holoBody.get(parkour.getId());
                                        // 行数=0
                                        int i = 0;
                                        // 循环增加
                                        for (; i < leaderboard.size(); i++) {
                                            // 设置行数内容
                                            ((TextLine) body.getLine(i)).setText(main.getLanguageHandler()
                                                    .getMessage("Holograms.Top.Body.Line").replaceAll("%position%", Integer.toString(i + 1))
                                                    .replaceAll("%player%", main.getPlayerDataHandler().getPlayerName(body.getWorld(), leaderboard.get(i).getName()))
                                                    .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.ParkourTimer"), leaderboard.get(i).getTime())));
                                        }
                                        for (int j = i; j < 10; j++) {
                                            ((TextLine) body.getLine(j)).setText(main.getLanguageHandler()
                                                    .getMessage("Holograms.Top.Body.NoTime").replaceAll("%position%", Integer.toString(j + 1)));
                                        }
                                    }
                                }
                            }
                        }.runTaskTimer(Main.getInstance(), 0L, 20L);
                    });
                }
                restartTimeLeft();
            }
            for (String parkour : main.getParkourHandler().getParkours().keySet()) {
                if (holoFooter.containsKey(parkour)) {
                    holoFooter.get(parkour)
                            .setText(main.getLanguageHandler()
                            .getMessage("Holograms.Top.Footer.Line")
                            .replaceAll("%time%", main.getTimerManager().millisToString(main.getLanguageHandler().getMessage("Timer.Formats.HologramUpdate"), timeLeft * 1000)));
                }
            }
            timeLeft--;
        }
    }

    public void removeHologram(String id) {
        if (main.isHologramsEnabled()) {
            if (holoHeader.containsKey(id)) {
                holoHeader.get(id).delete();
                holoHeader.remove(id);
            }

            if (holoBody.containsKey(id)) {
                holoBody.get(id).delete();
                holoBody.remove(id);
            }

            if (holoFooter.containsKey(id)) {
                holoFooter.get(id).getParent().delete();
                holoFooter.remove(id);
            }
        }
    }

}
