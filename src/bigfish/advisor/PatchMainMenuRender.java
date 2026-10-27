package bigfish.advisor;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;

@SpirePatch(clz = MainMenuScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class PatchMainMenuRender {
    @SpirePostfixPatch
    public static void Postfix(MainMenuScreen screen, SpriteBatch sb) {
        BigFishAdvisor.renderWelcome(sb);
    }
}
