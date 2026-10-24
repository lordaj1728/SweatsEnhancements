package com.arena.pekaclient.gui;

import com.arena.pekaclient.feature.LeapClass;
import com.arena.pekaclient.feature.LeapClassCache;
import com.arena.pekaclient.feature.LeapDiagnostics;
import com.arena.pekaclient.feature.LeapIdentity;
import com.arena.pekaclient.feature.LeapText;
import com.arena.pekaclient.feature.SpiritLeap;
import com.arena.pekaclient.render.SkinHead;
import com.arena.pekaclient.render.UIRender;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Spirit Leap teammate picker. It replaces only the server-opened leap chest and draws a centered two-column
 * grid of compact rounded teammate cards over the visible world. Each card places a square player head to the left
 * of a fitted, enlarged player name and class label. There is no full-screen dimmer or window frame, and the Sky
 * VeLor hub layout is not used.
 *
 * Every click is the server's own window click on the teammate's slot, the same as a left click in the vanilla
 * menu. Sky VeLor sends no packet and no command of its own. After a pick the screen waits for the server, so a
 * double click or a held Enter cannot send a second leap.
 *
 * Heads use the matching tab-list skin or an asynchronous profile-texture lookup. A useful letter fallback is
 * drawn immediately while a skin is unavailable, and failed texture lookups are retried with a cooldown.
 *
 * Classes are read from visible scoreboard rows/team text first, then the matching tab-list entry and item lore.
 * A short-lived class cache is keyed by UUID (or canonical username when UUID is unavailable), never by a rank- or
 * class-formatted display name. Unknown data shows as \"Unknown class\".
 */
public final class LeapScreen extends GuiScreen {

    private static final long PENDING_NS = 1500000000L;
    /** Scoreboard and tab data changes slowly; avoid rebuilding the snapshot on every render frame. */
    private static final long REFRESH_INTERVAL_NS = 250000000L;
    /** A failed profile lookup is eligible for a retry after this delay. */
    private static final long SKIN_RETRY_MS = 15000L;

    private static final int TEST_LABEL_HEIGHT = 18;
    static final float NAME_SCALE = 2.6f;
    static final float CLASS_SCALE = 1.9f;
    private static final int FONT_LINE_HEIGHT = 9;
    private static final int NAME_LINE_GAP = 1;
    private static final float MIN_SINGLE_LINE_NAME_SCALE = 1.6f;
    private static final float CLASS_RELATIVE_SCALE = 0.82f;
    private static final int CLASS_TEXT_GAP = 3;
    private static final int TEXT_VERTICAL_PADDING = 2;

    private static final int CARD_FILL = LeapPalette.CARD_BACKGROUND;
    private static final int CARD_HOVER = LeapPalette.CARD_HOVER_BACKGROUND;
    private static final int CARD_EDGE = LeapPalette.CARD_BORDER;
    private static final int CARD_SELECT_EDGE = LeapPalette.CARD_HOVER_BORDER;
    private static final int HEAD_LETTER = 0xFF2C3E5C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFE0E0E0;

    /** Head states: unavailable skins immediately use a named fallback tile; ready skins use the real texture. */
    private static final int SKIN_UNAVAILABLE = 0;
    private static final int SKIN_READY = 1;

    /** Resolved skull textures, keyed by profile UUID when known (otherwise by canonical username). */
    private static final Map<String, ResourceLocation> SKULL_SKINS = new ConcurrentHashMap<String, ResourceLocation>();
    /** Last profile-texture request time. A miss is not permanent and can be retried after SKIN_RETRY_MS. */
    private static final Map<String, Long> SKULL_REQUESTED_AT = new ConcurrentHashMap<String, Long>();

    private static final class BoardEntry {
        final String member;
        final String raw;

        BoardEntry(String member, String raw) {
            this.member = member;
            this.raw = raw;
        }
    }

    private static final class BoardDetection {
        final String member;
        final String raw;
        final LeapClass cls;

        BoardDetection(String member, String raw, LeapClass cls) {
            this.member = member;
            this.raw = raw;
            this.cls = cls;
        }
    }

