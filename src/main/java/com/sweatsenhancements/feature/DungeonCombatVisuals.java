package com.sweatsenhancements.feature;

import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Visual-only dungeon cues. Never clicks, targets, attacks, or predicts unobserved spawns. */
public final class DungeonCombatVisuals {
    private static final Pattern KEY_COUNT = Pattern.compile("(?i)\\bKeys:\\s*.*?([0-9]+)x\\b");
    private static final String[] BLOOD_NAMES = {"putrid", "reaper", "vader", "frost", "cannibal",
            "revoker", "tear", "mr. dead", "skull", "walker", "psycho", "ooze", "freak",
            "flamer", "mute", "leech", "parasite"};
    private final List<EntityArmorStand> starredLabels = new ArrayList<EntityArmorStand>();
    private final List<EntityArmorStand> bloodLabels = new ArrayList<EntityArmorStand>();
    private final List<EntityLivingBase> starred = new ArrayList<EntityLivingBase>();
    private final List<EntityLivingBase> bloodMobs = new ArrayList<EntityLivingBase>();
    private final List<EntityArmorStand> felHeads = new ArrayList<EntityArmorStand>();
    private final List<EntityBat> bats = new ArrayList<EntityBat>();
    private final List<EntityArmorStand> keyStands = new ArrayList<EntityArmorStand>();
    private final Set<BlockPos> doors = new HashSet<BlockPos>();
    private Object world;
    private long lastScan;
    private int keys = -1;
    private long lastDoorScan;
    private long lastDungeonSeen;
    private int doorColumn;
    private long bloodStart, bloodEnd;
    private long bloodRushMillis = -1;
    private long runElapsedMillis = -1;
    private int chatCursor;
    private boolean bloodReady;

