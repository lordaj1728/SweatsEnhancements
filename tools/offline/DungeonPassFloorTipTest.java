import com.sweatsenhancements.feature.DungeonPassFloorTip;

public class DungeonPassFloorTipTest {
    private static void eq(String actual, String expected) {
        if (expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError("expected " + expected + " got " + actual);
    }
    public static void main(String[] args) {
        eq(DungeonPassFloorTip.floorLabel("CATACOMBS_PASS_3"), "E");
        eq(DungeonPassFloorTip.floorLabel("CATACOMBS_PASS_4"), "F1");
        eq(DungeonPassFloorTip.floorLabel("CATACOMBS_PASS_10"), "F7");
        eq(DungeonPassFloorTip.floorLabel("MASTER_CATACOMBS_PASS_4"), "M1");
        eq(DungeonPassFloorTip.floorLabel("MASTER_CATACOMBS_PASS_10"), "M7");
        eq(DungeonPassFloorTip.floorLabel("CATACOMBS_PASS_999"), null);
        eq(DungeonPassFloorTip.floorLabel("CATACOMBS_PASS_4_EXTRA"), null);
        eq(DungeonPassFloorTip.floorLabel("NOT_A_PASS_4"), null);
        if (!DungeonPassFloorTip.isPassSelection(new String[] {"CATACOMBS_PASS_3", "CATACOMBS_PASS_4"})
                || DungeonPassFloorTip.isPassSelection(new String[] {"CATACOMBS_PASS_3", "OTHER"})
                || DungeonPassFloorTip.isPassSelection(new String[] {"OTHER", "OTHER"}))
            throw new AssertionError("pass menu matching incorrect");
        System.out.println("Pass floor IDs and menu/item gating OK; item IDs seen in user dump");
    }
}