    /** One teammate, kept by slot number between refreshes. Live and preview cards use the same renderer. */
    private static final class Card {
        final int slot;
        ItemStack stack;
        String name = "";
        String owner = "";
        String rawDisplay = "";
        /** The item's own \"Class: X\" lore line, or null. Used after scoreboard/tab data. */
        String loreClass;
        LeapClass cls;
        String rawBoard;
        String rawTab;
        String source = "none";
        String boardMember;
        /** The skull's own profile (name, UUID and textures), or null for a non-skull item. */
        GameProfile profile;
        String identityKey;
        String identityUsername;
        String clsText = LeapClass.displayText(null);
        int clsColor = LeapClass.UNKNOWN_COLOR;
        ResourceLocation skin;
        int skinState = SKIN_UNAVAILABLE;
        int x;
        int y;

        Card(int slot) {
            this.slot = slot;
        }
    }

    private final Container container;
    private final boolean preview;
    private final GuiScreen previewParent;
    private final LeapClassCache classCache = new LeapClassCache();
    private final List<Card> previous = new ArrayList<Card>();
    private final List<Card> cards = new ArrayList<Card>();
    private final List<Slot> slotBuffer = new ArrayList<Slot>();

    private int highlight;
    private int previewOffset;
    private int cols = LeapLayout.COLS;
    /** Responsive card geometry for the current scaled Minecraft viewport. */
    private int cardW;
    private int cardH;
    private int headSize;
    private int gapX;
    private int gapY;
    private int gridY;
    private long pendingUntilNs;
    private long lastRefreshNs;

