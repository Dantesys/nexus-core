package org.dantesys.nexus.relic;

/** Executado por checkRelicMastery, sem iniciar o jogo. */
public final class RelicMasteryCheck {
    public static void main(String[] args) {
        RelicMastery edited = new RelicMastery();
        edited.setLevel("nexus:rupty", 100);
        check(edited.level("nexus:rupty") == 100 && !edited.isDiscovered("nexus:rupty"), "Command does not reveal secret");
        edited.togglePassive("nexus:rupty", 3);
        edited.discover("nexus:rupty");
        edited.setLevel("nexus:rupty", 32);
        check(edited.experience("nexus:rupty") == 3100, "Command sets exact start of level");
        check(!edited.isPassiveEnabled("nexus:rupty", 3) && edited.isDiscovered("nexus:rupty"), "Downgrade locks passive but preserves discovery");
        check(edited.level("nexus:other") == 1, "Command changes only requested relic");
        for (var element : org.dantesys.nexus.util.Elementos.values()) {
            for (int slot = 0; slot < 4; slot++) {
                boolean neutral = element == org.dantesys.nexus.util.Elementos.NAO_ELEMENTAL;
                check(RuptyElements.description(element, slot).isEmpty() == neutral,
                        "Every elemental skill has a description; neutral has no extra bonus");
            }
        }
        check(RuptyRules.weakness(0) == 0 && RuptyRules.weakness(100) == 0.5, "Shadow weakness is bounded");
        check(RuptyRules.vulnerability(0) == 0 && RuptyRules.vulnerability(100) == 0.3, "Light vulnerability is bounded");
        check(RuptyRules.penetration(4, 0, 0) == 0, "Metal requires scar threshold");
        check(Math.abs(RuptyRules.penetration(5, 0, 0) - 0.1) < 0.0001, "Metal gains penetration every five scars");
        check(RuptyRules.penetration(0, 1, 0) == 0.25, "Defense Cut has immediate penetration");
        check(RuptyRules.penetration(0, 2, 4) > RuptyRules.penetration(0, 2, 1), "Zone gains penetration per pulse");
        check(RuptyRules.penetration(100, 3, 100) == 0.8, "Penetration cap");
        check(RuptyRules.healing(-1) == 0 && RuptyRules.healing(Float.MAX_VALUE) == 2, "Healing uses bounded real damage");
        check(RuptyRules.crossedElectricThreshold(4, 5), "Discharge at five scars");
        check(!RuptyRules.crossedElectricThreshold(5, 5), "No repeat discharge without new scars");
        check(!RuptyRules.crossedElectricThreshold(5, 4), "Decay does not discharge");
        check(RuptyRules.canCharge(10, 20, 50, 100), "Ruptura accepts exactly half health and mana");
        check(!RuptyRules.canCharge(10.1f, 20, 50, 100), "Ruptura rejects health above half");
        check(!RuptyRules.canCharge(10, 20, 49, 100), "Ruptura rejects low mana");
        check(!RuptyRules.canCharge(0, 20, 100, 100), "Dead player cannot charge");
        check(!RuptyRules.canExecute(0, false, 20, 20), "Unmarked target cannot be executed");
        check(RuptyRules.canExecute(1, false, 20, 20), "Marked ordinary target can be executed");
        check(!RuptyRules.canExecute(1, true, 20, 100), "Boss at exactly twenty percent is protected");
        check(RuptyRules.canExecute(1, true, 19.9f, 100), "Boss below twenty percent can be executed");
        check(RuptyRules.remainingScars(10, 99) == 10, "Scars persist during five-second grace period");
        check(RuptyRules.remainingScars(10, 100) == 9, "First scar expires at five seconds");
        check(RuptyRules.remainingScars(10, 120) == 8, "Subsequent scars expire one per second");
        check(RuptyRules.remainingScars(10, 1000) == 0, "Scars never become negative");
        check(RelicSkill.nextSlot(0, 4, 1, true) == 0, "Skip locked skills");
        check(RelicSkill.nextSlot(0, 4, 32, true) == 1, "Next unlocked skill");
        check(RelicSkill.nextSlot(1, 4, 32, true) == 0, "Forward wrap");
        check(RelicSkill.nextSlot(0, 4, 64, false) == 2, "Backward wrap skips secret");
        check(RelicSkill.nextSlot(2, 4, 100, true) == 3, "Secret selectable at maximum");
        check(RelicSkill.nextSlot(3, 4, 100, true) == 0, "Wrap after secret");
        check(RelicSkill.nextSlot(3, 4, 1, false) == 0, "Selection adapts to another player");
        check(RelicSkill.nextSlot(0, 0, 100, true) == 0, "Empty skill list");
        RelicMastery player = new RelicMastery();
        String relic = "nexus:test_relic";
        check(player.level(relic) == 1, "Initial level");
        check(!player.isDiscovered(relic), "Secret starts hidden");
        check(!player.isPassiveEnabled(relic, 0), "Passives start disabled");
        check(player.togglePassive(relic, 0), "First passive can be enabled");
        check(player.isPassiveEnabled(relic, 0), "Enabled preference");
        check(!player.isPassiveEnabled("nexus:other", 0), "Preference is per relic");
        check(!new RelicMastery().isPassiveEnabled(relic, 0), "Preference is per player");
        check(player.togglePassive(relic, 0) && !player.isPassiveEnabled(relic, 0), "Passive can be disabled");
        check(!player.togglePassive(relic, 1), "Locked passive cannot be enabled");
        check(!player.togglePassive(relic, -1) && !player.togglePassive(relic, 4), "Reject invalid slots");
        check(player.experience(relic) == 0, "Toggling does not grant experience");
        for (int slot = 1; slot < 4; slot++) {
            int required = RelicSkill.requiredLevel(slot);
            int threshold = (required - 1) * RelicMastery.XP_PER_LEVEL;
            player.addExperience(relic, threshold - 1 - player.experience(relic));
            check(player.level(relic) == required - 1, "Locked before threshold " + required);
            player.addExperience(relic, 1);
            check(player.level(relic) == required, "Unlocked at threshold " + required);
        }
        check(!player.isDiscovered(relic), "Maximum mastery alone must not reveal secret");
        check(player.togglePassive(relic, 3), "Secret passive can be enabled at maximum mastery");
        check(!player.isDiscovered(relic), "Enabling secret passive does not reveal it");
        check(player.togglePassive(relic, 1), "Second passive can be enabled after unlock");
        check(player.isPassiveEnabled(relic, 3), "Toggling one slot preserves other slots");
        check(!player.addExperience(relic, Integer.MAX_VALUE), "Experience capped at max");
        check(player.level(relic) == 100, "Level capped at 100");
        check(player.discover(relic), "First discovery");
        check(!player.discover(relic), "Discovery is idempotent");
        check(player.level("nexus:other") == 1, "Different relic has separate mastery");
        check(new RelicMastery().level(relic) == 1, "Different player has separate mastery");
        RelicMastery fresh = new RelicMastery();
        check(!fresh.addExperience(relic, -1), "Negative experience ignored");
        fresh.addExperience(relic, Integer.MAX_VALUE);
        check(fresh.level(relic) == 100, "Large awards cannot overflow");
        System.out.println("Relic mastery checks passed.");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
