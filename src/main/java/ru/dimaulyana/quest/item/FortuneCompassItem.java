package ru.dimaulyana.quest.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import ru.dimaulyana.quest.story.RoleAndStoryManager;

import java.util.List;

/**
 * Невыбрасываемый Компас Удачи с быстрой рулеткой ("Деп ресурсов").
 * Поддерживает 5 барабанов ставок:
 * 0 -> 64x Булыжник
 * 1 -> 16x Железо
 * 2 -> 12x Золото
 * 3 -> 8x Алмазы
 * 4 -> 2x Незерит
 * В каждом барабане ВСЕГДА есть шанс на выпадение нубского ресурса (земля / гнилая плоть / палка),
 * но с каждым уровнем кирки (1..6) и ролью Ульяны шанс на ценный куш растёт!
 */
public class FortuneCompassItem extends Item {
    private static final String DRUM_KEY = "SelectedDrumIndex";

    public FortuneCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        int drumIndex = nbt.getInt(DRUM_KEY);

        // Shift + ПКМ переключает барабан ставок (64 Булыги -> 16 Железа -> 12 Золота -> 8 Алмазов -> 2 Незерита)
        if (player.isSneaking()) {
            drumIndex = (drumIndex + 1) % 5;
            nbt.putInt(DRUM_KEY, drumIndex);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 0.9f, 1.2f + drumIndex * 0.15f);
            player.sendMessage(Text.literal("§6§l[Компас Удачи] §eВыбран барабан депа: " + getDrumDisplayName(drumIndex)), true);
            return TypedActionResult.success(stack);
        }

        // Обычный ПКМ — Мгновенный быстрый прокрут рулетки (кулдаун всего 20 тиков = 1 секунда!)
        Item requiredItem = getStakeItem(drumIndex);
        int requiredCount = getStakeCount(drumIndex);

        if (!consumeResourceFromInventory(player, requiredItem, requiredCount)) {
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0f, 0.7f);
            player.sendMessage(Text.literal("§c§l[Рулетка Компаса] §fНедостаточно ресурсов для депа! Нужно: §e" + requiredCount + "x " + requiredItem.getName().getString() + " §7(Shift+ПКМ — сменить барабан)"), true);
            return TypedActionResult.fail(stack);
        }

        // Быстрый прокрут без долгого ожидания (1 сек кулдаун)
        player.getItemCooldownManager().set(this, 20);
        int pickaxeLevel = getHighestPickaxeTierInInventory(player);
        int winChance = getWinProbability(pickaxeLevel, player);

        int roll = world.random.nextInt(100);
        ServerWorld serverWorld = (ServerWorld) world;

        if (roll < winChance) {
            // ВЫИГРЫШ ИЛИ ДЖЕКПОТ!
            boolean isJackpot = world.random.nextFloat() < (0.18f + pickaxeLevel * 0.04f);
            giveWinningReward(player, drumIndex, isJackpot);
            serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.1, player.getZ(), 28, 0.4, 0.5, 0.4, 0.15);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, isJackpot ? 1.3f : 1.0f);
        } else {
            // НУБСКИЙ ДРОП (есть в каждом барабане!)
            giveNoobDrop(player, drumIndex, pickaxeLevel, winChance);
            serverWorld.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.3, 0.3, 0.04);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 0.85f);
        }

        return TypedActionResult.success(stack);
    }

    private int getHighestPickaxeTierInInventory(PlayerEntity player) {
        int maxTier = 1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() instanceof StoryPickaxeItem storyPickaxe) {
                maxTier = Math.max(maxTier, storyPickaxe.getTierLevel());
            }
        }
        return maxTier;
    }

    private int getWinProbability(int pickaxeLevel, PlayerEntity player) {
        int base = switch (pickaxeLevel) {
            case 1 -> 38;
            case 2 -> 52;
            case 3 -> 65;
            case 4 -> 76;
            case 5 -> 86;
            default -> 94;
        };
        if (RoleAndStoryManager.getPlayerRole(player) == RoleAndStoryManager.HeroRole.ULYANA) {
            base = Math.min(97, base + 12); // Бонус роли Ульяны (+12% к удаче, но 3% шанс на нуб-дроп всегда остаётся!)
        }
        return base;
    }

    private void giveWinningReward(PlayerEntity player, int drumIndex, boolean isJackpot) {
        boolean isDima = RoleAndStoryManager.getPlayerRole(player) == RoleAndStoryManager.HeroRole.DIMA;
        int mult = isJackpot ? 2 : 1;
        if (isDima && isJackpot) mult = 3; // Бонус роли Димы при джекпоте

        switch (drumIndex) {
            case 0 -> { // Деп 64 Булыжника
                player.getInventory().offerOrDrop(new ItemStack(Items.COBBLESTONE, 64 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.IRON_INGOT, 12 * mult));
                if (isJackpot) player.getInventory().offerOrDrop(new ItemStack(Items.GOLD_INGOT, 6));
                player.sendMessage(Text.literal("§a§l[КУШ КОМПАСА!] §fДеп 64 Булыжника принёс §e" + (12 * mult) + "x Железа §fи кучу камня!"), false);
            }
            case 1 -> { // Деп 16 Железа
                player.getInventory().offerOrDrop(new ItemStack(Items.IRON_INGOT, 24 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.EMERALD, 10 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 3 * mult));
                player.sendMessage(Text.literal("§b§l[КУШ КОМПАСА!] §fДеп 16 Железа выдал §b" + (3 * mult) + "x Алмазов §fи §a" + (10 * mult) + "x Изумрудов!"), false);
            }
            case 2 -> { // Деп 12 Золота
                player.getInventory().offerOrDrop(new ItemStack(Items.GOLD_INGOT, 24 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 8 * mult));
                if (isJackpot) player.getInventory().offerOrDrop(new ItemStack(Items.NETHERITE_SCRAP, 2));
                player.sendMessage(Text.literal("§6§l[КУШ КОМПАСА!] §fЗолотой барабан насыпал §b" + (8 * mult) + "x Алмазов!"), false);
            }
            case 3 -> { // Деп 8 Алмазов
                player.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 16 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.NETHERITE_INGOT, 2 * mult));
                player.sendMessage(Text.literal("§d§l[АЛМАЗНЫЙ ДЖЕКПОТ!] §fВы подняли §5" + (2 * mult) + "x Незеритовых слитка §fи §b" + (16 * mult) + "x Алмазов!"), false);
            }
            default -> { // Деп 2 Незерита
                player.getInventory().offerOrDrop(new ItemStack(Items.NETHERITE_INGOT, 4 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 24 * mult));
                player.getInventory().offerOrDrop(new ItemStack(Items.NETHER_STAR, 1));
                player.sendMessage(Text.literal("§5§l[КОРОЛЕВСКИЙ АБСОЛЮТ!] §fКомпас выдал §e" + (4 * mult) + "x Незерита §fи §bЗвезду Сингулярности!"), false);
            }
        }
    }

    private void giveNoobDrop(PlayerEntity player, int drumIndex, int pickaxeLevel, int winChance) {
        ItemStack noobItem;
        int roll = player.getWorld().random.nextInt(3);
        if (roll == 0) {
            noobItem = new ItemStack(Items.DIRT, 1);
            noobItem.setCustomName(Text.literal("§6Нубская Земля «Слёзы Шахтёра» (Барабан #" + (drumIndex + 1) + ")"));
        } else if (roll == 1) {
            noobItem = new ItemStack(Items.ROTTEN_FLESH, 2);
            noobItem.setCustomName(Text.literal("§cГнилая Плоть Неудачи"));
        } else {
            noobItem = new ItemStack(Items.STICK, 1);
            noobItem.setCustomName(Text.literal("§eУтешительная Палка Димы и Ульяны"));
        }
        player.getInventory().offerOrDrop(noobItem);
        player.sendMessage(Text.literal("§c§l[НУБСКИЙ ДРОП!] §7Не фартануло! Выпал: " + noobItem.getName().getString()
            + " §8(Ваш уровень кирки: " + pickaxeLevel + ", шанс куша был " + winChance + "%)"), false);
    }

    private boolean consumeResourceFromInventory(PlayerEntity player, Item item, int amountNeeded) {
        int totalFound = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item)) totalFound += s.getCount();
        }
        if (totalFound < amountNeeded) return false;

        int remaining = amountNeeded;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item)) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
            }
        }
        return true;
    }

    private Item getStakeItem(int drumIndex) {
        return switch (drumIndex) {
            case 0 -> Items.COBBLESTONE;
            case 1 -> Items.IRON_INGOT;
            case 2 -> Items.GOLD_INGOT;
            case 3 -> Items.DIAMOND;
            default -> Items.NETHERITE_INGOT;
        };
    }

    private int getStakeCount(int drumIndex) {
        return switch (drumIndex) {
            case 0 -> 64;
            case 1 -> 16;
            case 2 -> 12;
            case 3 -> 8;
            default -> 2;
        };
    }

    private String getDrumDisplayName(int drumIndex) {
        return switch (drumIndex) {
            case 0 -> "§7[I] 64x Булыжник";
            case 1 -> "§f[II] 16x Железных слитков";
            case 2 -> "§6[III] 12x Золотых слитков";
            case 3 -> "§b[IV] 8x Алмазов";
            default -> "§5[V] 2x Незеритовых слитка";
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int drumIndex = stack.getOrCreateNbt().getInt(DRUM_KEY);
        tooltip.add(Text.literal("§c§l🔒 НЕВЫБРАСЫВАЕМЫЙ СЮЖЕТНЫЙ АРТЕФАКТ"));
        tooltip.add(Text.literal("§7Текущий барабан депа: " + getDrumDisplayName(drumIndex)));
        tooltip.add(Text.literal("§eПКМ §7— Быстрый прокрут рулетки (1 сек)"));
        tooltip.add(Text.literal("§bShift + ПКМ §7— Сменить ресурс ставки"));
        tooltip.add(Text.literal("§8В каждом барабане есть шанс на нубский ресурс,"));
        tooltip.add(Text.literal("§8но чем круче ваша кирка — тем выше шанс Джекпота!"));
    }
}
