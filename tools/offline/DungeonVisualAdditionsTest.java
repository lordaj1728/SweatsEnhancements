import com.sweatsenhancements.feature.DungeonVisualAdditions;
public final class DungeonVisualAdditionsTest {
    static void ok(boolean b) { if (!b) throw new AssertionError("dungeon visual classification"); }
    public static void main(String[] args) {
        ok(DungeonVisualAdditions.bossFloor("§cBonzo §a12k❤")==1);
        // Supplied F1 dump: a decorative starred label above a glyph-named NPC,
        // and a remote Wither named Bonzo used as boss-bar state.
        ok(DungeonVisualAdditions.bossFloor("§e§l★§c§lBonzo §a245793§c❤ §e§l★")==1);
        ok(DungeonVisualAdditions.syntheticActorName("\uE000"));
        ok(DungeonVisualAdditions.canPairActor(true,"\uE000",false,2));
        ok(!DungeonVisualAdditions.canPairActor(true,"normalTeammate",false,2));
        ok(DungeonVisualAdditions.canPairActor(true,"Bonzo",true,1));
        ok(!DungeonVisualAdditions.syntheticActorName("aditbro"));
        ok(DungeonVisualAdditions.remoteBossbarType(net.minecraft.entity.boss.EntityWither.class));
        ok(!DungeonVisualAdditions.remoteBossbarType(net.minecraft.entity.player.EntityPlayer.class));
        ok(DungeonVisualAdditions.pairingDistanceSq(0,2.19,.02)<1);
        ok(Double.isInfinite(DungeonVisualAdditions.pairingDistanceSq(5.34,1.1,13.12)));
        ok(DungeonVisualAdditions.bossFloor("Scarf")==2);
        ok(DungeonVisualAdditions.bossFloor("The Professor")==3);
        ok(DungeonVisualAdditions.bossFloor("BonzoFan")==0);
        ok(DungeonVisualAdditions.scarfMinion("Undead Mage 3400❤"));
        // Actual F2 dump: class icons, colour codes and abbreviated HP above
        // private-use-glyph player NPCs; these were previously all rejected.
        ok(DungeonVisualAdditions.scarfMinion("§2༕§r§5♃§r §c§a§lUndead Priest §a566.9k§c❤"));
        ok(DungeonVisualAdditions.scarfMinion("§2༕§r§5♃§r §c§c§lUndead Mage §a400k§c❤"));
        ok(DungeonVisualAdditions.scarfMinion("§2༕§r§5♃§r §c§a§lUndead Archer §a400k§c❤"));
        ok(!DungeonVisualAdditions.scarfMinion("[VIP] Undead Mage said hi"));
        ok(!DungeonVisualAdditions.scarfMinion("mage-player"));
        ok(DungeonVisualAdditions.spiritBow("§bSpirit Bow"));
        ok(!DungeonVisualAdditions.spiritBow("Spirit Bow of somebody"));
        ok(DungeonVisualAdditions.bossMessage("[BOSS] The Watcher: That will be enough for now."));
        ok(!DungeonVisualAdditions.bossMessage("[MVP] Player: [BOSS] Bonzo: hello"));
        ok(Double.isInfinite(DungeonVisualAdditions.pairingDistanceSq(3,0,0)));
        ok(Double.isInfinite(DungeonVisualAdditions.pairingDistanceSq(0,4,0)));
        ok(DungeonVisualAdditions.pairingDistanceSq(.2,2,.2)<.65);
        net.minecraft.util.AxisAlignedBB source=new net.minecraft.util.AxisAlignedBB(100,64,200,101,66,201);
        net.minecraft.util.AxisAlignedBB result=DungeonVisualAdditions.relativeBox(source,
                100.5,64,200.5,100.75,64,200.5,98,63,199);
        ok(Math.abs(result.minX-2.25)<.001 && Math.abs(result.minY-1)<.001);
        result=DungeonVisualAdditions.relativeBox(source,
                100.5,64,200.5,100.75,64,200.5,99,63,199);
        ok(Math.abs(result.minX-1.25)<.001); // follows world entity, not aim direction
        System.out.println("  Boss/minion/bow/chat classification and world-space box pairing OK");
    }
}
