package ru.dimaulyana.quest.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.dimaulyana.quest.item.ModItems;

/**
 * Mixin-перехватчик: запрещает выкидывать Компас Удачи из инвентаря
 * (как через клавишу Q, так и перетаскиванием за пределы окна инвентаря).
 */
@Mixin(PlayerEntity.class)
public abstract class SoulboundCompassMixin {

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void preventDroppingFortuneCompass(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if (stack != null && stack.isOf(ModItems.FORTUNE_COMPASS)) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            // Возвращаем компас обратно в инвентарь игрока
            self.getInventory().insertStack(stack.copy());
            self.sendMessage(Text.literal("§c§l[Защита Сюжета] §fКомпас Удачи связан с душами Димы и Ульяны! Его нельзя выкинуть из инвентаря!"), true);
            self.getWorld().playSound(null, self.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.9f, 1.1f);
            cir.setReturnValue(null);
        }
    }
}
