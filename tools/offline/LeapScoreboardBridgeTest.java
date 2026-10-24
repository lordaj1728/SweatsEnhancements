import com.sweatsenhancements.feature.CustomScoreboard;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
public final class LeapScoreboardBridgeTest {
    public static void main(String[] args) throws Exception {
        Scoreboard board=new Scoreboard();
        ScoreObjective server=board.addScoreObjective("catacombs",IScoreObjectiveCriteria.DUMMY);
        board.setObjectiveInDisplaySlot(1,server);
        CustomScoreboard panel=new CustomScoreboard();
        java.lang.reflect.Field saved=CustomScoreboard.class.getDeclaredField("objective");
        saved.setAccessible(true);saved.set(panel,server);
        board.setObjectiveInDisplaySlot(1,null);
        if(board.getObjectiveInDisplaySlot(1)!=null||panel.retainedServerSidebar(board)!=server)
            throw new AssertionError("Hidden server objective not available to Leap without showing sidebar");
        Scoreboard another=new Scoreboard();another.addScoreObjective("catacombs",IScoreObjectiveCriteria.DUMMY);
        if(panel.retainedServerSidebar(another)!=null)
            throw new AssertionError("Stale objective leaked into another scoreboard");
        board.removeObjective(server);
        if(panel.retainedServerSidebar(board)!=null)
            throw new AssertionError("Deleted server objective was reused");
        System.out.println("Leap sees retained server objective; hidden slot remains hidden and stale worlds reject it");
    }
}
