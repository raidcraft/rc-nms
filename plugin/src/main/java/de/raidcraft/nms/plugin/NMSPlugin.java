package de.raidcraft.nms.plugin;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.nms.api.EntityRegistry;
import de.raidcraft.util.ReflectionUtil;
import org.bukkit.Bukkit;

public class NMSPlugin extends BasePlugin {

    private static final String NMSPackage = "de.raidcraft.nms.classes";

    private EntityRegistry entityRegistry;

    @Override
    public void enable() {
        try {
            ReflectionUtil.<EntityRegistry>getNmsClassInstance(NMSPackage, "NmsEntityRegistry").ifPresent(entityRegistry -> {
                this.entityRegistry = entityRegistry;
                RaidCraft.registerComponent(EntityRegistry.class, entityRegistry);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {

    }
}
