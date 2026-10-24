package com.sweatsenhancements;


import com.sweatsenhancements.chat.ChatLog;
import com.sweatsenhancements.cmd.CommandBridge;
import com.sweatsenhancements.config.Cfg;
import com.sweatsenhancements.feature.BobberTimer;
import com.sweatsenhancements.feature.ChatTools;
import com.sweatsenhancements.feature.CustomScoreboard;
import com.sweatsenhancements.feature.CustomTabList;
import com.sweatsenhancements.feature.CurrentPetHud;
import com.sweatsenhancements.feature.EntityTools;
import com.sweatsenhancements.feature.Fullbright;
import com.sweatsenhancements.feature.HudTools;
import com.sweatsenhancements.feature.TabListReader;
import com.sweatsenhancements.gui.MoulMenu;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * SweatsEnhancements - quality of life for Fakepixel SkyBlock on 1.8.9.
 *
 * The config menu is drawn by MoulConfig, a config screen library published separately, which is
 * also why it looks the way it does. Planned-only entries remain in the source
 * catalog, not the live settings menu; a switch that does nothing is worse than none.
 *
 * One installable jar; client mixins used only for pre-drop safety and visual camera smoothing.
 */
@Mod(modid = SweatsEnhancements.MODID, version = SweatsEnhancements.VERSION, acceptedMinecraftVersions = "[1.8.9]",
     clientSideOnly = true)
public class SweatsEnhancements {

    public static final String MODID = "sweatsenhancements";
    public static final String VERSION = "0.0.1";

    public static Minecraft mc;
    public static Cfg cfg;
    public static ChatLog chat;
    public static ChatTools chatTools;
    public static HudTools hud;
    public static CurrentPetHud currentPet;
    public static TabListReader tabList;
    public static CustomScoreboard scoreboard;
    public static CustomTabList tabListOverlay;
    public static com.sweatsenhancements.feature.DungeonCombatVisuals dungeonVisuals;
    public static com.sweatsenhancements.feature.DungeonVisualAdditions dungeonAdditions;
    public static com.sweatsenhancements.feature.DungeonFloorSixInfo dungeonInfo;
    public static com.sweatsenhancements.feature.nef.SlayerMobsDisplay slayerMobs;


