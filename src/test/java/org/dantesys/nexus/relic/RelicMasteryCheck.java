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
        check(Math.abs(RuptyRules.defenseReduction(10, 0, false) - 0.5) < 1e-9, "10 marks reduce 50% of defense");
        check(Math.abs(RuptyRules.defenseReduction(0, 10, true) - 1.5) < 1e-9, "Open marks (10% each) + Mortal (50%)");
        check(RuptyRules.defenseIgnored(1.5) == 1.0 && Math.abs(RuptyRules.damageMultiplier(1.5) - 1.5) < 1e-9,
                "Reduction above 100% becomes extra damage");
        check(RuptyRules.damageMultiplier(0.5) == 1.0, "No extra damage below 100%");
        check(RuptyRules.rotationMarks(1.0) == 5 && RuptyRules.rotationMarks(2.5) == 3 && RuptyRules.rotationMarks(3.5) == 0,
                "Rotation marks by distance");
        check(RuptyRules.OPEN_MARK_DURATION == 2 * RuptyRules.MARK_DURATION, "Open Mark doubles duration");
        check(RuptyRules.mortalTickDamage(8f) == 4f, "Mortal Mark deals half the sword damage");

        // Krefty
        var e = KreftyRules.parse("minecraft:diamond_block=96@15");
        check(e != null && !e.tag() && e.cost() == 96 && e.minLevel() == 15, "Parse block rule");
        e = KreftyRules.parse("#c:ores=16");
        check(e != null && e.tag() && e.key().equals("c:ores") && e.minLevel() == 1, "Parse tag rule without level");
        check(KreftyRules.parse("lixo") == null && KreftyRules.parse("a=b@c") == null && KreftyRules.parse("x=-1") == null, "Reject invalid rules");
        check(KreftyRules.areaCost(30, 1000) == 130, "Area cost by volume");
        check(KreftyRules.hasFinalRank(680) && !KreftyRules.hasFinalRank(679), "Instant build needs capacity A");

        // Escordy
        check(EscordyEffects.normalLimit(1) == 10 && EscordyEffects.normalLimit(20) == 20, "Charge limit 10 to 20");
        check(EscordyEffects.falloff(2) == 1.0 && EscordyEffects.falloff(24) < 0.2 && EscordyEffects.falloff(10) < 1.0, "Pellet falloff");
        check(EscordyEffects.spread(20) > EscordyEffects.spread(2) && EscordyEffects.spread(50) <= 25, "Cone widens with pellets");
        System.out.println("RelicMasteryCheck: OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
