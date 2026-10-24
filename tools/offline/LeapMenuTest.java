import com.sweatsenhancements.thirdparty.skyvelor.feature.SpiritLeap;
import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapUse;
import com.sweatsenhancements.thirdparty.skyvelor.feature.LeapText;
public final class LeapMenuTest {
    public static void main(String[] args){
        if(!SpiritLeap.decide(true,false,false,0))throw new AssertionError("Title detection");
        if(SpiritLeap.decide(false,true,true,0)||SpiritLeap.decide(false,true,false,1))
            throw new AssertionError("Unverified alternate menu detection");
        if(!SpiritLeap.decide(false,true,true,1))throw new AssertionError("Recent use and teammate skull");
        if(!LeapUse.isLeapItemName("§aSpirit Leap")||!LeapUse.isLeapItemName("Infinileap"))
            throw new AssertionError("Leap item names");
        if(!SpiritLeap.teammateSlots(null).isEmpty())throw new AssertionError("Null menu");
        System.out.println("Original Leap menu title/recent use/dungeon/skull gating OK (offline)");
    }
}