    /** the accessory power's own listener, held so both buses can be given it */
    private static com.sweatsenhancements.feature.PowerStone power;
    private static Fullbright brightness;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        mc = Minecraft.getMinecraft();
        com.sweatsenhancements.config.LegacyConfigMigration.run(event.getModConfigurationDirectory());
        cfg = new Cfg(event.getModConfigurationDirectory());
        cfg.load();
        com.sweatsenhancements.feature.nef.exact.SlayerSettingsStore.load();
        com.sweatsenhancements.feature.ProtectedItems.load(event.getModConfigurationDirectory());
        // a file written before this build's lists changed is brought forward before anything reads
        // them: the panel draws from these lists on the first frame
        com.sweatsenhancements.config.Migrate.run(cfg);
        chat = new ChatLog();
        chatTools = new ChatTools();
        hud = new HudTools();
        currentPet = new CurrentPetHud();
        chatTools.init(event.getModConfigurationDirectory());
        tabList = new TabListReader();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        scoreboard = new CustomScoreboard();
        dungeonVisuals = new com.sweatsenhancements.feature.DungeonCombatVisuals();
        dungeonInfo = new com.sweatsenhancements.feature.DungeonFloorSixInfo();
        tabListOverlay = new CustomTabList();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(chat);
        MinecraftForge.EVENT_BUS.register(chatTools);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.RequestedChatFilters());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.DungeonBlessingFilter());
        dungeonAdditions = new com.sweatsenhancements.feature.DungeonVisualAdditions();
        MinecraftForge.EVENT_BUS.register(dungeonAdditions);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.ServerAnnouncementFilter());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.BatchSoundTweaks());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.SkyblockMenuMiddleClick());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.PreventAbilityPlacement());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.UnclaimedContestHighlight());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.ActiveWardrobeHighlight());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.HideFlamingFists());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.ItemDataTooltips());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.BlankGlassTooltips());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.ProtectedItems());
        com.sweatsenhancements.feature.SmoothTeleportView smoothTeleport = new com.sweatsenhancements.feature.SmoothTeleportView();
        MinecraftForge.EVENT_BUS.register(smoothTeleport);
        FMLCommonHandler.instance().bus().register(smoothTeleport);
        com.sweatsenhancements.feature.AutoCloseSecretChest autoClose = new com.sweatsenhancements.feature.AutoCloseSecretChest();
        MinecraftForge.EVENT_BUS.register(autoClose);
        FMLCommonHandler.instance().bus().register(autoClose);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.BlockOutline());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.ClickRateHud());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.CopyChatMessage());
        MinecraftForge.EVENT_BUS.register(new EntityTools());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.WardrobeSlotKeys());
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.RainVisual());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.MuteTeleportItemSound());
        com.sweatsenhancements.feature.GroundBottleHider bottleHider = new com.sweatsenhancements.feature.GroundBottleHider();
        MinecraftForge.EVENT_BUS.register(bottleHider);
        FMLCommonHandler.instance().bus().register(bottleHider);
        com.sweatsenhancements.feature.HideFallingBlocks fallingBlocks = new com.sweatsenhancements.feature.HideFallingBlocks();
        MinecraftForge.EVENT_BUS.register(fallingBlocks);
        FMLCommonHandler.instance().bus().register(fallingBlocks);
        brightness = new Fullbright();
        MinecraftForge.EVENT_BUS.register(brightness);
        MinecraftForge.EVENT_BUS.register(hud);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.CoreShortcuts());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.ClickedSecrets());
        com.sweatsenhancements.feature.NoCursorReset cursorKeeper = new com.sweatsenhancements.feature.NoCursorReset();
        MinecraftForge.EVENT_BUS.register(cursorKeeper);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.DungeonRunHud());
        MinecraftForge.EVENT_BUS.register(dungeonVisuals);
        MinecraftForge.EVENT_BUS.register(dungeonInfo);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.DungeonScoreHud());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.DungeonPassFloorTip());
        com.sweatsenhancements.feature.DungeonSmallQoL dungeonQoL = new com.sweatsenhancements.feature.DungeonSmallQoL();
        MinecraftForge.EVENT_BUS.register(dungeonQoL);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.DungeonAlertCard());
        com.sweatsenhancements.gui.SpiritLeapMenuListener leapMenu = new com.sweatsenhancements.gui.SpiritLeapMenuListener();
        MinecraftForge.EVENT_BUS.register(leapMenu);
        FMLCommonHandler.instance().bus().register(leapMenu);
        FMLCommonHandler.instance().bus().register(dungeonQoL);
        com.sweatsenhancements.feature.nef.exact.SlayerData slayerData = new com.sweatsenhancements.feature.nef.exact.SlayerData();
        MinecraftForge.EVENT_BUS.register(slayerData);
        FMLCommonHandler.instance().bus().register(slayerData);
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.nef.SlayerOverlay());
        // Direct NEF LGPL ports: replace the two earlier substitute handlers.
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.nef.HellionShieldFilter());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.nef.AutoOpenMaddox());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.nef.FirePillarDisplay());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.nef.VoidgloomSeraph());
        MinecraftForge.EVENT_BUS.register(new com.sweatsenhancements.feature.nef.BlazeAttunements());
        slayerMobs = new com.sweatsenhancements.feature.nef.SlayerMobsDisplay();
        MinecraftForge.EVENT_BUS.register(slayerMobs);
        com.sweatsenhancements.feature.nef.SlayerHealthDisplay slayerHealth = new com.sweatsenhancements.feature.nef.SlayerHealthDisplay();
        MinecraftForge.EVENT_BUS.register(slayerHealth);
        FMLCommonHandler.instance().bus().register(slayerHealth);
        com.sweatsenhancements.feature.nef.SlayerTimer nefSlayerTimer = new com.sweatsenhancements.feature.nef.SlayerTimer();
        MinecraftForge.EVENT_BUS.register(nefSlayerTimer);
        FMLCommonHandler.instance().bus().register(nefSlayerTimer);
        MinecraftForge.EVENT_BUS.register(new BobberTimer());
        power = new com.sweatsenhancements.feature.PowerStone();
        MinecraftForge.EVENT_BUS.register(power);
        MinecraftForge.EVENT_BUS.register(tabList);
        MinecraftForge.EVENT_BUS.register(scoreboard);
        MinecraftForge.EVENT_BUS.register(tabListOverlay);

        // 1.8.9 fires the tick events on FML's own bus, not on the Forge bus, and every object
        // below that waits for a tick has been waiting on a bus that never carried one. The
        // evidence is in the player's own dump: the tab list reader reports "no tab list data yet"
        // on a screen whose tab list is full, because its tick handler has never run once. The
        // same bus carries the other tick work - the config's flush, the tab list's own cache,
        // the sidebar's repair - and none of it has run either. Registering on both buses is
        // harmless: an object only ever receives the events it declares a handler for, and each
        // bus only fires its own.
        FMLCommonHandler.instance().bus().register(this);
        FMLCommonHandler.instance().bus().register(chat);
        FMLCommonHandler.instance().bus().register(chatTools);
        FMLCommonHandler.instance().bus().register(hud);
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.CoreShortcuts());
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.DungeonRequeue());
        FMLCommonHandler.instance().bus().register(cursorKeeper);
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.DungeonRunHud());
        FMLCommonHandler.instance().bus().register(dungeonVisuals);
        FMLCommonHandler.instance().bus().register(dungeonInfo);
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.DungeonScoreHud());
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.ClickRateHud());
        FMLCommonHandler.instance().bus().register(currentPet);
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.AlwaysSprint());
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.ConfigShortcuts());
        FMLCommonHandler.instance().bus().register(new com.sweatsenhancements.feature.SafeguardKey());
        FMLCommonHandler.instance().bus().register(brightness);
        FMLCommonHandler.instance().bus().register(tabList);
        FMLCommonHandler.instance().bus().register(scoreboard);
        FMLCommonHandler.instance().bus().register(tabListOverlay);
        FMLCommonHandler.instance().bus().register(power);

        ClientCommandHandler.instance.registerCommand(new CommandBridge());
        ClientCommandHandler.instance.registerCommand(new com.sweatsenhancements.cmd.CopyChatCommand());

        System.out.println("[SweatsEnhancements] SweatsEnhancements " + VERSION + " loaded ("
                + com.sweatsenhancements.config.Implemented.IDS.size() + " features live, "
                + com.sweatsenhancements.config.Features.all().size() + " listed).");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MoulMenu.tick();
        // A setting changed since the last write is written out here, at most every couple of
        // seconds. Writing on every change is what made the menu hitch - a slider being dragged
        // changes a value every frame, and the config file was being rewritten every frame with it.
        try {
            if (cfg != null) cfg.flushIfDirty(2000L);
        } catch (Throwable t) {
            // a failed flush is logged by Cfg itself; the next one tries again
        }
    }

}
