import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapText;
import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapClass;
import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapIdentity;
import java.util.Arrays;
public final class LeapSourcePortTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        check("Archer".equals(LeapText.classFromTab("catgirlgabagool (Archer XXV)","catgirlgabagool")),"tab Archer");
        check(LeapText.classFromScoreboardLine("[B] MerePasSorF","MerePasSorF",Arrays.asList("MerePasSorF"))==LeapClass.BERSERK,"scoreboard Berserk");
        check(LeapText.classFromScoreboardLine("[A] catgirlgabagool","MerePasSorF",Arrays.asList("catgirlgabagool","MerePasSorF"))==null,"no cross-player class");
        check("iRighteous".equals(LeapIdentity.usernameFromDisplay("[VIP] iRighteous",Arrays.asList("iRighteous"))),"rank prefix identity");
        System.out.println("Upstream Leap class resolution and exact player identity passed (offline)");
    }
}
