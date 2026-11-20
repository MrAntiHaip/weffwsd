package ru.dimaulyana.quest.story;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoleAndStoryManager {
    public enum HeroRole {
        DIMA,
        ULYANA
    }

    private static final Map<UUID, HeroRole> PLAYER_ROLES = new HashMap<>();

    public static HeroRole getPlayerRole(PlayerEntity player) {
        return PLAYER_ROLES.getOrDefault(player.getUuid(), HeroRole.DIMA);
    }

    public static void setPlayerRole(ServerPlayerEntity player, HeroRole role) {
        PLAYER_ROLES.put(player.getUuid(), role);
        if (role == HeroRole.DIMA) {
            player.sendMessage(Text.literal("§6§l[Выбор Роли: ДИМА] §eВы выбрали роль Гео-Штурмовика! §7(+Сопротивление, +мощь бурения, бонус x3 при Джекпоте в Компасе)"), false);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0f, 1.1f);
        } else {
            player.sendMessage(Text.literal("§d§l[Выбор Роли: УЛЬЯНА] §bВы выбрали роль Хранительницы Эфира! §7(+12% к шансу куша в Рулетке Компаса, Регенерация и Эфирный Резонанс)"), false);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);
        }
    }

    public static void copyRole(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        if (PLAYER_ROLES.containsKey(oldPlayer.getUuid())) {
            PLAYER_ROLES.put(newPlayer.getUuid(), PLAYER_ROLES.get(oldPlayer.getUuid()));
        }
    }

    public static void tickPlayerRolePassives(ServerPlayerEntity player) {
        if (player.getWorld().getTime() % 80 != 0) return;
        HeroRole role = getPlayerRole(player);
        if (role == HeroRole.DIMA) {
            if (player.getY() < 16) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 120, 0, true, false));
            }
        } else {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 120, 1, true, false));
        }
    }

    public static void triggerWorldEvent(ServerPlayerEntity player, String eventId) {
        ServerWorld world = player.getServerWorld();
        switch (eventId) {
            case "compass_gold_fever" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 900, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 900, 1));
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.6, 0.6, 0.1);
                player.sendMessage(Text.literal("§6§l[СОБЫТИЕ МИРА] §eЗолотая Лихорадка Компаса! Удача повышена до максимума!"), false);
            }
            case "tectonic_meteor_vein" -> {
                player.getInventory().offerOrDrop(new ItemStack(Items.COBBLESTONE, 64));
                player.getInventory().offerOrDrop(new ItemStack(Items.IRON_INGOT, 16));
                player.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 4));
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8f, 0.9f);
                player.sendMessage(Text.literal("§b§l[СОБЫТИЕ МИРА] §fТектонический Разлом вскрыл Супер-Жилу руд для Димы и Ульяны!"), false);
            }
            case "gambler_archivist_deal" -> {
                player.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 8));
                player.getInventory().offerOrDrop(new ItemStack(Items.NETHERITE_SCRAP, 2));
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
                player.sendMessage(Text.literal("§d§l[СОБЫТИЕ МИРА] §fСтранствующий Архивариус обменял ваш нубский дроп на чистые Алмазы и Незерит!"), false);
            }
            default -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 3));
                player.sendMessage(Text.literal("§a§l[СОБЫТИЕ МИРА] §fЭфирный Шторм Ульяны и Прорыв Димы активированы!"), false);
            }
        }
    }
}
