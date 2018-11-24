package de.raidcraft.nms.api;

import de.raidcraft.api.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Optional;

/**
 * The {@link EntityRegistry} can be used to register custom NMS entities with the server.
 * The interface will be implemented according to the current supported Minecraft version.
 */
public interface EntityRegistry extends Component {

    /**
     * Registers a new custom NMS entity with the server.
     * The provided name must be unique and should be prefixed with your plugin shortcut.
     * The entity can than be spawned by referencing the custom name.
     * <example>
     *     registerCustomEntity("rc_my_custom_skeleton", {@link EntityType#SKELETON}, MyCustomSkeleton.class)
     * </example>
     *
     * @param name custom unique name of the new entity
     * @param entityType
     * @param clazz
     */
    void registerCustomEntity(String name, EntityType entityType, Class<?> clazz);

    /**
     * Tries to get a registered entity from the store.
     * The entity still needs to be spawned.
     *
     * @param name of the entity as registered in {@link #registerCustomEntity(String, EntityType, Class)}
     * @param world to create the entity in
     * @return created entity or empty optional
     */
    Optional<Entity> getEntity(String name, World world);

    void rebuildWorldGenMobs();

    void rebuildBiomes();
}
