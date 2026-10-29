package bigfish.advisor;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.neow.NeowRoom;

/** Draw the Neow panel after the room's own top-level dialogue. */
@SpirePatch(clz = NeowRoom.class, method = "renderAboveTopPanel",
        paramtypez = {SpriteBatch.class})
public class PatchNeowRender {
    @SpirePostfixPatch
    public static void Postfix(NeowRoom room, SpriteBatch sb) {
        BigFishAdvisor.renderNeow(sb);
    }
}
