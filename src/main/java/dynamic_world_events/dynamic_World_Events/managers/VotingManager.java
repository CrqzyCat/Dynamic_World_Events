package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages player voting for the next event.
 *
 * Flow:
 *   1. EventScheduler calls startVote() X seconds before the next event
 *   2. Players use /dwe vote to cast their vote
 *   3. After the voting period, the event with the most votes wins
 *   4. On tie, a random winner among the tied events is chosen
 *
 * Config:
 *   voting:
 *     enabled: true
 *     duration-seconds: 60      # How long the vote stays open
 *     options: 3                # How many event options to present (2-4)
 *     announce-in-chat: true    # Broadcast vote options in chat
 */
public class VotingManager {

    private final Dynamic_World_Events plugin;
    private final Random random = new Random();

    private boolean voteActive = false;
    private final List<WorldEvent> options   = new ArrayList<>();
    private final Map<UUID, Integer> votes   = new HashMap<>(); // uuid → option index (0-based)
    private BukkitTask endTask;
    private int secondsRemaining = 0;

    public VotingManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    // ── Start a vote ──────────────────────────────────────────────────────────

    public boolean startVote() {
        if (!plugin.getConfig().getBoolean("voting.enabled", true)) return false;
        if (voteActive) return false;
        if (plugin.getEventManager().hasActiveEvent()) return false;

        // Pick random options from the enabled pool
        List<WorldEvent> pool = new ArrayList<>(
            plugin.getEventManager().getRegisteredEvents().stream()
                .filter(WorldEvent::isEnabled)
                .toList()
        );
        if (pool.size() < 2) return false;

        Collections.shuffle(pool);
        int optionCount = Math.min(plugin.getConfig().getInt("voting.options", 3), pool.size());
        options.clear();
        votes.clear();
        for (int i = 0; i < optionCount; i++) options.add(pool.get(i));

        voteActive = true;
        secondsRemaining = plugin.getConfig().getInt("voting.duration-seconds", 60);

        broadcastVoteStart();

        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::resolveVote,
            (long) secondsRemaining * 20L);

        // Reminders at half-time and 10s
        int half = secondsRemaining / 2;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (voteActive) broadcastReminder(secondsRemaining / 2);
        }, (long) half * 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (voteActive) broadcastReminder(10);
        }, (long) (secondsRemaining - 10) * 20L);

        return true;
    }

    // ── Player casts a vote ───────────────────────────────────────────────────

    /**
     * @param player The voter.
     * @param choice 1-based index matching what was shown in chat.
     * @return Result message to send back to the player.
     */
    public String castVote(Player player, int choice) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        if (!voteActive) return prefix + "&cNo vote is currently active.";
        if (choice < 1 || choice > options.size())
            return prefix + "&cInvalid choice. Pick a number between 1 and " + options.size() + ".";

        boolean alreadyVoted = votes.containsKey(player.getUniqueId());
        votes.put(player.getUniqueId(), choice - 1);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 1.2f);

        String eventName = options.get(choice - 1).getDisplayName();
        return prefix + (alreadyVoted
            ? "&7Vote changed to &f" + eventName + "&7."
            : "&aVoted for &f" + eventName + "&a!");
    }

    // ── Resolve ───────────────────────────────────────────────────────────────

    private void resolveVote() {
        voteActive = false;
        endTask = null;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        if (votes.isEmpty()) {
            // No votes — pick random
            WorldEvent fallback = options.get(random.nextInt(options.size()));
            Bukkit.broadcastMessage(MessageUtil.color(
                prefix + "&7No votes cast. Starting random event: &f" + fallback.getDisplayName()));
            plugin.getEventManager().startEventById(fallback.getId());
            cleanup();
            return;
        }

        // Count votes per option
        int[] tally = new int[options.size()];
        for (int idx : votes.values()) tally[idx]++;

        // Find max
        int max = 0;
        for (int count : tally) max = Math.max(max, count);

        // Collect winners (tie-break)
        List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < tally.length; i++) {
            if (tally[i] == max) winners.add(i);
        }
        int winnerIdx = winners.get(random.nextInt(winners.size()));
        WorldEvent winner = options.get(winnerIdx);

        // Broadcast result
        StringBuilder sb = new StringBuilder();
        sb.append(MessageUtil.color(prefix + "&6Vote result:"));
        Bukkit.broadcastMessage(sb.toString());
        for (int i = 0; i < options.size(); i++) {
            String bar   = buildBar(tally[i], votes.size());
            boolean won  = i == winnerIdx;
            Bukkit.broadcastMessage(MessageUtil.color(
                (won ? "&a" : "&7") + " [" + (i + 1) + "] "
                + options.get(i).getDisplayName()
                + " &8— " + bar + " &7" + tally[i] + " vote" + (tally[i] == 1 ? "" : "s")
                + (won ? " &a✓" : "")
            ));
        }

        Bukkit.broadcastMessage(MessageUtil.color(
            prefix + "&a" + (winners.size() > 1 ? "Tie! " : "") + "Starting: &f" + winner.getDisplayName()));

        plugin.getEventManager().startEventById(winner.getId());
        cleanup();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcastVoteStart() {
        if (!plugin.getConfig().getBoolean("voting.announce-in-chat", true)) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        Bukkit.broadcastMessage(MessageUtil.color(
            prefix + "&6Vote for the next event! &7Use &f/dwe vote <number>"));
        for (int i = 0; i < options.size(); i++) {
            Bukkit.broadcastMessage(MessageUtil.color(
                "&7  [&e" + (i + 1) + "&7] &f" + options.get(i).getDisplayName()));
        }
        Bukkit.broadcastMessage(MessageUtil.color(
            prefix + "&7You have &e" + secondsRemaining + "s &7to vote."));

        // Play a sound for all online players
        Bukkit.getOnlinePlayers().forEach(p ->
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 0.7f, 1.0f));
    }

    private void broadcastReminder(int secs) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        int[] tally = new int[options.size()];
        for (int idx : votes.values()) tally[idx]++;

        Bukkit.broadcastMessage(MessageUtil.color(
            prefix + "&7Vote closes in &e" + secs + "s&7 — current standings:"));
        for (int i = 0; i < options.size(); i++) {
            Bukkit.broadcastMessage(MessageUtil.color(
                "&7  [&e" + (i + 1) + "&7] &f" + options.get(i).getDisplayName()
                + " &8— &7" + tally[i] + " vote" + (tally[i] == 1 ? "" : "s")));
        }
    }

    private String buildBar(int count, int total) {
        int filled = total == 0 ? 0 : (int) Math.round((double) count / total * 10);
        return ChatColor.GREEN + "█".repeat(filled) + ChatColor.DARK_GRAY + "█".repeat(10 - filled);
    }

    public void cancel() {
        voteActive = false;
        if (endTask != null) { endTask.cancel(); endTask = null; }
        cleanup();
    }

    private void cleanup() {
        options.clear();
        votes.clear();
        voteActive = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isVoteActive()          { return voteActive; }
    public List<WorldEvent> getOptions()   { return Collections.unmodifiableList(options); }
    public int getVoteCount()              { return votes.size(); }
}
