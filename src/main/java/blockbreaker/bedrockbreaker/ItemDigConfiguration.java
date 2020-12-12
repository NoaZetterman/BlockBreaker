package blockbreaker.bedrockbreaker;

public class ItemDigConfiguration {
    private final boolean isHarvestable;
    private final boolean isBestTool;

    public ItemDigConfiguration(boolean isHarvestable, boolean isBestTool) {
        this.isHarvestable = isHarvestable;
        this.isBestTool = isBestTool;
    }

    public boolean isHarvestable() {
        return isHarvestable;
    }

    public boolean isBestTool() {
        return isBestTool;
    }
}