    public static int keyCount(String sidebar) {
        if (sidebar == null) return -1;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(sidebar);
        Matcher m = KEY_COUNT.matcher(clean == null ? "" : clean);
        if (!m.find()) return -1;
        try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ex) { return -1; }
    }

    public static boolean starred(String name) { return name != null && name.contains("✮"); }
    public static boolean starLabelAlive(String name) {
        if (!starred(name)) return false;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        return plain != null && !plain.matches(".*(?:^|\\s)0(?:/[^\\s]+)?❤.*");
    }
    public static boolean keyStand(String name) {
        if (name == null) return false;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        String n = clean == null ? "" : clean.trim().toLowerCase(Locale.ROOT);
        return n.equals("wither key") || n.equals("blood key");
    }
    public static boolean bloodMob(String name) {
        if (name == null || !name.contains("✰")) return false;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        String n = clean == null ? "" : clean.toLowerCase(Locale.ROOT);
        for (String part : BLOOD_NAMES) if (n.contains(part)) return true;
        return false;
    }
    public static boolean bloodStart(String chat) {
        String n = chat == null ? "" : chat.toLowerCase(Locale.ROOT);
        return n.contains("blood door") && n.contains("open") && !n.contains("right click");
    }
    public static boolean bloodReady(String chat) {
        String n = chat == null ? "" : chat.toLowerCase(Locale.ROOT);
        return n.contains("watcher") && (n.contains("enough for now") || n.contains("you may pass"));
    }
    private boolean option(String id) { return SweatsEnhancements.cfg != null && SweatsEnhancements.cfg.getBool(Cfg.key(id), false); }
    private boolean inDungeon() {
        if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null || SweatsEnhancements.mc.thePlayer == null) return false;
        long now = System.currentTimeMillis();
        // The custom scoreboard removes the vanilla display objective. Read its raw server rows
        // directly, including from render/chat callbacks when client tick listeners have not run.
        // One observed F/M floor parser for the existing keys, doors and mob boxes.
        if (DungeonFloorSixInfo.floorFromRows(sidebar()) > 0) {
            lastDungeonSeen = now;
            return true;
        }
        return world == SweatsEnhancements.mc.theWorld && now - lastDungeonSeen < 3000;
    }

    private void reset() {
        starred.clear(); starredLabels.clear(); bloodLabels.clear(); bloodMobs.clear(); keyStands.clear();
        felHeads.clear(); bats.clear(); doors.clear();
        keys = -1; bloodStart = bloodEnd = lastScan = lastDoorScan = lastDungeonSeen = 0;
        doorColumn = 0; bloodReady = false; world = null; bloodRushMillis = -1;
        runElapsedMillis = -1; chatCursor = SweatsEnhancements.chat == null ? 0 : SweatsEnhancements.chat.size();
    }

    private void refresh() {
        try {
            if (SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null) { if (world != null) reset(); return; }
            if (world != SweatsEnhancements.mc.theWorld) { reset(); world = SweatsEnhancements.mc.theWorld; }
            if (!inDungeon()) { if (world != null) reset(); return; }
            long now = System.currentTimeMillis();
            if (now - lastScan < 250) return;
            lastScan = now;
            keys = -1;
            for (String row : sidebar()) {
                int count = keyCount(row);
                if (count >= 0) keys = count;
                long elapsed = dungeonElapsed(row);
                if (elapsed >= 0) runElapsedMillis = elapsed;
            }
            // ChatLog is a second observer: process missed events after the renderer sees
            // the custom scoreboard, not solely at the instant the chat callback arrives.
            if (SweatsEnhancements.chat != null) {
                java.util.List<String> history = SweatsEnhancements.chat.raw();
                for (int i=Math.max(chatCursor, history.size()-100); i<history.size(); i++)
                    acceptBloodChat(history.get(i), now);
                chatCursor = history.size();
            }
            if (option("DU006") || option("DU007") || option("DU009") || option("DU012") || option("DU013")) scanEntities();
            if (option("DU008") && now - lastDoorScan >= 350) { scanDoors(); lastDoorScan = now; }
            else if (!option("DU008")) doors.clear();
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon visual scan failed: " + t); }
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) refresh();
    }

    private List<String> sidebar() {
        if (CustomScoreboard.on() && SweatsEnhancements.scoreboard != null) return SweatsEnhancements.scoreboard.serverSidebarLines();
        List<String> rows = new ArrayList<String>();
        Scoreboard board = SweatsEnhancements.mc.theWorld.getScoreboard();
        ScoreObjective obj = board == null ? null : board.getObjectiveInDisplaySlot(1);
        if (obj != null) for (Score score : board.getSortedScores(obj)) {
            String name = score.getPlayerName();
            if (name != null) rows.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(name), name));
        }
        return rows;
    }

    private void scanEntities() {
        starred.clear(); bloodMobs.clear(); keyStands.clear(); starredLabels.clear(); bloodLabels.clear();
        felHeads.clear(); bats.clear();
        List<EntityArmorStand> starNames = starredLabels;
        List<EntityArmorStand> bloodNames = bloodLabels;
        List<EntityLivingBase> living = new ArrayList<EntityLivingBase>();
        for (Entity entity : SweatsEnhancements.mc.theWorld.loadedEntityList) {
            if (entity == null || entity.isDead || entity.getDistanceSqToEntity(SweatsEnhancements.mc.thePlayer) > 16384) continue;
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand stand = (EntityArmorStand) entity;
                String name = stand.getName();
                if (keyStand(name)) keyStands.add(stand);
                if (starred(name)) starNames.add(stand);
                if (bloodMob(name)) bloodNames.add(stand);
                if (option("DU012") && felHead(stand)) felHeads.add(stand);
            } else if (entity instanceof EntityLivingBase && entity != SweatsEnhancements.mc.thePlayer
                    && ((EntityLivingBase) entity).getHealth() > 0.1f) {
                living.add((EntityLivingBase) entity);
                if (entity instanceof EntityBat) bats.add((EntityBat) entity);
            }
        }
        for (EntityArmorStand name : starNames) {
            EntityLivingBase match = nearest(name, living);
            if (match != null && !starred.contains(match)) starred.add(match);
        }
        for (EntityArmorStand name : bloodNames) {
            EntityLivingBase match = nearest(name, living);
            if (match != null && !bloodMobs.contains(match)) bloodMobs.add(match);
        }
    }

    // A distinctive dungeon skull texture distinguishes a hidden Fel from ordinary
    // named armor stands. Compare only the texture's URL hash, not its entire NBT.
    private static final String FEL_TEXTURE_HASH = "7a59bb0a7a32965b3d90d8eafa899d1835f424509eadd4e6b709ada50b9cf";
    private static boolean felHead(EntityArmorStand stand) {
        try {
            ItemStack head = stand.getEquipmentInSlot(4);
            if (head == null || !head.hasTagCompound()) return false;
            NBTTagCompound skull = head.getTagCompound().getCompoundTag("SkullOwner");
            NBTTagList textures = skull.getCompoundTag("Properties").getTagList("textures", 10);
            if (textures.tagCount() == 0) return false;
            String encoded = textures.getCompoundTagAt(0).getString("Value");
            return felTexture(encoded);
        } catch (Throwable ignored) { return false; }
    }
    public static boolean felTexture(String encoded) {
        if (encoded == null || encoded.length() > 1024) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            return decoded.toLowerCase(Locale.ROOT).contains(FEL_TEXTURE_HASH);
        } catch (IllegalArgumentException ignored) { return false; }
    }
    public static String starColourKey(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.contains("stormy")) return "stormyColour";
        if (n.contains("withermancer")) return "witherColour";
        if (n.contains("zombie commander")) return "commanderColour";
        if (n.contains("skeleton master")) return "masterColour";
        return "starredColour";
    }
    public static boolean visibleStyle(String style) {
        return "Box".equals(style) || "Filled Box".equals(style);
    }
    private int mobColour(String suffix, String defaultValue) {
        try { return ChromaColour.forLegacyString(SweatsEnhancements.cfg.get("dungeonMobs." + suffix, defaultValue))
                .getEffectiveColourRGB(); }
        catch (Throwable ignored) { return ChromaColour.forLegacyString(defaultValue).getEffectiveColourRGB(); }
    }
    private String style(String suffix) { return SweatsEnhancements.cfg.get("dungeonMobs." + suffix, "Box"); }

    private static EntityLivingBase nearest(EntityArmorStand stand, List<EntityLivingBase> living) {
        EntityLivingBase best = null;
        double distance = 1.0; // paired mob shares the label’s X/Z, not a nearby unrelated mob
        for (EntityLivingBase entity : living) {
            double dx = entity.posX - stand.posX, dy = entity.posY - stand.posY, dz = entity.posZ - stand.posZ;
            double d = dx*dx + dz*dz + Math.max(0, Math.abs(dy)-2.5)*Math.max(0, Math.abs(dy)-2.5);
            if (Math.abs(dy) <= 3 && dx*dx + dz*dz <= .65 && d < distance) { best = entity; distance = d; }
        }
        return best;
    }

    private boolean doorBlock(BlockPos p) {
        IBlockState state = SweatsEnhancements.mc.theWorld.getBlockState(p);
        Block block = state.getBlock();
        return block == Blocks.coal_block || (block == Blocks.stained_hardened_clay
                && state.getValue(BlockColored.COLOR) == EnumDyeColor.RED);
    }
    private void scanDoors() {
        // Retain discovered structures beyond the current room and check them for opening.
        for (java.util.Iterator<BlockPos> it = doors.iterator(); it.hasNext();) {
            if (!doorBlock(it.next())) it.remove();
        }
        BlockPos centre = SweatsEnhancements.mc.thePlayer.getPosition();
        // Incremental loaded-area search: forty x columns per pass, covering +/-96 blocks
        // over several frames. Never request unloaded chunk data.
        for (int column = 0; column < 40; column++) {
            int x = centre.getX() - 96 + (doorColumn++ % 193);
            for (int z = centre.getZ()-96; z <= centre.getZ()+96; z++) {
            BlockPos base = new BlockPos(x, 69, z);
            if (!SweatsEnhancements.mc.theWorld.isBlockLoaded(base) || !doorBlock(base)) continue;
            boolean good = true;
            for (int dx=0; dx<3 && good; dx++) for (int dz=0; dz<3 && good; dz++)
                for (int dy=0; dy<4; dy++) if (!doorBlock(base.add(dx,dy,dz))) { good=false; break; }
            if (good) doors.add(base);
            }
        }
    }

    public static long dungeonElapsed(String row) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(row == null ? "" : row);
        if (clean == null) return -1;
        Matcher m = Pattern.compile("(?i)Time Elapsed:\\s*(\\d{1,2}):(\\d{2})(?::(\\d{2}))?s?").matcher(clean);
        if (!m.find()) return -1;
        long first = Long.parseLong(m.group(1)), second = Long.parseLong(m.group(2));
        return m.group(3) == null ? (first*60+second)*1000 :
                (first*3600+second*60+Long.parseLong(m.group(3)))*1000;
    }

    private void acceptBloodChat(String msg, long now) {
        if (bloodStart == 0 && bloodStart(msg)) {
            bloodStart = now;
            bloodRushMillis = runElapsedMillis;
        }
        if (bloodReady(msg) && !bloodReady) {
            bloodReady = true; bloodEnd = now;
            if (SweatsEnhancements.mc != null && SweatsEnhancements.mc.thePlayer != null) {
                if (option("DU011")) {
                    SweatsEnhancements.mc.ingameGUI.displayTitle("§cBLOOD READY!", "", 5, 45, 10);
                    SweatsEnhancements.mc.thePlayer.playSound("note.pling", 1f, 1f);
                    SweatsEnhancements.mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§c[SweatsEnhancements] BLOOD READY!"));
                }
                // Independent opt-in: server Watcher cue, once per run; never auto-ready the party.
                if (option("DU035")) SweatsEnhancements.mc.thePlayer.sendChatMessage("/pc Blood Ready!");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2 || event.message == null || SweatsEnhancements.mc == null || SweatsEnhancements.mc.theWorld == null) return;
        if (world != SweatsEnhancements.mc.theWorld) { reset(); world = SweatsEnhancements.mc.theWorld; }
        if (!inDungeon()) return;
        try { acceptBloodChat(event.message.getUnformattedText(), System.currentTimeMillis()); }
        catch (Throwable t) { System.out.println("[SweatsEnhancements] blood chat cue failed: " + t); }
    }

    public List<HudLayout.Line> timerLines() {
        List<HudLayout.Line> result = new ArrayList<HudLayout.Line>();
        if (!inDungeon()) return result;
        // Freeze the observed sidebar clock at blood-door opening and show the
        // elapsed phase separately; neither line predicts a spawn time.
        long rush = bloodRushMillis >= 0 ? bloodRushMillis : runElapsedMillis;
        if (rush >= 0) result.add(new HudLayout.Line("§cBlood Rush §f" + clock(rush), 0xffffff));
        if (bloodStart > 0) {
            long phase = (bloodEnd > 0 ? bloodEnd : System.currentTimeMillis()) - bloodStart;
            result.add(new HudLayout.Line((bloodReady ? "§aBlood Ready §f" : "§cBlood Phase §f")
                    + clock(Math.max(0,phase)), 0xffffff));
        }
        return result;
    }

    private static String clock(long millis) {
        long seconds = millis/1000;
        return String.format(Locale.ROOT, "%02d:%02d", seconds/60, seconds%60);
    }

    private static void box(AxisAlignedBB bb, double x, double y, double z, float red, float green, float blue) {
        AxisAlignedBB b = bb.expand(.06,.06,.06).offset(-x,-y,-z);
        // Translucent volume and visible outline in the local world-render pass.
        GL11.glColor4f(red, green, blue, .18f);
        GL11.glBegin(GL11.GL_QUADS);
        double a=b.minX,c=b.maxX,d=b.minY,e=b.maxY,f=b.minZ,g=b.maxZ;
        GL11.glVertex3d(a,d,f);GL11.glVertex3d(c,d,f);GL11.glVertex3d(c,d,g);GL11.glVertex3d(a,d,g);
        GL11.glVertex3d(a,e,f);GL11.glVertex3d(a,e,g);GL11.glVertex3d(c,e,g);GL11.glVertex3d(c,e,f);
        GL11.glVertex3d(a,d,f);GL11.glVertex3d(a,e,f);GL11.glVertex3d(c,e,f);GL11.glVertex3d(c,d,f);
        GL11.glVertex3d(a,d,g);GL11.glVertex3d(c,d,g);GL11.glVertex3d(c,e,g);GL11.glVertex3d(a,e,g);
        GL11.glVertex3d(a,d,f);GL11.glVertex3d(a,d,g);GL11.glVertex3d(a,e,g);GL11.glVertex3d(a,e,f);
        GL11.glVertex3d(c,d,f);GL11.glVertex3d(c,e,f);GL11.glVertex3d(c,e,g);GL11.glVertex3d(c,d,g);
        GL11.glEnd();
        GL11.glColor4f(red,green,blue,1f);
        RenderGlobal.drawSelectionBoundingBox(b);
    }
    private static void mobBox(AxisAlignedBB bb, double x, double y, double z,
                               int colour, boolean filled) {
        float r = ((colour >>> 16) & 255) / 255f;
        float g = ((colour >>> 8) & 255) / 255f;
        float b = (colour & 255) / 255f;
        if (filled) box(bb,x,y,z,r,g,b);
        else {
            GL11.glColor4f(r,g,b,1f);
            RenderGlobal.drawSelectionBoundingBox(bb.expand(.06,.06,.06).offset(-x,-y,-z));
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        refresh();
        if (!inDungeon() || SweatsEnhancements.mc.thePlayer == null) return;
        if (!option("DU006") && !option("DU007") && !option("DU008") && !option("DU009")
                && !option("DU012") && !option("DU013")) return;
        try {
            float t = event.partialTicks;
            Entity camera = SweatsEnhancements.mc.getRenderViewEntity();
            if (camera == null) return;
            double x = camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*t;
            double y = camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*t;
            double z = camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*t;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
            try {
                GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(false); GL11.glLineWidth(3f);
                boolean esp = SweatsEnhancements.cfg.getBool("dungeonMobs.starredEsp", true);
                if (!esp) GL11.glEnable(GL11.GL_DEPTH_TEST);
                try {
                    String starStyle = style("starredDisplay");
                    if (option("DU006") && visibleStyle(starStyle)) for (EntityArmorStand label : starredLabels) {
                        if (label.isDead || label.worldObj != SweatsEnhancements.mc.theWorld || !starLabelAlive(label.getName())) continue;
                        if (!esp && !SweatsEnhancements.mc.thePlayer.canEntityBeSeen(label)) continue;
                        EntityLivingBase mob = nearest(label, starred);
                        AxisAlignedBB area = mob != null && !mob.isDead && mob.worldObj == SweatsEnhancements.mc.theWorld
                                ? mob.getEntityBoundingBox()
                                : new AxisAlignedBB(label.posX-.5, label.posY-2.08, label.posZ-.5,
                                        label.posX+.5, label.posY+.05, label.posZ+.5);
                        String kind = starColourKey(label.getName());
                        String fallback = "starredColour".equals(kind) ? "0:255:148:255:255"
                                : "witherColour".equals(kind) ? "0:255:164:154:255"
                                : "commanderColour".equals(kind) ? "0:255:0:50:255"
                                : "masterColour".equals(kind) ? "0:255:127:0:255" : "0:255:210:228:255";
                        mobBox(area,x,y,z,mobColour(kind,fallback),"Filled Box".equals(starStyle));
                    }
                    String felStyle = style("felStyle");
                    if (option("DU012") && visibleStyle(felStyle)) for (EntityArmorStand head : felHeads) {
                        if (head.isDead || head.worldObj != SweatsEnhancements.mc.theWorld) continue;
                        if (!esp && !SweatsEnhancements.mc.thePlayer.canEntityBeSeen(head)) continue;
                        mobBox(new AxisAlignedBB(head.posX-.5, head.posY-1.5, head.posZ-.5,
                                head.posX+.5, head.posY+2, head.posZ+.5), x,y,z,
                                mobColour("felColour","0:255:148:255:255"), "Filled Box".equals(felStyle));
                    }
                    String batStyle = style("batStyle");
                    if (option("DU013") && visibleStyle(batStyle)) for (EntityBat bat : bats) {
                        if (bat.isDead || bat.worldObj != SweatsEnhancements.mc.theWorld) continue;
                        if (!esp && !SweatsEnhancements.mc.thePlayer.canEntityBeSeen(bat)) continue;
                        mobBox(bat.getEntityBoundingBox(),x,y,z,
                                mobColour("batColour","0:255:0:255:0"),"Filled Box".equals(batStyle));
                    }
                } finally { if (!esp) GL11.glDisable(GL11.GL_DEPTH_TEST); }
                if (option("DU009")) for (EntityLivingBase mob : bloodMobs)
                    if (!mob.isDead && mob.getHealth()>0.1f && mob.worldObj==SweatsEnhancements.mc.theWorld)
                        box(mob.getEntityBoundingBox(),x,y,z,1f,.2f,.35f);
                if (option("DU007")) for (EntityArmorStand key : keyStands) if (!key.isDead && key.worldObj==SweatsEnhancements.mc.theWorld)
                    box(new AxisAlignedBB(key.posX-.45,key.posY-.5,key.posZ-.45,key.posX+.45,key.posY+2,key.posZ+.45),x,y,z,.3f,1f,1f);
                if (option("DU008") && keys >= 0) for (BlockPos base : doors)
                    box(new AxisAlignedBB(base,base.add(3,4,3)),x,y,z,keys>0?.25f:1f,keys>0?1f:.2f,.2f);
            } finally { GL11.glPopAttrib(); }
        } catch (Throwable t) { System.out.println("[SweatsEnhancements] dungeon boxes failed: " + t); }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) { if (event.world != null && event.world.isRemote) reset(); }
}
