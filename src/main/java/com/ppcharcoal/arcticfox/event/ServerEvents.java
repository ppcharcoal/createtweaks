package com.ppcharcoal.arcticfox.event;

import com.ppcharcoal.arcticfox.ArcticFox;
import com.ppcharcoal.arcticfox.Config;
import com.ppcharcoal.arcticfox.SmokeTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ArcticFox.MOD_ID)
public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        GameRules rules = server.getGameRules();

        if (Config.DISABLE_WATER_SOURCES.get()) {
            rules.getRule(GameRules.RULE_WATER_SOURCE_CONVERSION).set(false, server);
        }
        if (Config.BANISH_ILLAGERS_AND_VILLAGERS.get()) {
            rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        }

        if (Config.LOCK_WINTER.get() && ModList.get().isLoaded("sereneseasons")) {
            String subSeason = Config.WINTER_SUB_SEASON.get().toLowerCase(Locale.ROOT);
            if (!subSeason.endsWith("_winter")) {
                subSeason = "mid_winter";
            }
            runCommand(server, "gamerule doSeasonCycle false");
            runCommand(server, "season set " + subSeason);
            ArcticFox.LOGGER.info("Serene Seasons locked to {}", subSeason);
        }

        if (SmokeTest.ENABLED) {
            SmokeTest.run(server);
        }
    }

    private static void runCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            server.getCommands().performPrefixedCommand(source, command);
        } catch (Exception e) {
            ArcticFox.LOGGER.warn("Failed to run '{}'", command, e);
        }
    }
}
