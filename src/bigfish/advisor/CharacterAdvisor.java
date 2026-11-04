package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import java.util.ArrayList;
import java.util.List;

/**
 * Character-specific combat reads.  The generic scorer in CombatAdvisor is
 * deliberately conservative; Watcher stance dancing and Defect orb management
 * are too important to reduce to a flat tag bonus, so those characters get a
 * dedicated read here and a detailed explanation in the panel.
 */
public final class CharacterAdvisor {
    public static final class Read {
        /** -1 means "do not replace the generic damage estimate". */
        public int damage = -1;
        public double bonus;
        public String note = "";
    }

    private CharacterAdvisor() {
    }

    public static Read combatCard(AbstractPlayer p, AbstractCard c, List<AbstractMonster> alive,
                                  AbstractMonster target, boolean aoe, int energy, int incoming) {
        if (p == null || c == null) {
            return new Read();
        }
        String cls = p.chosenClass == null ? "" : p.chosenClass.name();
        if (cls.contains("WATCHER")) {
            return StanceAdvisor.readCard(p, c, alive, target, aoe, energy, incoming);
        }
        if (cls.contains("DEFECT")) {
            return OrbAdvisor.readCard(p, c, alive, target, aoe, energy, incoming);
        }
        if (cls.contains("SILENT")) {
            return SilentAdvisor.readCard(p, c, alive, target, aoe, energy, incoming);
        }
        if (cls.contains("IRONCLAD")) {
            return IroncladAdvisor.readCard(p, c, alive, target, aoe, energy, incoming);
        }
        return new Read();
    }

    public static List<String> combatLines(AbstractPlayer p, List<AbstractMonster> alive,
                                           int energy, int incoming) {
        if (p == null) {
            return new ArrayList<String>();
        }
        String cls = p.chosenClass == null ? "" : p.chosenClass.name();
        if (cls.contains("WATCHER")) {
            return StanceAdvisor.lines(p, alive, energy, incoming);
        }
        if (cls.contains("DEFECT")) {
            return OrbAdvisor.lines(p, alive, energy, incoming);
        }
        if (cls.contains("SILENT")) {
            return SilentAdvisor.lines(p, alive, energy, incoming);
        }
        if (cls.contains("IRONCLAD")) {
            return IroncladAdvisor.lines(p, alive, energy, incoming);
        }
        return new ArrayList<String>();
    }

    public static String combatPlan(AbstractPlayer p, int energy, int incoming) {
        if (p == null) {
            return "";
        }
        String cls = p.chosenClass == null ? "" : p.chosenClass.name();
        if (cls.contains("WATCHER")) {
            return StanceAdvisor.plan(p, energy, incoming);
        }
        if (cls.contains("DEFECT")) {
            return OrbAdvisor.plan(p, energy, incoming);
        }
        if (cls.contains("SILENT")) {
            return SilentAdvisor.plan(p, energy, incoming);
        }
        if (cls.contains("IRONCLAD")) {
            return IroncladAdvisor.plan(p, energy, incoming);
        }
        return "";
    }

    public static double pickBonus(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null) {
            return 0.0;
        }
        String cls = p.chosenClass == null ? "" : p.chosenClass.name();
        if (cls.contains("WATCHER")) {
            return StanceAdvisor.pickBonus(p, c, d);
        }
        if (cls.contains("DEFECT")) {
            return OrbAdvisor.pickBonus(p, c, d);
        }
        if (cls.contains("SILENT")) {
            return SilentAdvisor.pickBonus(p, c, d);
        }
        if (cls.contains("IRONCLAD")) {
            return IroncladAdvisor.pickBonus(p, c, d);
        }
        return 0.0;
    }

    public static String pickNote(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null) {
            return "";
        }
        String cls = p.chosenClass == null ? "" : p.chosenClass.name();
        if (cls.contains("WATCHER")) {
            return StanceAdvisor.pickNote(p, c, d);
        }
        if (cls.contains("DEFECT")) {
            return OrbAdvisor.pickNote(p, c, d);
        }
        if (cls.contains("SILENT")) {
            return SilentAdvisor.pickNote(p, c, d);
        }
        if (cls.contains("IRONCLAD")) {
            return IroncladAdvisor.pickNote(p, c, d);
        }
        return "";
    }
}
