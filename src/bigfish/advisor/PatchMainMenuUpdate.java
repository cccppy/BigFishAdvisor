package bigfish.advisor;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;

@SpirePatch(clz = MainMenuScreen.class, method = "update", paramtypez = {})
public class PatchMainMenuUpdate {
    @SpirePrefixPatch
    public static void Prefix() {
        BigFishAdvisor.preUpdate();
    }
}
