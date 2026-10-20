package com.ppcharcoal.arcticfox.item;

import com.ppcharcoal.arcticfox.ArcticFox;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Backport of the 1.21.11 spear.
 * <ul>
 *     <li><b>Jab</b> (attack): quick stab with +1.5 block reach.</li>
 *     <li><b>Charge</b> (hold use): while held, any creature in front of the tip is struck; damage scales with how
 *     fast you (or your mount) are moving. A full gallop on a horse hits about 3x the jab damage and can knock
 *     riders off their mounts. Each creature can only be struck once per second; a charge lasts up to 5 seconds.</li>
 * </ul>
 */
public class SpearItem extends TieredItem {
    private static final double REACH_BONUS = 1.5;
    private static final double CHARGE_REACH = 4.0;
    private static final double MIN_CHARGE_SPEED = 0.18; // blocks/tick (a brisk sprint is ~0.28)
    private static final int MAX_CHARGE_TICKS = 100;

    private final float jabDamage;
    private static final Map<LivingEntity, Vec3> LAST_POS = new WeakHashMap<>();
    private static final Map<Entity, Long> LAST_HIT = new WeakHashMap<>();

    public SpearItem(Tier tier, float jabDamage, Properties properties) {
        super(tier, properties.attributes(attributes(tier, jabDamage)));
        this.jabDamage = jabDamage;
    }

    public static ItemAttributeModifiers attributes(Tier tier, float jabDamage) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                        jabDamage - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                        -2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ArcticFox.id("spear_reach"),
                        REACH_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public float getJabDamage() {
        return jabDamage;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        LAST_POS.put(player, mover(player).position());
        return InteractionResultHolder.consume(stack);
    }

    private static Entity mover(LivingEntity entity) {
        Entity vehicle = entity.getVehicle();
        return vehicle != null ? vehicle : entity;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel server)) return;
        if (getUseDuration(stack, user) - remainingUseDuration > MAX_CHARGE_TICKS) {
            user.releaseUsingItem();
            return;
        }
        Vec3 now = mover(user).position();
        Vec3 before = LAST_POS.put(user, now);
        if (before == null) return;
        double speed = now.subtract(before).horizontalDistance();
        if (speed < MIN_CHARGE_SPEED) return;

        Vec3 look = user.getLookAngle();
        Vec3 eye = user.getEyePosition();
        AABB box = user.getBoundingBox().expandTowards(look.scale(CHARGE_REACH)).inflate(0.6);
        long tick = server.getGameTime();
        Entity vehicle = user.getVehicle();
        for (Entity target : server.getEntities(user, box, e -> e instanceof LivingEntity && e.isAlive() && e != vehicle && !e.isAlliedTo(user))) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > CHARGE_REACH + target.getBbWidth() || to.normalize().dot(look) < 0.75) continue;
            Long last = LAST_HIT.get(target);
            if (last != null && tick - last < 20) continue;
            LAST_HIT.put(target, tick);

            float multiplier = (float) Math.min(3.5, speed / 0.28 * 1.25);
            float damage = jabDamage * multiplier;
            DamageSource source = user instanceof Player p ? server.damageSources().playerAttack(p) : server.damageSources().mobAttack(user);
            if (target.hurt(source, damage)) {
                Vec3 push = look.multiply(1, 0, 1).normalize().scale(0.4 + speed);
                target.push(push.x, 0.15, push.z);
                if (speed > 0.45 && target.isPassenger()) target.stopRiding();
                stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
                server.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 0.8F + (float) speed);
                if (stack.isEmpty()) return;
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        LAST_POS.remove(user);
    }
}
