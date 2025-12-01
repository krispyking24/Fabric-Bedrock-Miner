package com.github.bunnyi116.bedrockminer.mixin;

import com.github.bunnyi116.bedrockminer.BedrockMiner;
import com.github.bunnyi116.bedrockminer.task.TaskManager;
import com.github.bunnyi116.bedrockminer.util.player.PlayerInteractionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;
import static com.github.bunnyi116.bedrockminer.BedrockMiner.world;

@Mixin(value = MultiPlayerGameMode.class, priority = 999)
public abstract class MixinMultiPlayerGameMode {
    @Unique
    private int interactBlockCooldown = 0;

    @Inject(at = @At(value = "HEAD"), method = "startDestroyBlock", cancellable = true)
    private void attackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (TaskManager.getInstance().isBedrockMinerFeatureEnable()) {
            TaskManager.getInstance().addBlockTask(world, blockPos, block);
        }
        if (PlayerInteractionUtils.isBreakingBlock()) {
            cir.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "stopDestroyBlock", cancellable = true)
    public void cancelBlockBreaking(CallbackInfo ci) {
        if (PlayerInteractionUtils.isBreakingBlock()) {
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "useItemOn", cancellable = true)
    private void interactBlock(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (this.interactBlockCooldown > 0) {
            this.interactBlockCooldown--;
        } else {
            this.interactBlockCooldown = 1;
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = world.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (TaskManager.getInstance().isBedrockMinerFeatureEnable() && player.getMainHandItem().isEmpty()) {
                TaskManager.getInstance().switchToggle(block);
            }
        }
        if (PlayerInteractionUtils.isBreakingBlock()) {
            cir.setReturnValue(InteractionResult.FAIL);
            cir.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "tick", cancellable = true)
    public void tick(CallbackInfo ci) {
        updateGameVariable();
        if (PlayerInteractionUtils.isBreakingBlock()) {
            ci.cancel();
        }
        TaskManager.getInstance().tick();
        PlayerInteractionUtils.autoResetBreaking();    // 自动解除拦截玩家破坏机制，避免任务阻塞或玩家离开任务方块破坏范围
    }

    @Unique
    private void updateGameVariable() {
        BedrockMiner.client = Minecraft.getInstance();
        world = BedrockMiner.client.level;
        player = BedrockMiner.client.player;
        if (player != null) {
            BedrockMiner.playerInventory = player.getInventory();
        }
        BedrockMiner.crosshairTarget = BedrockMiner.client.hitResult;
        BedrockMiner.interactionManager = BedrockMiner.client.gameMode;
        BedrockMiner.networkHandler = BedrockMiner.client.getConnection();
        if (BedrockMiner.interactionManager != null) {
            BedrockMiner.gameMode = BedrockMiner.interactionManager.getPlayerMode();
        }
    }
}
