package net.waterworld.worldflood;

import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.waterworld.worldflood.worldgen.StructureConfigPopulator;
import net.waterworld.worldflood.worldgen.StructureDetector;
import net.waterworld.worldflood.worldgen.StructureOverrideManager;
import org.slf4j.Logger;

@Mod(WorldFlood.MOD_ID)
public class WorldFlood {
    public static final String MOD_ID = "worldflood";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldFlood(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, WorldFloodConfig.SPEC);
        modEventBus.addListener(this::onConfigReload);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        LOGGER.info("World Flood loaded.");
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MOD_ID)
                .then(Commands.literal("listStructures")
                        .executes(context -> {
                            var registry = context.getSource().getServer()
                                    .registryAccess().registryOrThrow(Registries.STRUCTURE);
                            var ids = registry.keySet().stream().sorted().toList();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Loaded " + ids.size() + " structures (see log for full list)."), false);
                            LOGGER.info("=== Registered structures ===");
                            for (var id : ids) {
                                LOGGER.info("  {}", id);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private void onServerStarting(ServerStartingEvent event) {
        StructureDetector.writeDetectedFile(event.getServer());
        StructureConfigPopulator.populate(event.getServer());
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            StructureOverrideManager.invalidateCache();
        }
    }
}
