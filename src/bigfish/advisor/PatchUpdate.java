package bigfish.advisor;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(clz = AbstractDungeon.class, method = "update", paramtypez = {})
public class PatchUpdate {
    @SpirePrefixPatch
    public static void Prefix() {
        BigFishAdvisor.preUpdate();
    }

    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon dungeon) {
        BigFishAdvisor.update();
    }
}
