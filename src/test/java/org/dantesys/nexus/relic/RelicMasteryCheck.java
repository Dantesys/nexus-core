package org.dantesys.nexus.relic;

import java.util.List;
import net.minecraft.network.chat.Component;

/** Executado por checkRelicMastery, sem iniciar o jogo. */
public final class RelicMasteryCheck {
    public static void main(String[] args) {
        // Maestria: niveis 1 a 20, habilidades em 1/5/10/15/20.
        check(RelicSkill.requiredLevel(0) == 1 && RelicSkill.requiredLevel(1) == 5 && RelicSkill.requiredLevel(4) == 20,
                "Skills unlock every five levels");
        RelicMastery edited = new RelicMastery();
        edited.setLevel("nexus:rupty", 20);
        check(edited.level("nexus:rupty") == 20, "Command sets level 20");
        edited.setLevel("nexus:rupty", 5);
        check(edited.experience("nexus:rupty") == 400, "Command sets exact start of level");
        check(edited.level("nexus:other") == 1, "Command changes only requested relic");
        RelicMastery player = new RelicMastery();
        check(!player.addExperience("r", 0) && player.level("r") == 1, "Initial level");
        player.addExperience("r", Integer.MAX_VALUE);
        check(player.level("r") == RelicMastery.MAX_LEVEL, "Level capped at 20");

        // Selecao so de ativas liberadas.
        RelicAbility none = (p, s, c) -> false;
        List<RelicSkill> skills = List.of(
                RelicSkill.passive(Component.literal("p"), 0, none),
                RelicSkill.active(Component.literal("a5"), 0, none),
                RelicSkill.active(Component.literal("a10"), 0, none),
                RelicSkill.passive(Component.literal("p15"), 0, none),
                RelicSkill.active(Component.literal("a20"), 0, none));
        check(RelicSkill.nextActive(0, skills, 1, true) == -1, "No active at level 1");
        check(RelicSkill.nextActive(1, skills, 5, true) == 1, "Only unlocked active is kept");
        check(RelicSkill.nextActive(1, skills, 10, true) == 2, "Next active");
        check(RelicSkill.nextActive(2, skills, 20, true) == 4, "Passives are skipped");
        check(RelicSkill.nextActive(4, skills, 20, true) == 1, "Wraps to first active");
        check(RelicSkill.visualStage(4) == 0 && RelicSkill.visualStage(5) == 1
                && RelicSkill.visualStage(15) == 2 && RelicSkill.visualStage(20) == 3, "Visual stage");

        // Rupty
        check(RuptyRules.totalFragments(14) == 10 && RuptyRules.totalFragments(15) == 20, "Fragmentar unlocks 20 fragments");
        check(RuptyRules.marksPerTarget(1) == 5 && RuptyRules.marksPerTarget(15) == 10, "Per-target cap doubles");
        check(Math.abs(RuptyRules.defenseIgnored(5) - 0.5) < 1e-9 && RuptyRules.defenseIgnored(10) == 1.0
                && RuptyRules.defenseIgnored(30) == 1.0, "10% defense per mark, up to 100%");
        check(Math.abs(RuptyRules.bladeScale(10) - 0.5) < 1e-9 && RuptyRules.bladeScale(20) == 0, "Blade shrinks 5% per fragment");
        check(RuptyRules.hiddenPieces(0) == 0 && RuptyRules.hiddenPieces(20) == 5, "Blade pieces");
        check(RuptyRules.stabMarks(1) == 5 && RuptyRules.stabMarks(5) < 5 && RuptyRules.stabMarks(5) >= 2, "Stab marks by distance");
        check(RuptyRules.strengthAmplifier(1) == 0 && RuptyRules.strengthAmplifier(20) == 4, "Union strength scales");
        check(RuptyRules.canUseFinal(680, 20, 20, 9, 20), "Final accepted");
        check(!RuptyRules.canUseFinal(679, 20, 20, 9, 20), "Final needs affinity A");
        check(!RuptyRules.canUseFinal(680, 19, 20, 9, 20), "Final needs every fragment on the blade");
        check(!RuptyRules.canUseFinal(680, 10, 10, 9, 20), "Final needs 20 fragments");
        check(!RuptyRules.canUseFinal(680, 20, 20, 10, 20), "Final needs less than half health");

        // Krefty
        var e = KreftyRules.parse("minecraft:diamond_block=96@15");
        check(e != null && !e.tag() && e.cost() == 96 && e.minLevel() == 15, "Parse block rule");
        e = KreftyRules.parse("#c:ores=16");
        check(e != null && e.tag() && e.key().equals("c:ores") && e.minLevel() == 1, "Parse tag rule without level");
        check(KreftyRules.parse("lixo") == null && KreftyRules.parse("a=b@c") == null && KreftyRules.parse("x=-1") == null, "Reject invalid rules");
        check(KreftyRules.areaCost(30, 1000) == 130, "Area cost by volume");
        check(KreftyRules.hasFinalRank(680) && !KreftyRules.hasFinalRank(679), "Instant build needs capacity A");
        System.out.println("RelicMasteryCheck: OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
