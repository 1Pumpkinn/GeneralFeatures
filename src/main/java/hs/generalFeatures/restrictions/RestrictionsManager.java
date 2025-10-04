package hs.generalFeatures.restrictions;

/**
 * Manages all game restrictions and provides access to their status
 */
public class RestrictionsManager {

    private final DisablePotTwo disablePotTwo;
    private final DisableFireworks disableFireworks;
    private final DisablePearls disablePearls;

    public RestrictionsManager(DisablePotTwo disablePotTwo, DisableFireworks disableFireworks, DisablePearls disablePearls) {
        this.disablePotTwo = disablePotTwo;
        this.disableFireworks = disableFireworks;
        this.disablePearls = disablePearls;
    }

    public DisablePotTwo getDisablePotTwo() {
        return disablePotTwo;
    }

    public DisableFireworks getDisableFireworks() {
        return disableFireworks;
    }

    public DisablePearls getDisablePearls() {
        return disablePearls;
    }

    /**
     * Check if potions restriction is enabled
     */
    public boolean isPotionsRestricted() {
        return disablePotTwo.isEnabled();
    }

    /**
     * Check if fireworks restriction is enabled
     */
    public boolean isFireworksRestricted() {
        return disableFireworks.isEnabled();
    }

    /**
     * Check if pearls restriction is enabled
     */
    public boolean isPearlsRestricted() {
        return disablePearls.isEnabled();
    }
}