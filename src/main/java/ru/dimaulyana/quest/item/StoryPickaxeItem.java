package ru.dimaulyana.quest.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StoryPickaxeItem extends PickaxeItem {
    private final int tierLevel;

    public StoryPickaxeItem(ToolMaterial material, int attackDamage, float attackSpeed, int tierLevel, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
        this.tierLevel = tierLevel;
    }

    public int getTierLevel() {
        return this.tierLevel;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        float base = super.getMiningSpeedMultiplier(stack, state);
        return base > 1.0f ? (4.0f + tierLevel * 5.5f) : base;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return this.tierLevel >= 3 || super.hasGlint(stack);
    }

    // Пассивные баффы, пока сюжетная кирка находится в руке Димы или Ульяны
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && selected && entity instanceof LivingEntity living) {
            if (tierLevel >= 1 && world.getTime() % 40 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, Math.min(3, tierLevel - 1), true, false));
            }
            if (tierLevel >= 2 && world.getTime() % 40 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, true, false));
            }
            if (tierLevel >= 3 && world.getTime() % 40 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, true, false));
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 60, 0, true, false));
            }
            if (tierLevel >= 4 && world.getTime() % 40 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1, true, false));
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 0, true, false));
            }
            if (tierLevel >= 5 && world.getTime() % 40 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1, true, false));
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 1, true, false));
            }
            if (tierLevel >= 6 && world.getTime() % 40 == 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 1, true, false));
                // Неразрушимость для 6-го уровня
                if (stack.isDamaged()) {
                    stack.setDamage(0);
                }
            }
        }
    }

    // Визуальные эффекты частиц и способности ломания (1x3, VeinMiner, 3x3, 5x5 + Авто-магнит)
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && miner instanceof PlayerEntity player) {
            spawnTierParticles(serverWorld, pos);

            // Уровень 1 (Земляная Кирка): 25% шанс выкопать бонусный булыжник прямо из земли/камня для депа
            if (tierLevel == 1 && world.random.nextFloat() < 0.25f) {
                Block.dropStack(world, pos, new ItemStack(Items.COBBLESTONE, 2));
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
            }

            // Уровень 2 (Базальтовый Крушитель): вертикальный пропил 1x3
            if (tierLevel == 2) {
                breakExtraBlock(serverWorld, pos.up(), player);
                breakExtraBlock(serverWorld, pos.down(), player);
            }

            // Уровень 3 (Изумрудная Кирка): цепной раскол соседних блоков руды
            if (tierLevel == 3) {
                for (BlockPos offset : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                    if (!offset.equals(pos) && serverWorld.getBlockState(offset).getBlock().getTranslationKey().contains("ore")) {
                        breakExtraBlock(serverWorld, offset, player);
                    }
                }
            }

            // Уровень 4 и 5: Бурение 3x3 + Вакуумный магнит всех выпавших ресурсов прямо в инвентарь!
            if (tierLevel == 4 || tierLevel == 5) {
                for (BlockPos target : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                    if (!target.equals(pos)) {
                        breakExtraBlock(serverWorld, target, player);
                    }
                }
                vacuumDropsToPlayer(serverWorld, pos, player, 6.0);
            }

            // Уровень 6 (Хроно-Звёздный Бур): Сфера 5x5 + умножение руд + вакуумный магнит
            if (tierLevel >= 6) {
                for (BlockPos target : BlockPos.iterate(pos.add(-2, -1, -2), pos.add(2, 2, 2))) {
                    if (!target.equals(pos)) {
                        breakExtraBlock(serverWorld, target, player);
                    }
                }
                vacuumDropsToPlayer(serverWorld, pos, player, 12.0);
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    private void breakExtraBlock(ServerWorld world, BlockPos target, PlayerEntity player) {
        BlockState bs = world.getBlockState(target);
        if (!bs.isAir() && bs.getHardness(world, target) >= 0 && bs.getHardness(world, target) < 50.0f) {
            world.breakBlock(target, true, player);
        } else if (tierLevel >= 6 && bs.isOf(Blocks.BEDROCK) && target.getY() > -63) {
            // 6-й уровень способен пробить сюжетные печати Бедрока!
            world.breakBlock(target, false, player);
            Block.dropStack(world, target, new ItemStack(Items.NETHER_STAR, 1));
        }
    }

    private void vacuumDropsToPlayer(ServerWorld world, BlockPos center, PlayerEntity player, double radius) {
        List<ItemEntity> drops = world.getEntitiesByClass(ItemEntity.class, new Box(center).expand(radius), e -> true);
        for (ItemEntity itemEntity : drops) {
            itemEntity.setPosition(player.getX(), player.getY() + 0.5, player.getZ());
            itemEntity.setPickupDelay(0);
        }
    }

    private void spawnTierParticles(ServerWorld world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (tierLevel) {
            case 1 -> world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.35, 0.35, 0.35, 0.05);
            case 2 -> world.spawnParticles(ParticleTypes.FLAME, x, y, z, 14, 0.35, 0.35, 0.35, 0.04);
            case 3 -> world.spawnParticles(ParticleTypes.SCRAPE, x, y, z, 18, 0.4, 0.4, 0.4, 0.08);
            case 4 -> world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 24, 0.5, 0.5, 0.5, 0.12);
            case 5 -> {
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 22, 0.5, 0.5, 0.5, 0.06);
                world.spawnParticles(ParticleTypes.LAVA, x, y, z, 8, 0.3, 0.3, 0.3, 0.05);
            }
            default -> {
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.6, 0.6, 0.6, 0.1);
                world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 25, 0.5, 0.5, 0.5, 0.2);
            }
        }
    }

    // Активная способность кирки по нажатию ПКМ!
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            user.getItemCooldownManager().set(this, Math.max(40, 160 - tierLevel * 20));
            spawnTierParticles(serverWorld, user.getBlockPos().up());

            if (tierLevel == 1) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 100, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 160, 1));
                user.sendMessage(Text.literal("§a[Корневой Упор] §fСпоры мха восстановили силы и ускорили копку!"), true);
            } else if (tierLevel == 2) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 300, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 200, 1));
                user.sendMessage(Text.literal("§6[Кузнечный Импульс] §fГорн разогрет! Скорость бурения и защита от лавы активны!"), true);
            } else if (tierLevel == 3) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 400, 1));
                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);
                user.sendMessage(Text.literal("§2[Эхо-Радар Ульяны] §fКристаллы резонируют с ближайшими жилами руд!"), true);
            } else if (tierLevel == 4) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 2));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 2));
                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7f, 1.4f);
                user.sendMessage(Text.literal("§b[Шаровая Молния Димы] §fГрозовой разряд ускорил вас до предела!"), true);
            } else if (tierLevel == 5) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 2));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 300, 2));
                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
                user.sendMessage(Text.literal("§c[Сердце Вулкана] §fМагмовый щит и тройная мощь активированы!"), true);
            } else {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80, 1));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 240, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 400, 3));
                world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.9f, 1.2f);
                user.sendMessage(Text.literal("§d§l[Хроно-Сингулярность] §eЗвёздный Овердрайв Димы и Ульяны активирован!"), true);
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("§7Уровень эволюции кирки: §6§l" + tierLevel + " / 6"));
        int winChance = switch (tierLevel) {
            case 1 -> 38;
            case 2 -> 52;
            case 3 -> 65;
            case 4 -> 76;
            case 5 -> 86;
            default -> 94;
        };
        tooltip.add(Text.literal("§7Шанс куша в Компасе Удачи: §a" + winChance + "% §8(Нуб-дроп: " + (100 - winChance) + "%)"));
        tooltip.add(Text.literal("§eПКМ §7— Активировать уникальную способность и частицы"));
    }
}
