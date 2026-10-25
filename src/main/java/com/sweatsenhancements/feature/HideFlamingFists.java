package com.sweatsenhancements.feature;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.sweatsenhancements.SweatsEnhancements;
import com.sweatsenhancements.config.Cfg;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Scoped render-only filter for the cosmetic's known skull texture. */
public final class HideFlamingFists {
    // Texture identity is item metadata, not a player UUID or a generic armor-stand rule.
    private static final String TEXTURE = "7717933c40fbf936aa9288513efe19bda4601efc0e4ecad2e023b0c1d28444b";

    public static boolean matchesEncodedTexture(String value) {
        if (value == null || value.length() > 8192) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            return decoded.contains("/texture/" + TEXTURE);
        } catch (IllegalArgumentException badBase64) {
            return false;
        }
    }

    public static boolean matchesSkull(ItemStack head) {
        if (head == null || head.getItem() != Items.skull || !head.hasTagCompound()) return false;
        NBTTagCompound root = head.getTagCompound();
        if (root == null || !root.hasKey("SkullOwner", 10)) return false;
        NBTTagCompound owner = root.getCompoundTag("SkullOwner");
        if (!owner.hasKey("Properties", 10)) return false;
        NBTTagCompound properties = owner.getCompoundTag("Properties");
        if (!properties.hasKey("textures", 9)) return false;
        NBTTagList textures = properties.getTagList("textures", 10);
        for (int i = 0; i < textures.tagCount(); i++) {
            NBTTagCompound entry = textures.getCompoundTagAt(i);
            if (matchesEncodedTexture(entry.getString("Value"))) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onRender(RenderLivingEvent.Pre<?> event) {
        try {
            if (event == null || SweatsEnhancements.cfg == null || !SweatsEnhancements.cfg.getBool(Cfg.key("MS217"), false)
                    || !(event.entity instanceof EntityArmorStand)
                    || event.entity.worldObj == null || !event.entity.worldObj.isRemote) return;
            EntityArmorStand stand = (EntityArmorStand) event.entity;
            if (matchesSkull(stand.getEquipmentInSlot(4))) event.setCanceled(true);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] Flaming Fists filter failed: " + t);
        }
    }
}
