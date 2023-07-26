package net.runelite.client.plugins.nightmare.util.loadouts;

public class EquipmentItem {
    private final int id;
    private final EquipCondition condition;
    @FunctionalInterface
    public interface EquipCondition {
        boolean checkCondition();
    }

    /**
     * Returns a new EquipmentItem with a required condition that has to return TRUE in order to be considered in fulfillment.
     * @param id
     * @param conditionToEquip
     */
    public EquipmentItem(int id, EquipCondition conditionToEquip) {
        this.id = id;
        this.condition = conditionToEquip;
    }
    public EquipmentItem(int id) {
        this.id = id;
        this.condition = () -> true;
    }
    public boolean canEquip() {
        return condition.checkCondition();
    }
}