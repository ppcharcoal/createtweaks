package com.ppcharcoal.arcticfox.entity;

import com.ppcharcoal.arcticfox.registry.ModSounds;
import com.ppcharcoal.arcticfox.world.WinterWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Wendigo stalks one player through the blizzard at night.
 * <ul>
 *     <li>While the player looks at it, it freezes and stares back.</li>
 *     <li>If the player walks towards it while looking, it bolts away into the fog and vanishes, then returns
 *     30 seconds later from a different direction.</li>
 *     <li>While the player looks away it creeps closer; once within reach (or if it is spotted very close)
 *     it screams and attacks until the player is dead.</li>
 *     <li>At dawn it is gone, unless it is already hunting.</li>
 * </ul>
 */
public class Wendigo extends Monster {
    public static final int STALK = 0, WATCHED = 1, SCREAM = 2, HUNT = 3, FLEE = 4, HIDDEN = 5;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Wendigo.class, EntityDataSerializers.INT);

    public static final int HIDE_TICKS = 600;      // 30 seconds out of sight
    private static final int SCREAM_TICKS = 40;
    private static final double STRIKE_RANGE = 3.0;
    private static final double SPOTTED_CLOSE = 5.0;

    @Nullable private UUID preyId;
    @Nullable private Player preyRef;
    private int hiddenTicks;
    private int fleeTicks;
    private int screamTicks;
    private int attackCooldown;
    private int soundCooldown = 80;
    private int lostTicks;
    private double watchStartDist = -1;
    private double lastBearing;
    private int swings;
    /** Counts claw attacks (for tests). */
    public int strikes;

    // client-side animation clocks
    private int stateChangedAt;

    public Wendigo(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)      // 5x a zombie
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, STALK);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int state) {
        if (getState() != state) this.entityData.set(STATE, state);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) stateChangedAt = this.tickCount;
    }

    /** Ticks spent in the current state (client animation clock). */
    public float watchedFor(float ageInTicks) {
        return ageInTicks - stateChangedAt;
    }

    /** 0..1 progress of the scream, or 0 when not screaming. */
    public float screamProgress(float ageInTicks) {
        if (getState() != SCREAM) return 0;
        return Mth.clamp((ageInTicks - stateChangedAt) / SCREAM_TICKS, 0.001F, 1.0F);
    }

    public int swingCount() {
        return swings;
    }

    public void setPrey(Player player) {
        this.preyRef = player;
        this.preyId = player.getUUID();
        this.lastBearing = bearingFrom(player);
    }

    @Nullable
    public Player getPrey() {
        Player p = preyRef;
        if (p == null || p.isRemoved() || p.level() != level()) {
            p = preyId == null ? null : level().getPlayerByUUID(preyId);
        }
        if (p != null && (!p.isAlive() || p.isSpectator() || p.isCreative() || p.distanceToSqr(this) > 160 * 160)) p = null;
        if (p == null) {
            p = level().getNearestPlayer(getX(), getY(), getZ(), 64, e -> e instanceof Player pl && !pl.isCreative() && !pl.isSpectator());
            if (p != null) setPrey(p);
        }
        preyRef = p;
        return p;
    }

    private double bearingFrom(Player player) {
        return Math.atan2(getZ() - player.getZ(), getX() - player.getX());
    }

    /** True if the player is looking roughly at the Wendigo (a ~25 degree cone) with nothing solid in between. */
    public boolean isLookedAtBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - player.getX(), getY(0.75) - player.getEyeY(), getZ() - player.getZ());
        double dist = to.length();
        if (dist < 0.01) return true;
        double dot = view.dot(to.scale(1 / dist));
        double threshold = dist < 6 ? 0.8 : 0.9;
        return dot > threshold && player.hasLineOfSight(this);
    }

    public boolean isHidden() {
        return hiddenTicks > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) attackCooldown--;
        if (hiddenTicks > 0) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (--hiddenTicks == 0) reappear();
            return;
        }
        Player prey = getPrey();
        int state = getState();
        if (prey == null) {
            getNavigation().stop();
            if (++lostTicks > 200) vanishForGood();
            return;
        }
        lostTicks = 0;
        if (state != HUNT && state != SCREAM && !WinterWeather.isNight(level())) {
            vanishForGood(); // gone with the dawn
            return;
        }
        double dist = distanceTo(prey);
        switch (state) {
            case SCREAM -> {
                getNavigation().stop();
                getLookControl().setLookAt(prey, 30, 30);
                if (--screamTicks <= 0) setState(HUNT);
            }
            case HUNT -> hunt(prey, dist);
            case FLEE -> {
                Vec3 away = position().subtract(prey.position()).normalize().scale(24).add(position());
                getNavigation().moveTo(away.x, away.y, away.z, 1.9);
                if (--fleeTicks <= 0 || dist > 30) hide(prey);
            }
            default -> stalk(prey, dist);
        }
        stalkSounds(prey);
    }

    private void stalk(Player prey, double dist) {
        if (isLookedAtBy(prey)) {
            if (dist <= SPOTTED_CLOSE) {
                startScream(prey);
                return;
            }
            if (getState() != WATCHED) {
                setState(WATCHED);
                watchStartDist = dist;
            }
            // frozen in place, staring back
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            getLookControl().setLookAt(prey, 10, 10);
            float yaw = (float) (Mth.atan2(prey.getZ() - getZ(), prey.getX() - getX()) * Mth.RAD_TO_DEG) - 90;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 4));
            yBodyRot = getYRot();
            if (watchStartDist - dist > 3.0) startFlee();
            watchStartDist = Math.max(watchStartDist, dist);
            return;
        }
        setState(STALK);
        watchStartDist = dist;
        if (dist <= STRIKE_RANGE) {
            startScream(prey);
            return;
        }
        double speed = dist > 18 ? 0.55 : dist > 8 ? 0.85 : 1.3;
        getNavigation().moveTo(prey, speed);
        getLookControl().setLookAt(prey, 10, 10);
    }

    private void hunt(Player prey, double dist) {
        setTarget(prey);
        getLookControl().setLookAt(prey, 30, 30);
        getNavigation().moveTo(prey, 1.55);
        double reach = getBbWidth() * 2 + prey.getBbWidth() + 1.2;
        if (dist <= reach && attackCooldown <= 0 && hasLineOfSight(prey)) {
            swings++;
            swing(InteractionHand.MAIN_HAND);
            strikes++;
            doHurtTarget(prey);
            attackCooldown = 14;
        }
        if (!prey.isAlive() || dist > 80) {
            setTarget(null);
            setState(STALK);
            hide(prey);
        }
    }

    private void startScream(Player prey) {
        setState(SCREAM);
        screamTicks = SCREAM_TICKS;
        getNavigation().stop();
        playSound(ModSounds.WENDIGO_SCREAM.get(), 4.0F, 0.9F + random.nextFloat() * 0.15F);
        prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        prey.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
    }

    private void startFlee() {
        setState(FLEE);
        fleeTicks = 30;
        playSound(ModSounds.WENDIGO_VANISH.get(), 1.2F, 0.9F + random.nextFloat() * 0.2F);
    }

    /** Goes out of sight for 30 seconds. */
    private void hide(Player prey) {
        lastBearing = bearingFrom(prey);
        poof();
        setState(HIDDEN);
        hiddenTicks = HIDE_TICKS;
        setInvisible(true);
        setInvulnerable(true);
        setSilent(true);
        getNavigation().stop();
    }

    public void forceHide(Player prey) {
        hide(prey);
    }

    /** Ends the hiding immediately (tests). */
    public void endHideNow() {
        if (hiddenTicks > 0) {
            hiddenTicks = 0;
            reappear();
        }
    }

    public double lastBearing() {
        return lastBearing;
    }

    /** Comes back 26-34 blocks from the prey, at least 110 degrees away from where it vanished, out of sight. */
    private void reappear() {
        Player prey = getPrey();
        setInvisible(false);
        setInvulnerable(false);
        setSilent(false);
        setState(STALK);
        if (prey == null) return;
        for (int attempt = 0; attempt < 24; attempt++) {
            double turn = Math.toRadians(110 + random.nextDouble() * 140) * (random.nextBoolean() ? 1 : -1);
            double angle = lastBearing + turn;
            double r = 26 + random.nextDouble() * 8;
            int x = Mth.floor(prey.getX() + Math.cos(angle) * r);
            int z = Mth.floor(prey.getZ() + Math.sin(angle) * r);
            if (!level().hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos top = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            BlockState ground = level().getBlockState(top.below());
            if (!level().getFluidState(top.below()).isEmpty()) continue;
            if (attempt < 18 && !ground.isFaceSturdy(level(), top.below(), net.minecraft.core.Direction.UP)
                    && !ground.is(net.minecraft.world.level.block.Blocks.SNOW)) continue;
            Vec3 spot = new Vec3(x + 0.5, top.getY(), z + 0.5);
            Vec3 to = spot.subtract(prey.getEyePosition()).normalize();
            if (attempt < 16 && prey.getViewVector(1.0F).dot(to) > 0.5) continue; // prefer out of view
            teleportTo(spot.x, spot.y, spot.z);
            lastBearing = angle;
            playSound(ModSounds.WENDIGO_CLICK.get(), 0.8F, 0.8F);
            return;
        }
    }

    private void vanishForGood() {
        poof();
        discard();
    }

    private void poof() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(1.0), getZ(), 40, 0.5, 1.2, 0.5, 0.05);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.6), getZ(), 12, 0.3, 0.8, 0.3, 0.01);
        }
    }

    /** Barely audible breathing, bone clicks and snow crunching from wherever it is lurking. */
    private void stalkSounds(Player prey) {
        int state = getState();
        if (state == HUNT || state == SCREAM || --soundCooldown > 0) return;
        soundCooldown = 120 + random.nextInt(200);
        SoundEvent sound = random.nextInt(3) == 0 ? ModSounds.WENDIGO_CLICK.get() : ModSounds.WENDIGO_BREATH.get();
        level().playSound(null, getX(), getEyeY(), getZ(), sound, SoundSource.HOSTILE, 0.5F, 0.85F + random.nextFloat() * 0.2F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isHidden()) return false;
        boolean hurt = super.hurt(source, amount);
        int state = getState();
        if (hurt && isAlive() && (state == STALK || state == WATCHED) && source.getEntity() instanceof Player) {
            startFlee(); // wounded while stalking: it melts back into the storm
        }
        return hurt;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        float volume = getState() == HUNT ? 0.6F : 0.12F;
        playSound(ModSounds.WENDIGO_STEP.get(), volume, 0.8F + random.nextFloat() * 0.3F);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WENDIGO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WENDIGO_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isHidden() && super.isPushable();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isHidden();
    }

    @Override
    public boolean isPickable() {
        return !isHidden() && super.isPickable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (preyId != null) tag.putUUID("Prey", preyId);
        tag.putInt("Hidden", hiddenTicks);
        tag.putInt("WendigoState", getState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Prey")) preyId = tag.getUUID("Prey");
        hiddenTicks = tag.getInt("Hidden");
        setState(hiddenTicks > 0 ? HIDDEN : Math.min(tag.getInt("WendigoState"), HUNT));
        if (hiddenTicks > 0) {
            setInvisible(true);
            setInvulnerable(true);
        }
    }
}
