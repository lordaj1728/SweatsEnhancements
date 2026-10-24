import com.sweatsenhancements.feature.DungeonBlessingFilter;
public final class DungeonBlessingFilterTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError("blessing message classifier"); }
    public static void main(String[] args) {
        check(DungeonBlessingFilter.isAnnouncement("§aYou found a Blessing of Power V!"));
        check(DungeonBlessingFilter.isAnnouncement("Blessing of Wisdom was found!"));
        check(!DungeonBlessingFilter.isAnnouncement("[MVP] Player: You found a Blessing of Power V!"));
        check(!DungeonBlessingFilter.isAnnouncement("Your Blessing of Power is now level V"));
        check(!DungeonBlessingFilter.isAnnouncement(null));
        System.out.println("  Blessing announcement filter leaves chat and other blessing updates alone");
    }
}
