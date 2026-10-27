package bigfish.advisor;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(clz = AbstractDungeon.class, method = "render",
        paramtypez = {SpriteBatch.class})
public class PatchRender {
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon dungeon, SpriteBatch sb) {
        BigFishAdvisor.render(sb);
    }
}
