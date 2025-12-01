package com.github.bunnyi116.bedrockminer;

import com.github.bunnyi116.bedrockminer.command.CommandManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BedrockMiner implements ModInitializer  {
    public static final String MOD_NAME = "Bedrock Miner";
    public static final String MOD_ID = "bedrockminer";
    public static final String COMMAND_PREFIX = "bedrockMiner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final boolean TEST = false;

    // 常用游戏变量(通过 mixin 从 MultiPlayerGameMode 更新)
    public static Minecraft client;
    public static ClientLevel world;
    public static LocalPlayer player;
    public static Inventory playerInventory;
    public static @Nullable HitResult crosshairTarget;
    public static ClientPacketListener networkHandler;
    public static MultiPlayerGameMode interactionManager;
    public static GameType gameMode;

    @Override
    public void onInitialize() {
        initGameVariable();
        CommandManager.register();
        Debug.alwaysWrite("模组初始化成功");
    }

    public static void initGameVariable() {
        var mc = Minecraft.getInstance();
        BedrockMiner.client = mc;
        BedrockMiner.world = mc.level;
        BedrockMiner.player = mc.player;
        if (mc.player != null) {
            BedrockMiner.playerInventory = mc.player.getInventory();
        }
        BedrockMiner.crosshairTarget = mc.hitResult;
        BedrockMiner.networkHandler = mc.getConnection();
        BedrockMiner.interactionManager = mc.gameMode;
        if (mc.gameMode!= null) {
            BedrockMiner.gameMode = mc.gameMode.getPlayerMode();
        }
    }

    public static boolean gameVariableIsValid() {
        return client != null
                && world != null
                && player != null
                && networkHandler != null
                && interactionManager != null;
    }
}
