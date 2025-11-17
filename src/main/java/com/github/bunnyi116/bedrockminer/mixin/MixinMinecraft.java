package com.github.bunnyi116.bedrockminer.mixin;

import com.github.bunnyi116.bedrockminer.BedrockMiner;
import com.github.bunnyi116.bedrockminer.task.TaskManager;
import com.github.bunnyi116.bedrockminer.util.ClientPlayerInteractionManagerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = Minecraft.class, priority = 999)
public abstract class MixinMinecraft {
    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public HitResult hitResult;

    @Inject(method = "startUseItem", at = @At(value = "HEAD"))
    private void doItemUse(CallbackInfo ci) {
        if (hitResult == null || level == null || player == null) {
            return;
        }
        if (hitResult.getType() != HitResult.Type.BLOCK || !player.getMainHandItem().isEmpty()) {
            return;
        }
        var blockHitResult = (BlockHitResult) hitResult;
        var blockPos = blockHitResult.getBlockPos();
        var blockState = level.getBlockState(blockPos);
        var block = blockState.getBlock();
        if (TaskManager.getInstance().isBedrockMinerFeatureEnable()) {
            TaskManager.getInstance().switchToggle(block);
        }
    }

    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getDirection()Lnet/minecraft/core/Direction;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void handleBlockBreaking(boolean bl, CallbackInfo ci, BlockHitResult blockHitResult, BlockPos blockPos) {
        if (level == null || player == null) {
            return;
        }
        var blockState = level.getBlockState(blockPos);
        var block = blockState.getBlock();
        if (TaskManager.getInstance().isBedrockMinerFeatureEnable()) {
            TaskManager.getInstance().addBlockTask(level, blockPos, block);
        }
        if (TaskManager.getInstance().isProcessing() || ClientPlayerInteractionManagerUtils.isBreakingBlock()) {    // 避免冲突, 当模组正在破坏时, 拦截玩家破坏操作
            ci.cancel();
        }
    }

    @Inject(method = "handleKeybinds", at = @At(value = "HEAD"))
    public void tick(CallbackInfo ci) {
        updateGameVariable();
        TaskManager.getInstance().tick();
        ClientPlayerInteractionManagerUtils.autoResetBreaking();    // 自动解除拦截玩家破坏机制，避免任务阻塞或玩家离开任务方块破坏范围
    }

    @Unique
    private void updateGameVariable() {
        BedrockMiner.client = (Minecraft) (Object) this;
        BedrockMiner.world = BedrockMiner.client.level;
        BedrockMiner.player = BedrockMiner.client.player;
        if (BedrockMiner.player != null) {
            BedrockMiner.playerInventory = BedrockMiner.player.getInventory();
        }
        BedrockMiner.crosshairTarget = BedrockMiner.client.hitResult;
        BedrockMiner.interactionManager = BedrockMiner.client.gameMode;
        BedrockMiner.networkHandler = BedrockMiner.client.getConnection();
        if (BedrockMiner.interactionManager != null) {
            BedrockMiner.gameMode = BedrockMiner.interactionManager.getPlayerMode();
        }
    }
}
