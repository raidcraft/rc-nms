package de.raidcraft.nms.classes.v1_13_R2;

import com.google.common.collect.Lists;
import com.mojang.datafixers.types.Type;
import de.raidcraft.RaidCraft;
import de.raidcraft.nms.api.EntityRegistry;
import de.raidcraft.util.CaseInsensitiveMap;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.*;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class NmsEntityRegistry implements EntityRegistry {
    
    private static Field registry_field_b;
    private static Field registry_field_c;
    private static Field registry_field_d;
    private static Field materials_field_b;
    private static Field materials_field_c;
    private static Field field_modifiers;
    private static Field item_fishType;
    private static Field item_entityType;
    private static Method registry_method_d;
    private static Method registry_method_e;
    private static Method biomebase_registerBiome;
    private static Method biomebase_addSpawn;
    private static Method entitypositiontypes_register;

    static {
        try {
            registry_field_b = RegistryID.class.getDeclaredField("b");
            registry_field_b.setAccessible(true);
            registry_field_c = RegistryID.class.getDeclaredField("c");
            registry_field_c.setAccessible(true);
            registry_field_d = RegistryID.class.getDeclaredField("d");
            registry_field_d.setAccessible(true);
            materials_field_b = RegistryMaterials.class.getDeclaredField("b"); // RegistryID<V>
            materials_field_b.setAccessible(true);
            materials_field_c = RegistryMaterials.class.getDeclaredField("c"); // BiMap<MinecraftKey,V>
            materials_field_c.setAccessible(true);
            field_modifiers = Field.class.getDeclaredField("modifiers");
            field_modifiers.setAccessible(true);
            item_fishType = ItemFishBucket.class.getDeclaredField("a");
            item_fishType.setAccessible(true);
            item_entityType = ItemMonsterEgg.class.getDeclaredField("d");
            item_entityType.setAccessible(true);
            registry_method_d = RegistryID.class.getDeclaredMethod("d", Object.class);
            registry_method_d.setAccessible(true);
            registry_method_e = RegistryID.class.getDeclaredMethod("e", int.class);
            registry_method_e.setAccessible(true);
            biomebase_registerBiome = BiomeBase.class.getDeclaredMethod("a", int.class, String.class, BiomeBase.class);
            biomebase_registerBiome.setAccessible(true);
            biomebase_addSpawn = BiomeBase.class.getDeclaredMethod("a", EnumCreatureType.class, BiomeBase.BiomeMeta.class);
            biomebase_addSpawn.setAccessible(true);
            entitypositiontypes_register = EntityPositionTypes.class.getDeclaredMethod("a", EntityTypes.class, EntityPositionTypes.Surface.class, HeightMap.Type.class);
            entitypositiontypes_register.setAccessible(true);

        } catch (NoSuchFieldException | NoSuchMethodException ignore) {
        }
    }

    private final Map<String, EntityTypes<?>> registeredCustomEntities = new CaseInsensitiveMap<>();

    @Override
    public Optional<org.bukkit.entity.Entity> getEntity(String name, org.bukkit.World world) {
        EntityTypes<?> entityTypes = registeredCustomEntities.get(name);
        if (entityTypes == null) return Optional.empty();
        Entity entity = entityTypes.a(((CraftWorld) world).getHandle());
        if (entity == null) return Optional.empty();
        return Optional.ofNullable(entity.getBukkitEntity());
    }

    @SuppressWarnings("unchecked")
    public void registerCustomEntity(String name, EntityType entityType, Class<?> clazz) {
        injectNewEntityTypes(name, entityType.getName(), (Class<? extends Entity>) clazz, world -> {
            try {
                return (Entity) clazz.getConstructor(World.class).newInstance(world);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public void injectNewEntityTypes(String name, String extend_from, Class<? extends Entity> clazz, Function<? super World, ? extends Entity> function) {
        RaidCraft.LOGGER.info("Attempting to inject new entity: &3" + name);
        RaidCraft.LOGGER.info("Injecting new datatypes");
        Map<Object, Type<?>> dataTypes = (Map<Object, Type<?>>) DataConverterRegistry.a().getSchema(15190).findChoiceType(DataConverterTypes.n).types();
        dataTypes.put("minecraft:" + name, dataTypes.get("minecraft:" + extend_from));
        RaidCraft.LOGGER.info("Injecting new EntityTypes");
        EntityTypes<Entity> entityTypes = EntityTypes.a(name, EntityTypes.a.a(clazz, function));
        RaidCraft.LOGGER.info("Successfully injected new entity: &a" + name);
        registeredCustomEntities.put(name, entityTypes);
    }

    public boolean injectReplacementEntityTypes(String name, EntityTypes entityTypes, MinecraftKey key, EntityTypes<?> newType, Material spawnEggMaterial, Material fishBucketMaterial) {
        RaidCraft.LOGGER.info("Attempting to inject replacement entity: &3" + name);
        try {
            RegistryID<EntityTypes<?>> registry = (RegistryID<EntityTypes<?>>) materials_field_b.get(IRegistry.ENTITY_TYPE);
            int id = registry.getId(entityTypes);

            RaidCraft.LOGGER.info("Detected original id: " + id);

            Object[] array_b = (Object[]) registry_field_b.get(registry);
            Object[] array_d = (Object[]) registry_field_d.get(registry);

            if (id < 0) {
                RaidCraft.LOGGER.info("&cInvalid id detected. Trying again!");
                for (int i = 0; i < array_d.length; i++) {
                    if (array_d[i] != null) {
                        if (array_d[i] == entityTypes) {
                            RaidCraft.LOGGER.info("Found EntityTypes id by reference");
                            id = i;
                            break;
                        }
                        if (((EntityTypes) array_d[i]).d().equals("minecraft:" + name)) {
                            RaidCraft.LOGGER.info("&cFound EntityTypes id using name but not reference! What?!");
                            id = i;
                            break;
                        }
                    }
                }
                RaidCraft.LOGGER.info("New detected id: " + id);
            }

            RaidCraft.LOGGER.info("EntityType at id " + id + ": " + ((EntityTypes) array_d[id]).d());

            int oldIndex = -1;
            for (int i = 0; i < array_b.length; i++) {
                if (array_b[i] != null) {
                    if (array_b[i] == entityTypes) {
                        //array_b[i] = null; // do not remove old reference (might be causing issues?)
                        oldIndex = i;
                        break;
                    }
                    if (((EntityTypes) array_b[i]).d().equals("minecraft:" + name)) {
                        RaidCraft.LOGGER.info("&cFound EntityTypes oldIndex using name but not reference! What?!");
                        //array_b[i] = null; // do not remove old reference (might be causing issues?)
                        oldIndex = i;
                        break;
                    }
                }
            }

            RaidCraft.LOGGER.info("Detected oldIndex: " + oldIndex);

            if (oldIndex < 0) {
                RaidCraft.LOGGER.info("&cInvalid oldIndex detected. Trying again!");
                array_b = (Object[]) registry_field_b.get(registry);
                for (int i = 0; i < array_b.length; i++) {
                    if (array_b[i] != null) {
                        if (array_b[i] == entityTypes) {
                            //array_b[i] = null; // do not remove old reference (might be causing issues?)
                            oldIndex = i;
                            break;
                        }
                        if (((EntityTypes) array_b[i]).d().equals("minecraft:" + name)) {
                            RaidCraft.LOGGER.info("&cFound EntityTypes oldIndex using name but not reference! What?!");
                            //array_b[i] = null; // do not remove old reference (might be causing issues?)
                            oldIndex = i;
                            break;
                        }
                    }
                }
                RaidCraft.LOGGER.info("New detected oldIndex: " + oldIndex);
            }

            RaidCraft.LOGGER.info("EntityType at oldIndex " + oldIndex + ": " + ((EntityTypes) array_b[oldIndex]).d());

            int newIndex = (int) registry_method_e.invoke(registry, registry_method_d.invoke(registry, newType));
            RaidCraft.LOGGER.info("Generated newIndex: " + newIndex);

            EntityTypes e = (EntityTypes) array_b[newIndex];
            RaidCraft.LOGGER.info("EntityType at newIndex " + newIndex + ": " + (e == null ? "null" : "&c" + e.d()));

            RaidCraft.LOGGER.info("Injecting new EntityTypes to b[newIndex]: " + newIndex);
            array_b[newIndex] = newType;
            RaidCraft.LOGGER.info("Injecting new EntityTypes to d[id]: " + id);
            array_d[id] = newType;

            int[] array_c = (int[]) registry_field_c.get(registry);
            if (oldIndex >= 0) {
                RaidCraft.LOGGER.info("Removing c[oldIndex] reference: " + oldIndex + ":0");
                array_c[oldIndex] = 0;
            } else {
                RaidCraft.LOGGER.info("&cSkipping c[oldIndex] reference: " + oldIndex + ":0");
            }
            RaidCraft.LOGGER.info("Injecting c[newIndex] reference: " + newIndex + ":" + id);
            array_c[newIndex] = id;

            RaidCraft.LOGGER.info("Updating RegistrySimple mapC");
            Map<MinecraftKey, EntityTypes<?>> map_c = (Map<MinecraftKey, EntityTypes<?>>) materials_field_c.get(IRegistry.ENTITY_TYPE);
            map_c.put(key, newType);

            RaidCraft.LOGGER.info("Updating EntityTypes static field");
            Field types_field = getField(entityTypes);
            types_field.setAccessible(true);
            field_modifiers.setInt(types_field, types_field.getModifiers() & ~Modifier.FINAL);
            types_field.set(null, newType);

            registry_field_b.set(registry, array_b);
            registry_field_c.set(registry, array_c);
            registry_field_d.set(registry, array_d);
            materials_field_b.set(IRegistry.ENTITY_TYPE, registry);
            materials_field_c.set(IRegistry.ENTITY_TYPE, map_c);

            if (spawnEggMaterial != null) {
                RaidCraft.LOGGER.info("Updating spawn egg reference");
                Item spawnEgg = CraftItemStack.asNMSCopy(new ItemStack(spawnEggMaterial)).getItem();
                item_entityType.set(spawnEgg, newType);
            }

            if (fishBucketMaterial != null) {
                RaidCraft.LOGGER.info("Updating fish bucket");
                Item fishBucket = CraftItemStack.asNMSCopy(new ItemStack(fishBucketMaterial)).getItem();
                field_modifiers.setInt(item_fishType, item_fishType.getModifiers() & ~Modifier.FINAL);
                item_fishType.set(fishBucket, newType);
            }

            registeredCustomEntities.put(name, entityTypes);

            return true;
        } catch (IllegalAccessException | InvocationTargetException | ArrayIndexOutOfBoundsException e) {
            RaidCraft.LOGGER.severe("Could not inject new custom entity to registry! Restart your server to try again! "
                    + "(&e" + name + "&c)");
            e.printStackTrace();
            return false;
        }
    }

    private Field getField(EntityTypes entityTypes) {
        for (Field field : EntityTypes.class.getDeclaredFields()) {
            try {
                if (entityTypes == field.get(null)) {
                    return field;
                }
            } catch (IllegalAccessException ignore) {
            }
        }
        throw new IllegalArgumentException("Could not get Field of " + entityTypes.getClass().getSimpleName());
    }

    public void rebuildWorldGenMobs() {
        setPrivateFinalField(WorldGenDungeons.class, "b", new EntityTypes[]{EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.ZOMBIE, EntityTypes.SPIDER});
        setPrivateFinalField(WorldGenFeatureSwampHut.class, "b", Lists.newArrayList(new BiomeBase.BiomeMeta(EntityTypes.WITCH, 1, 1, 1)));
        setPrivateFinalField(WorldGenMonument.class, "b", Lists.newArrayList(new BiomeBase.BiomeMeta(EntityTypes.GUARDIAN, 1, 2, 4)));
        setPrivateFinalField(WorldGenNether.class, "b", Lists.newArrayList(new BiomeBase.BiomeMeta(EntityTypes.BLAZE, 10, 2, 3), new BiomeBase.BiomeMeta(EntityTypes.ZOMBIE_PIGMAN, 5, 4, 4), new BiomeBase.BiomeMeta(EntityTypes.WITHER_SKELETON, 8, 5, 5), new BiomeBase.BiomeMeta(EntityTypes.SKELETON, 2, 5, 5), new BiomeBase.BiomeMeta(EntityTypes.MAGMA_CUBE, 3, 4, 4)));
    }

    private void setPrivateFinalField(Class clazz, String name, Object value) {
        RaidCraft.LOGGER.info("Rebuilding world gen mob features for: " + clazz.getSimpleName());
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            field_modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void rebuildBiomes() {
        RaidCraft.LOGGER.info("Rebuilding biome mob lists");

        try {
            entitypositiontypes_register.invoke(null, EntityTypes.ILLUSIONER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        rebuildBiome("OCEAN", 0, "ocean", new BiomeOcean());
        try {
            Field biomes_field = Biomes.class.getDeclaredField("b");
            biomes_field.setAccessible(true);
            field_modifiers.setInt(biomes_field, biomes_field.getModifiers() & ~Modifier.FINAL);
            biomes_field.set(null, Biomes.OCEAN);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        rebuildBiome("PLAINS", 1, "plains", createNonPublicBiome(BiomePlains.class));
        rebuildBiome("DESERT", 2, "desert", new BiomeDesert());
        rebuildBiome("MOUNTAINS", 3, "mountains", createNonPublicBiome(BiomeBigHills.class));
        rebuildBiome("FOREST", 4, "forest", new BiomeForest());
        rebuildBiome("TAIGA", 5, "taiga", new BiomeTaiga());
        rebuildBiome("SWAMP", 6, "swamp", createNonPublicBiome(BiomeSwamp.class));
        rebuildBiome("RIVER", 7, "river", new BiomeRiver());
        rebuildBiome("NETHER", 8, "nether", createNonPublicBiome(BiomeHell.class));
        rebuildBiome("THE_END", 9, "the_end", new BiomeTheEnd());
        rebuildBiome("FROZEN_OCEAN", 10, "frozen_ocean", new BiomeFrozenOcean());
        rebuildBiome("FROZEN_RIVER", 11, "frozen_river", new BiomeFrozenRiver());
        rebuildBiome("SNOWY_TUNDRA", 12, "snowy_tundra", new BiomeIcePlains());
        rebuildBiome("SNOWY_MOUNTAINS", 13, "snowy_mountains", new BiomeIceMountains());
        rebuildBiome("MUSHROOM_FIELDS", 14, "mushroom_fields", new BiomeMushrooms());
        rebuildBiome("MUSHROOM_FIELD_SHORE", 15, "mushroom_field_shore", new BiomeMushroomIslandShore());
        rebuildBiome("BEACH", 16, "beach", new BiomeBeach());
        rebuildBiome("DESERT_HILLS", 17, "desert_hills", new BiomeDesertHills());
        rebuildBiome("WOODED_HILLS", 18, "wooded_hills", new BiomeForestHills());
        rebuildBiome("TAIGA_HILLS", 19, "taiga_hills", new BiomeTaigaHills());
        rebuildBiome("MOUNTAIN_EDGE", 20, "mountain_edge", createNonPublicBiome(BiomeExtremeHillsEdge.class));
        rebuildBiome("JUNGLE", 21, "jungle", new BiomeJungle());
        rebuildBiome("JUNGLE_HILLS", 22, "jungle_hills", new BiomeJungleHills());
        rebuildBiome("JUNGLE_EDGE", 23, "jungle_edge", new BiomeJungleEdge());
        rebuildBiome("DEEP_OCEAN", 24, "deep_ocean", new BiomeDeepOcean());
        rebuildBiome("STONE_SHORE", 25, "stone_shore", new BiomeStoneBeach());
        rebuildBiome("SNOWY_BEACH", 26, "snowy_beach", new BiomeColdBeach());
        rebuildBiome("BIRCH_FOREST", 27, "birch_forest", new BiomeBirchForest());
        rebuildBiome("BIRCH_FOREST_HILLS", 28, "birch_forest_hills", new BiomeBirchForestHills());
        rebuildBiome("DARK_FOREST", 29, "dark_forest", new BiomeRoofedForest());
        rebuildBiome("SNOWY_TAIGA", 30, "snowy_taiga", new BiomeColdTaiga());
        rebuildBiome("SNOWY_TAIGA_HILLS", 31, "snowy_taiga_hills", new BiomeColdTaigaHills());
        rebuildBiome("GIANT_TREE_TAIGA", 32, "giant_tree_taiga", new BiomeMegaTaiga());
        rebuildBiome("GIANT_TREE_TAIGA_HILLS", 33, "giant_tree_taiga_hills", new BiomeMegaTaigaHills());
        rebuildBiome("WOODED_MOUNTAINS", 34, "wooded_mountains", createNonPublicBiome(BiomeExtremeHillsWithTrees.class));
        rebuildBiome("SAVANNA", 35, "savanna", createNonPublicBiome(BiomeSavanna.class));
        rebuildBiome("SAVANNA_PLATEAU", 36, "savanna_plateau", createNonPublicBiome(BiomeSavannaPlateau.class));
        rebuildBiome("BADLANDS", 37, "badlands", new BiomeMesa());
        rebuildBiome("WOODED_BADLANDS_PLATEAU", 38, "wooded_badlands_plateau", new BiomeMesaPlataeu());
        rebuildBiome("BADLANDS_PLATEAU", 39, "badlands_plateau", new BiomeMesaPlataeuClear());
        rebuildBiome("SMALL_END_ISLANDS", 40, "small_end_islands", new BiomeTheEndFloatingIslands());
        rebuildBiome("END_MIDLANDS", 41, "end_midlands", new BiomeTheEndMediumIsland());
        rebuildBiome("END_HIGHLANDS", 42, "end_highlands", new BiomeTheEndHighIsland());
        rebuildBiome("END_BARRENS", 43, "end_barrens", new BiomeTheEndBarrenIsland());
        rebuildBiome("WARM_OCEAN", 44, "warm_ocean", new BiomeWarmOcean());
        rebuildBiome("LUKEWARM_OCEAN", 45, "lukewarm_ocean", new BiomeLukewarmOcean());
        rebuildBiome("COLD_OCEAN", 46, "cold_ocean", new BiomeColdOcean());
        rebuildBiome("DEEP_WARM_OCEAN", 47, "deep_warm_ocean", new BiomeWarmDeepOcean());
        rebuildBiome("DEEP_LUKEWARM_OCEAN", 48, "deep_lukewarm_ocean", new BiomeLukewarmDeepOcean());
        rebuildBiome("DEEP_COLD_OCEAN", 49, "deep_cold_ocean", new BiomeColdDeepOcean());
        rebuildBiome("DEEP_FROZEN_OCEAN", 50, "deep_frozen_ocean", new BiomeFrozenDeepOcean());
        rebuildBiome("THE_VOID", 127, "the_void", new BiomeVoid());
        rebuildBiome("SUNFLOWER_PLAINS", 129, "sunflower_plains", createNonPublicBiome(BiomeSunflowerPlains.class));
        rebuildBiome("DESERT_LAKES", 130, "desert_lakes", new BiomeDesertMutated());
        rebuildBiome("GRAVELLY_MOUNTAINS", 131, "gravelly_mountains", createNonPublicBiome(BiomeExtremeHillsMutated.class));
        rebuildBiome("FLOWER_FOREST", 132, "flower_forest", new BiomeFlowerForest());
        rebuildBiome("TAIGA_MOUNTAINS", 133, "taiga_mountains", new BiomeTaigaMutated());
        rebuildBiome("SWAMP_HILLS", 134, "swamp_hills", createNonPublicBiome(BiomeSwamplandMutated.class));
        rebuildBiome("ICE_SPIKES", 140, "ice_spikes", new BiomeIcePlainsSpikes());
        rebuildBiome("MODIFIED_JUNGLE", 149, "modified_jungle", new BiomeJungleMutated());
        rebuildBiome("MODIFIED_JUNGLE_EDGE", 151, "modified_jungle_edge", new BiomeJungleEdgeMutated());
        rebuildBiome("TALL_BIRCH_FOREST", 155, "tall_birch_forest", new BiomeBirchForestMutated());
        rebuildBiome("TALL_BIRCH_HILLS", 156, "tall_birch_hills", new BiomeBirchForestHillsMutated());
        rebuildBiome("DARK_FOREST_HILLS", 157, "dark_forest_hills", new BiomeRoofedForestMutated());
        rebuildBiome("SNOWY_TAIGA_MOUNTAINS", 158, "snowy_taiga_mountains", new BiomeColdTaigaMutated());
        rebuildBiome("GIANT_SPRUCE_TAIGA", 160, "giant_spruce_taiga", new BiomeMegaSpruceTaiga());
        rebuildBiome("GIANT_SPRUCE_TAIGA_HILLS", 161, "giant_spruce_taiga_hills", new BiomeRedwoodTaigaHillsMutated());
        rebuildBiome("MODIFIED_GRAVELLY_MOUNTAINS", 162, "modified_gravelly_mountains", createNonPublicBiome(BiomeExtremeHillsWithTreesMutated.class));
        rebuildBiome("SHATTERED_SAVANNA", 163, "shattered_savanna", new BiomeSavannaMutated());
        rebuildBiome("SHATTERED_SAVANNA_PLATEAU", 164, "shattered_savanna_plateau", new BiomeSavannaPlateauMutated());
        rebuildBiome("ERODED_BADLANDS", 165, "eroded_badlands", new BiomeMesaBryce());
        rebuildBiome("MODIFIED_WOODED_BADLANDS_PLATEAU", 166, "modified_wooded_badlands_plateau", new BiomeMesaPlateauMutated());
        rebuildBiome("MODIFIED_BADLANDS_PLATEAU", 167, "modified_badlands_plateau", new BiomeMesaPlateauClearMutated());
    }

    private BiomeBase createNonPublicBiome(Class<? extends BiomeBase> clazz) {
        try {
            Constructor<? extends BiomeBase> biomebase_constructor = clazz.getDeclaredConstructor();
            biomebase_constructor.setAccessible(true);
            return biomebase_constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void rebuildBiome(String field, int id, String name, BiomeBase biome) {
        RaidCraft.LOGGER.info(" - rebuilding biome: " + name);
        try {
            biomebase_registerBiome.invoke(null, id, name, biome);
            Field biomes_field = Biomes.class.getDeclaredField(field);
            biomes_field.setAccessible(true);
            field_modifiers.setInt(biomes_field, biomes_field.getModifiers() & ~Modifier.FINAL);
            biomes_field.set(null, biome);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}