package ru.dimaulyana.quest;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dimaulyana.quest.item.ModItems;
import ru.dimaulyana.quest.story.RoleAndStoryManager;

public class DimaUlyanaQuestMod implements ModInitializer {
    public static final String MOD_ID = "dima_ulyana_quest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Хроники Глубин: Дима и Ульяна] Инициализация сюжетного мода Fabric 1.20.1...");
        ModItems.registerModItems();

        // При первом входе выдаём игроку Невыбрасываемый Компас Удачи и Земляную Кирку 1-го уровня
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ensureCompassAndStarterKit(player);
        });

        // Сохраняем Компас Удачи в инвентаре даже после смерти!
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            RoleAndStoryManager.copyRole(oldPlayer, newPlayer);
            ensureCompassAndStarterKit(newPlayer);
        });

        // Серверный тик: проверка баффов роли (Дима / Ульяна) и случайных мировых событий
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                RoleAndStoryManager.tickPlayerRolePassives(player);
            }
        });

        // Регистрация сюжетных команд: /role dima, /role ulyana, /story_event <id>
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("role")
                .then(CommandManager.literal("dima").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    RoleAndStoryManager.setPlayerRole(p, RoleAndStoryManager.HeroRole.DIMA);
                    return 1;
                }))
                .then(CommandManager.literal("ulyana").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    RoleAndStoryManager.setPlayerRole(p, RoleAndStoryManager.HeroRole.ULYANA);
                    return 1;
                }))
            );

            dispatcher.register(CommandManager.literal("story_event")
                .then(CommandManager.argument("event_id", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    String eventId = StringArgumentType.getString(ctx, "event_id");
                    RoleAndStoryManager.triggerWorldEvent(p, eventId);
                    return 1;
                }))
            );
        });
    }

    private void ensureCompassAndStarterKit(ServerPlayerEntity player) {
        boolean hasCompass = false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.FORTUNE_COMPASS)) {
                hasCompass = true;
                break;
            }
        }
        if (!hasCompass) {
            ItemStack compass = new ItemStack(ModItems.FORTUNE_COMPASS);
            player.getInventory().insertStack(compass);
            player.getInventory().insertStack(new ItemStack(ModItems.DIRT_PICKAXE));
            player.sendMessage(Text.literal("§6§l[Хроники Глубин] §eВы получили §bНевыбрасываемый Компас Удачи §eи §aЗемляную Кирку§e! Выберите роль: §6/role dima §eили §d/role ulyana"), false);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.1f);
        }
    }
}