    public LeapScreen(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Live Spirit Leap screen requires the server container");
        }
        this.container = container;
        this.preview = false;
        this.previewParent = null;
    }

    private LeapScreen(GuiScreen parent) {
        this.container = null;
        this.preview = true;
        this.previewParent = parent;
        addPreviewCards();
    }

    /** Opens this real screen in isolated local preview mode; no container or gameplay action is available. */
    public static LeapScreen createPreview(GuiScreen parent) {
        return new LeapScreen(parent);
    }

    private void addPreviewCards() {
        cards.clear();
        for (int slot = 0; slot < LeapPreviewData.CARD_COUNT; slot++) {
            int sample = LeapPreviewData.sampleIndex(previewOffset, slot);
            addPreviewCard(slot, LeapPreviewData.displayName(sample), LeapPreviewData.username(sample),
                    LeapPreviewData.playerClass(sample));
        }
        highlight = Math.min(highlight, Math.max(0, cards.size() - 1));
    }

    private void cyclePreviewSamples(int direction) {
        if (!preview) {
            return;
        }
        previewOffset = Math.floorMod(previewOffset + direction, LeapPreviewData.SAMPLE_COUNT);
        addPreviewCards();
    }

    private void addPreviewCard(int slot, String displayName, String username, LeapClass cls) {
        Card card = new Card(slot);
        card.name = displayName;
        card.owner = username;
        card.identityKey = "preview:" + slot;
        card.cls = cls;
        card.clsText = cls.label();
        card.clsColor = cls.color();
        // The normal unavailable-skin renderer draws a legible initial tile; preview never requests or caches skins.
        card.skinState = SKIN_UNAVAILABLE;
        cards.add(card);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        if (!preview) {
            // The chest the server opened was swapped before its own initGui ran, so make this container the
            // player's open container here. Otherwise the server's window updates would not reach this menu.
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.openContainer = container;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        if (!preview) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                container.onContainerClosed(mc.thePlayer);
            }
        }
    }

    /** True while the server still has this live menu open; local preview screens are independent of containers. */
    private boolean stillOpen() {
        if (preview) {
            return true;
        }
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        return p != null && p.openContainer == container;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!preview && !stillOpen()) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        refreshIfNeeded();
    }

    /**
     * Refreshes the live snapshot at most four times a second. Scoreboard parsing and item tooltip extraction do not
     * run per render frame; skin callbacks and input remain non-blocking.
     */
    private void refreshIfNeeded() {
        if (preview) {
            return;
        }
        long now = System.nanoTime();
        if (lastRefreshNs != 0L && now - lastRefreshNs < REFRESH_INTERVAL_NS) {
            return;
        }
        lastRefreshNs = now;
        refresh(System.currentTimeMillis());
    }

    private void refresh(long nowMs) {
        SpiritLeap.teammateSlots(container, slotBuffer);
        previous.clear();
        previous.addAll(cards);
        cards.clear();
        for (int i = 0, n = slotBuffer.size(); i < n; i++) {
            Slot s = slotBuffer.get(i);
            Card c = null;
            for (int k = 0, m = previous.size(); k < m; k++) {
                if (previous.get(k).slot == s.slotNumber) {
                    c = previous.get(k);
                    break;
                }
            }
            if (c == null) {
                c = new Card(s.slotNumber);
            }
            ItemStack stack = s.getStack();
            // Re-read the small number of skull items each snapshot. Server updates can mutate an ItemStack in place.
            c.stack = stack;
            readDetails(c, stack);
            cards.add(c);
        }

        Minecraft mc = Minecraft.getMinecraft();
        List<NetworkPlayerInfo> playerInfos = playerInfos(mc);
        Scoreboard board = mc.theWorld == null ? null : mc.theWorld.getScoreboard();
        List<BoardEntry> boardEntries = scoreboardEntries(board);
        Collection<String> names = knownNames(playerInfos, boardEntries);
        Set<String> activeIdentities = new HashSet<String>();
        for (Card c : cards) {
            resolve(c, nowMs, playerInfos, boardEntries, names);
            if (c.identityKey != null) {
                activeIdentities.add(c.identityKey);
            }
        }
        classCache.retainCurrentPlayers(activeIdentities, nowMs);
        if (highlight >= cards.size()) {
            highlight = Math.max(0, cards.size() - 1);
        }
    }

    private void readDetails(Card c, ItemStack stack) {
        if (stack == null) {
            c.name = "";
            c.owner = "";
            c.rawDisplay = "";
            c.loreClass = null;
            c.profile = null;
            return;
        }
        c.rawDisplay = stack.getDisplayName() == null ? "" : stack.getDisplayName();
        c.profile = skullProfile(stack);
        String taggedOwner = ownerName(stack, "");
        c.owner = LeapIdentity.canonicalUsername(taggedOwner);
        if (c.owner == null && c.profile != null) {
            c.owner = LeapIdentity.canonicalUsername(c.profile.getName());
        }
        c.name = LeapText.normalize(c.rawDisplay);
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        List<String> lore = player == null ? null : stack.getTooltip(player, false);
        c.loreClass = LeapText.classFromLore(lore);
    }

    /** The skull owner's profile from compound-form NBT, or null. Never throws. */
    static GameProfile skullProfile(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("SkullOwner", 10)) {
            return null;
        }
        try {
            GameProfile profile = NBTUtil.readGameProfileFromNBT(tag.getCompoundTag("SkullOwner"));
            if (profile == null) {
                return null;
            }
            String name = LeapIdentity.canonicalUsername(profile.getName());
            return name != null || profile.getId() != null ? profile : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Resolves one card from a single scoreboard/tab snapshot and its identity-bound cache. */
    private void resolve(Card c, long nowMs, List<NetworkPlayerInfo> infos,
                         List<BoardEntry> boardEntries, Collection<String> names) {
        if (c.owner == null) {
            c.owner = LeapIdentity.usernameFromDisplay(c.rawDisplay, names);
        }
        NetworkPlayerInfo info = findPlayerInfo(c, infos);
        if (c.owner == null && info != null && info.getGameProfile() != null) {
            c.owner = LeapIdentity.canonicalUsername(info.getGameProfile().getName());
        }

        UUID uuid = c.profile == null ? null : c.profile.getId();
        if (uuid == null && info != null && info.getGameProfile() != null) {
            uuid = info.getGameProfile().getId();
        }
        c.identityKey = LeapIdentity.identityKey(uuid, c.owner, c.identityKey, c.identityUsername);
        if (LeapIdentity.canonicalUsername(c.owner) != null) {
            c.identityUsername = c.owner;
        }
        c.name = displayName(c.rawDisplay, info, c.owner);
        resolveSkin(c, info, nowMs);

        BoardDetection boardHit = findBoardEntry(boardEntries, c.owner, names);
        String boardRaw = boardHit == null ? null : boardHit.raw;
        String boardMember = boardHit == null ? null : boardHit.member;
        LeapClass fromBoard = boardHit == null ? null : boardHit.cls;
        String tabRaw = info == null ? null : tabText(info);
        String tabName = info == null || info.getGameProfile() == null
                ? c.owner : LeapIdentity.canonicalUsername(info.getGameProfile().getName());
        LeapClass fromTab = tabRaw == null || tabName == null ? null : classFrom(tabRaw, tabName, names, true);

        // Scoreboard class markers are the primary source. Tab data is only a fallback; a delayed or conflicting
        // tab refresh must not erase an authoritative scoreboard class.
        LeapClass detected = fromBoard != null ? fromBoard : fromTab;
        String source = fromBoard != null ? "scoreboard" : fromTab != null ? "tab" : "none";
        if (detected == null && c.loreClass != null) {
            detected = LeapClass.parse(c.loreClass);
            if (detected != null) {
                source = "lore";
            }
        }
        boolean conflict = fromBoard != null && fromTab != null && fromBoard != fromTab;
        LeapClassCache.Resolution resolution = classCache.resolve(c.identityKey, detected, source, nowMs);
        c.cls = resolution.value;
        c.clsText = LeapClass.displayText(c.cls == null ? null : c.cls.label());
        c.clsColor = c.cls == null ? LeapClass.UNKNOWN_COLOR : c.cls.color();
        c.rawBoard = boardRaw;
        c.boardMember = boardMember;
        c.rawTab = tabRaw;
        c.source = c.cls == null ? "none" : resolution.source;

        boolean lowerPriorityConflict = resolution.reason.startsWith("conflicting lower-priority ");
        if (resolution.changed || conflict || lowerPriorityConflict) {
            String reason = resolution.reason;
            if (conflict) {
                reason = "scoreboard/tab disagreement; scoreboard class was kept as authoritative. " + reason;
            }
            String diagnosticBoard = boardRaw == null ? summarizeScoreboard(boardEntries) : boardRaw;
            LeapDiagnostics.record(c.identityKey == null ? "slot:" + c.slot : c.identityKey,
                    c.owner, diagnosticBoard, fromBoard != null ? fromBoard : fromTab,
                    c.cls, c.source, reason);
        }
    }

    private static NetworkPlayerInfo findPlayerInfo(Card c, List<NetworkPlayerInfo> infos) {
        UUID itemId = c.profile == null ? null : c.profile.getId();
        if (itemId != null) {
            for (NetworkPlayerInfo info : infos) {
                GameProfile profile = info.getGameProfile();
                if (profile != null && itemId.equals(profile.getId())) {
                    return info;
                }
            }
            // A UUID is stronger than a display-name match. Do not attach another player's skin if it disagrees.
            return null;
        }
        if (c.owner == null) {
            return null;
        }
        for (NetworkPlayerInfo info : infos) {
            GameProfile profile = info.getGameProfile();
            if (profile != null && LeapIdentity.sameUsername(profile.getName(), c.owner)) {
                return info;
            }
        }
        return null;
    }

    /** Uses the formatted tab/item label when it names this player, preserving rank colours; otherwise uses the ID. */
    private static String displayName(String itemDisplay, NetworkPlayerInfo info, String owner) {
        String formatted = null;
        if (info != null && info.getDisplayName() != null) {
            formatted = info.getDisplayName().getFormattedText();
        }
        if (owner != null && LeapIdentity.containsUsername(formatted, owner)) {
            return LeapText.playerDisplayName(formatted, owner);
        }
        if (owner != null && LeapIdentity.containsUsername(itemDisplay, owner)) {
            return LeapText.playerDisplayName(itemDisplay, owner);
        }
        if (LeapIdentity.canonicalUsername(owner) != null) {
            return owner;
        }
        String fallback = LeapText.normalize(itemDisplay);
        return fallback.isEmpty() ? "Unknown" : fallback;
    }

    /** Matches only the canonical username or a whole username token in a scoreboard row; no substring fallback. */
    private static BoardDetection findBoardEntry(List<BoardEntry> entries, String owner, Collection<String> names) {
        if (owner == null) {
            return null;
        }
        BoardDetection first = null;
        for (BoardEntry entry : entries) {
            boolean identityMatch = LeapIdentity.sameUsername(entry.member, owner)
                    || LeapIdentity.containsUsername(entry.raw, owner)
                    || LeapIdentity.sameUsername(LeapIdentity.usernameFromDisplay(entry.member, names), owner);
            LeapClass detected = classFrom(entry.raw, owner, names, false);
            if (!identityMatch && detected == null) {
                continue;
            }
            BoardDetection hit = new BoardDetection(entry.member, entry.raw, detected);
            if (detected != null) {
                return hit;
            }
            if (first == null) {
                first = hit;
            }
        }
        return first;
    }

    private static LeapClass classFrom(String raw, String owner, Collection<String> names, boolean tabSource) {
        if (raw == null || owner == null) {
            return null;
        }
        if (tabSource) {
            LeapClass fromTabClass = LeapClass.parse(LeapText.classFromTab(raw, owner));
            if (fromTabClass != null) {
                return fromTabClass;
            }
        }
        return LeapText.classFromScoreboardLine(raw, owner, names);
    }

    private static List<NetworkPlayerInfo> playerInfos(Minecraft mc) {
        List<NetworkPlayerInfo> out = new ArrayList<NetworkPlayerInfo>();
        if (mc.getNetHandler() == null) {
            return out;
        }
        for (Object value : mc.getNetHandler().getPlayerInfoMap()) {
            if (value instanceof NetworkPlayerInfo) {
                out.add((NetworkPlayerInfo) value);
            }
        }
        return out;
    }

    /** Sidebar entries are scanned first, then team membership rows (prefix + member + suffix). */
    private static List<BoardEntry> scoreboardEntries(Scoreboard board) {
        List<BoardEntry> out = new ArrayList<BoardEntry>();
        Set<String> seen = new HashSet<String>();
        if (board == null) {
            return out;
        }
        ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
        if (sidebar != null) {
            Collection<Score> scores = board.getSortedScores(sidebar);
            if (scores != null) {
                for (Score score : scores) {
                    if (score != null) {
                        addBoardEntry(board, score.getPlayerName(), out, seen);
                    }
                }
            }
        }
        for (ScorePlayerTeam team : board.getTeams()) {
            for (String member : team.getMembershipCollection()) {
                addBoardEntry(board, member, out, seen);
            }
        }
        return out;
    }

    private static void addBoardEntry(Scoreboard board, String member, List<BoardEntry> out, Set<String> seen) {
        if (member == null) {
            return;
        }
        ScorePlayerTeam team = board.getPlayersTeam(member);
        String raw = ScorePlayerTeam.formatPlayerName(team, member);
        String key = member + '\u0000' + raw;
        if (seen.add(key)) {
            out.add(new BoardEntry(member, raw));
        }
    }

    private Collection<String> knownNames(List<NetworkPlayerInfo> infos, List<BoardEntry> entries) {
        List<String> out = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (NetworkPlayerInfo info : infos) {
            GameProfile profile = info.getGameProfile();
            if (profile != null) {
                addKnownName(out, seen, profile.getName());
            }
        }
        for (Card c : cards) {
            addKnownName(out, seen, c.owner);
            if (c.profile != null) {
                addKnownName(out, seen, c.profile.getName());
            }
        }
        for (BoardEntry entry : entries) {
            addKnownName(out, seen, entry.member);
        }
        return out;
    }

    private static void addKnownName(List<String> out, Set<String> seen, String value) {
        String name = LeapIdentity.canonicalUsername(value);
        if (name != null && seen.add(name.toLowerCase(Locale.ROOT))) {
            out.add(name);
        }
    }

    private static String summarizeScoreboard(List<BoardEntry> entries) {
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < entries.size() && i < 15; i++) {
            if (summary.length() > 0) {
                summary.append(" | ");
            }
            summary.append(LeapText.normalize(entries.get(i).raw));
        }
        if (summary.length() > 600) {
            return summary.substring(0, 600);
        }
        return summary.toString();
    }

    /** Uses only the matching tab profile or this skull's own profile; missing skins remain a visible letter tile. */
    private static void resolveSkin(Card c, NetworkPlayerInfo info, long nowMs) {
        ResourceLocation skin = null;
        if (info != null) {
            // This getter may start vanilla's asynchronous download; only use the location after it is ready.
            ResourceLocation tabSkin = info.getLocationSkin();
            if (info.hasLocationSkin()) {
                skin = tabSkin;
            }
        }
        if (skin == null && c.profile != null) {
            skin = skullSkin(c.profile, nowMs);
        }
        c.skin = skin;
        c.skinState = skin == null ? SKIN_UNAVAILABLE : SKIN_READY;
    }

    /** Loads this skull profile asynchronously, keyed by UUID when available, with a retry for unresolved misses. */
    private static ResourceLocation skullSkin(final GameProfile profile, long nowMs) {
        final String key = LeapIdentity.identityKey(profile.getId(), profile.getName());
        if (key == null) {
            return null;
        }
        ResourceLocation done = SKULL_SKINS.get(key);
        if (done != null) {
            return done;
        }

        Long lastRequest = SKULL_REQUESTED_AT.get(key);
        boolean shouldRequest = lastRequest == null
                ? SKULL_REQUESTED_AT.putIfAbsent(key, nowMs) == null
                : nowMs - lastRequest >= SKIN_RETRY_MS && SKULL_REQUESTED_AT.replace(key, lastRequest, nowMs);
        if (shouldRequest) {
            try {
                Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile,
                        new SkinManager.SkinAvailableCallback() {
                            @Override
                            public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation loc,
                                                      MinecraftProfileTexture texture) {
                                if (type == MinecraftProfileTexture.Type.SKIN && loc != null) {
                                    SKULL_SKINS.put(key, loc);
                                    SKULL_REQUESTED_AT.remove(key);
                                }
                            }
                        }, false);
            } catch (RuntimeException e) {
                // Leave a retry timestamp behind; a transient client-side failure is not a permanent miss.
                SKULL_REQUESTED_AT.put(key, System.currentTimeMillis());
            }
        }
        return null;
    }

    /** The formatted text of a tab-list entry, or the profile name when it has no display component. */
    private static String tabText(NetworkPlayerInfo info) {
        IChatComponent display = info.getDisplayName();
        if (display != null) {
            return display.getFormattedText();
        }
        return info.getGameProfile() == null ? null : info.getGameProfile().getName();
    }

    /** The skull's owner name, from either NBT form, or the display name when the skull has no owner tag. */
    static String ownerName(ItemStack stack, String fallback) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            if (tag.hasKey("SkullOwner", 8)) {
                String n = tag.getString("SkullOwner").trim();
                if (!n.isEmpty()) {
                    return n;
                }
            } else if (tag.hasKey("SkullOwner", 10)) {
                String n = tag.getCompoundTag("SkullOwner").getString("Name").trim();
                if (!n.isEmpty()) {
                    return n;
                }
            }
        }
        return fallback == null ? "" : fallback;
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!stillOpen()) {
            mc.displayGuiScreen(null);
            return;
        }
        refreshIfNeeded();
        layout();

        // Keep the world visible behind the four compact rounded cards.
        FontRenderer fr = fr();
        int hovered = -1;
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (LeapLayout.containsRoundedRect(mx, my, c.x, c.y, cardW, cardH, LeapPalette.CARD_RADIUS)) {
                hovered = i;
            }
        }
        if (hovered >= 0) {
            highlight = hovered;
        }

        drawCardBackgrounds();
        drawHeads();
        drawCardText(fr);

        if (cards.isEmpty()) {
            String none = "No teammates in this menu";
            fr.drawStringWithShadow(none, (width - fr.getStringWidth(none)) / 2, height / 2 - 4, TEXT_DIM);
        }
        if (preview) {
            String hint = "TEST DATA - N/P cycles sample classes - Esc returns";
            hint = fr.trimStringToWidth(hint, Math.max(0, width - 12));
            fr.drawStringWithShadow(hint, (width - fr.getStringWidth(hint)) / 2, 4, TEXT_DIM);
        }
        super.drawScreen(mx, my, partialTicks);
    }

    private void layout() {
        int n = cards.size();
        cols = LeapLayout.COLS;
        int rows = LeapLayout.rows(n);
        int topInset = preview ? TEST_LABEL_HEIGHT : 0;
        cardW = LeapLayout.cardWidth(width, height);
        cardH = LeapLayout.cardHeight(width, height, rows, topInset);
        headSize = LeapLayout.headSize(cardW, cardH);
        gapX = LeapLayout.horizontalGap(width);
        gapY = LeapLayout.verticalGap(height, rows);

        int gridH = LeapLayout.gridHeight(cardH, rows, gapY);
        int availableHeight = Math.max(0, height - topInset);
        gridY = topInset + Math.max(0, (availableHeight - gridH) / 2);
        for (int i = 0; i < n; i++) {
            Card c = cards.get(i);
            int row = i / cols;
            int firstInRow = row * cols;
            int cardsInRow = Math.min(cols, n - firstInRow);
            int rowWidth = LeapLayout.gridWidth(cardW, cardsInRow, gapX);
            int rowX = (width - rowWidth) / 2;
            int column = i - firstInRow;
            c.x = rowX + column * (cardW + gapX);
            c.y = gridY + row * (cardH + gapY);
        }
    }

    private void drawCardBackgrounds() {
        int previousSegmentCap = UIRender.segCap;
        UIRender.segCap = UIRender.MAX_SEG;
        UIRender.beginFrame();
        try {
            for (int i = 0; i < cards.size(); i++) {
                Card c = cards.get(i);
                boolean selected = i == highlight;
                UIRender.roundRectBorder(c.x, c.y, cardW, cardH, LeapPalette.CARD_RADIUS,
                        selected ? CARD_HOVER : CARD_FILL,
                        selected ? CARD_HOVER : CARD_FILL,
                        selected ? CARD_SELECT_EDGE : CARD_EDGE);
            }
        } finally {
            try {
                UIRender.endFrame();
            } finally {
                UIRender.segCap = previousSegmentCap;
            }
        }
    }

    private void drawHeads() {
        for (Card c : cards) {
            int headLeft = LeapLayout.headLeft(cardW);
            int hx = c.x + headLeft;
            int hy = c.y + (cardH - headSize) / 2;
            if (c.skinState == SKIN_READY && c.skin != null) {
                SkinHead.draw(c.skin, hx, hy, headSize, 1f);
            } else {
                // Keep a visible, identity-specific fallback while the matching skin texture is unavailable.
                drawRect(hx, hy, hx + headSize, hy + headSize, HEAD_LETTER);
                String sourceName = c.owner == null || c.owner.isEmpty()
                        ? LeapText.normalize(c.name) : c.owner;
                String letter = sourceName.isEmpty() ? "?"
                        : sourceName.substring(0, 1).toUpperCase(Locale.ROOT);
                FontRenderer fr = fr();
                fr.drawString(letter, hx + (headSize - fr.getStringWidth(letter)) / 2,
                        hy + (headSize - 8) / 2, TEXT);
                drawBorder(hx, hy, headSize, headSize, CARD_EDGE);
            }
        }
    }

    private void drawCardText(FontRenderer fr) {
        int maxW = LeapLayout.textWidth(cardW, headSize);
        if (maxW <= 0) {
            return;
        }

        for (Card c : cards) {
            int textX = c.x + LeapLayout.headLeft(cardW) + headSize + LeapLayout.headGap(cardW);
            String fallbackName = c.owner == null || c.owner.isEmpty() ? "Unknown" : c.owner;
            String name = c.name == null || LeapText.normalize(c.name).isEmpty() ? fallbackName : c.name;
            String canonical = LeapIdentity.canonicalUsername(c.owner);
            if (fr.getStringWidth(name) * NAME_SCALE > maxW && canonical != null) {
                name = canonical;
            }

            String[] nameLines = new String[]{name};
            int nameWidth = fr.getStringWidth(name);
            float nameScale = LeapLayout.fittedScale(nameWidth, maxW, NAME_SCALE);
            if (nameScale < MIN_SINGLE_LINE_NAME_SCALE && canonical != null) {
                String[] wrapped = splitUsername(fr, canonical);
                if (wrapped != null) {
                    name = canonical;
                    nameLines = wrapped;
                    int widestLine = Math.max(fr.getStringWidth(nameLines[0]), fr.getStringWidth(nameLines[1]));
                    nameScale = LeapLayout.fittedScale(widestLine, maxW, NAME_SCALE);
                }
            }

            String classText = c.clsText == null || c.clsText.isEmpty() ? "Unknown class" : c.clsText;
            float classScale = LeapLayout.fittedScale(fr.getStringWidth(classText), maxW, CLASS_SCALE);
            // Keep the name visually dominant even when a long username must use a smaller fitted scale.
            classScale = Math.min(classScale, nameScale * CLASS_RELATIVE_SCALE);

            float maxTextHeight = Math.max(0f, cardH - 2f * TEXT_VERTICAL_PADDING);
            float nameBlockHeight = nameLines.length * FONT_LINE_HEIGHT * nameScale
                    + (nameLines.length - 1) * NAME_LINE_GAP;
            float blockHeight = nameBlockHeight + CLASS_TEXT_GAP + FONT_LINE_HEIGHT * classScale;
            float fixedGaps = CLASS_TEXT_GAP + (nameLines.length - 1) * NAME_LINE_GAP;
            if (blockHeight > maxTextHeight && blockHeight > fixedGaps) {
                float heightFactor = maxTextHeight <= fixedGaps ? 0f
                        : (maxTextHeight - fixedGaps) / (blockHeight - fixedGaps);
                nameScale *= heightFactor;
                classScale *= heightFactor;
                nameBlockHeight = nameLines.length * FONT_LINE_HEIGHT * nameScale
                        + (nameLines.length - 1) * NAME_LINE_GAP;
                blockHeight = nameBlockHeight + CLASS_TEXT_GAP + FONT_LINE_HEIGHT * classScale;
            }

            float nameTop = c.y + (cardH - blockHeight) * 0.5f;
            float lineY = nameTop;
            int nameColor = c.cls == null ? TEXT : c.clsColor;
            for (String nameLine : nameLines) {
                drawScaledText(fr, nameLine, textX, Math.round(lineY), nameColor, nameScale);
                lineY += FONT_LINE_HEIGHT * nameScale + NAME_LINE_GAP;
            }

            int classY = Math.round(nameTop + nameBlockHeight + CLASS_TEXT_GAP);
            int classColor = c.cls == null ? TEXT_DIM : c.clsColor;
            drawScaledText(fr, classText, textX, classY, classColor, classScale);
        }
    }

    /** Splits a long, canonical username into two fitted lines instead of shrinking it to unreadability. */
    private static String[] splitUsername(FontRenderer fr, String username) {
        if (username == null || username.length() < 2) {
            return null;
        }
        int halfWidth = Math.max(1, (fr.getStringWidth(username) + 1) / 2);
        String first = fr.trimStringToWidth(username, halfWidth);
        if (first.isEmpty() || first.length() >= username.length()) {
            return null;
        }
        String second = username.substring(first.length());
        return second.isEmpty() ? null : new String[]{first, second};
    }

    /** Uniformly enlarges vanilla text and restores the OpenGL matrix even if rendering fails. */
    private static void drawScaledText(FontRenderer fr, String text, int x, int y, int color, float scale) {
        if (text == null || text.isEmpty() || scale <= 0f) {
            return;
        }
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate((float) x, (float) y, 0f);
            GlStateManager.scale(scale, scale, 1f);
            fr.drawString(text, 0, 0, color);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static FontRenderer fr() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    /** One-pixel square border retained for the fallback head tile. */
    private static void drawBorder(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) {
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (LeapLayout.containsRoundedRect(mx, my, c.x, c.y, cardW, cardH, LeapPalette.CARD_RADIUS)) {
                pick(i);
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            closeMenu();
            return;
        }
        if (preview && code == Keyboard.KEY_N) {
            cyclePreviewSamples(1);
            return;
        }
        if (preview && code == Keyboard.KEY_P) {
            cyclePreviewSamples(-1);
            return;
        }
        if (cards.isEmpty()) {
            return;
        }
        switch (code) {
            case Keyboard.KEY_LEFT:
                highlight = Math.max(0, highlight - 1);
                return;
            case Keyboard.KEY_RIGHT:
                highlight = Math.min(cards.size() - 1, highlight + 1);
                return;
            case Keyboard.KEY_UP:
                highlight = Math.max(0, highlight - cols);
                return;
            case Keyboard.KEY_DOWN:
                highlight = Math.min(cards.size() - 1, highlight + cols);
                return;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
            case Keyboard.KEY_SPACE:
                pick(highlight);
                return;
            default:
                break;
        }
    }

    /**
     * Sends the same window click vanilla sends for a left click on the teammate's slot. After a pick the screen
     * waits for the server (pending), so a double click or a repeated Enter cannot send a second leap.
     */
    private void pick(int index) {
        if (preview || index < 0 || index >= cards.size()) {
            return;
        }
        long now = System.nanoTime();
        if (pendingUntilNs != 0L) {
            if (now < pendingUntilNs) {
                return;
            }
            pendingUntilNs = 0L;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController == null || mc.thePlayer == null || !stillOpen()) {
            return;
        }
        pendingUntilNs = now + PENDING_NS;
        highlight = index;
        mc.playerController.windowClick(container.windowId, cards.get(index).slot, 0, 0, mc.thePlayer);
    }

    /** Live menus close through vanilla; preview menus simply return to the GUI that opened them. */
    private void closeMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        if (preview) {
            if (mc.currentScreen == this) {
                mc.displayGuiScreen(previewParent);
            }
            return;
        }
        EntityPlayerSP p = mc.thePlayer;
        if (p != null && p.openContainer == container) {
            p.closeScreen();
        }
        if (mc.currentScreen == this) {
            mc.displayGuiScreen((GuiScreen) null);
        }
    }
}
