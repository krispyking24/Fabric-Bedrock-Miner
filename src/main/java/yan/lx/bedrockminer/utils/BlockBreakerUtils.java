package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;


public class BlockBreakerUtils {

    /**
     * 破坏活塞方块
     *
     * @param pos 位置
     * @return true为破坏成功，false表示正在破坏（需要等到下一个tick处理）
     */
    public static boolean instantBreakPistonBlock(BlockPos pos) {
        return instantBreakBlock(pos, Direction.UP, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
    }

    /**
     * 破坏活塞方块
     *
     * @param pos 位置
     * @return true为破坏成功，false表示正在破坏（需要等到下一个tick处理）
     */
    public static boolean breakPistonBlock(BlockPos pos) {
        return breakBlock(pos, Direction.UP, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
    }

    /**
     * 破坏活塞方块
     *
     * @param pos 位置
     */
    public static boolean breakBlock(BlockPos pos) {
       return breakBlock(pos, Direction.UP);
    }

    /**
     * 破坏活塞方块
     *
     * @param pos 位置
     */
    public static boolean instantBreakBlock(BlockPos pos) {
        return breakBlock(pos, Direction.UP);
    }

    /**
     * 瞬间破坏方块
     *
     * @param blockPos 目标方块位置
     * @param useItem  使用什么物品进行破坏
     * @return true为破坏成功，false表示无法瞬间完成破坏
     */
    public static boolean instantBreakBlock(BlockPos blockPos, Direction direction, Item... useItem) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientWorld world = minecraftClient.world;
        PlayerEntity player = minecraftClient.player;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        if (world == null || player == null || interactionManager == null || blockPos == null) {
            return false;
        }
        Block block = world.getBlockState(blockPos).getBlock();
        // 获取玩家背包中可以使用的物品清单
        if (useItem.length == 0) {
            useItem = new Item[]{player.getInventory().getMainHandStack().getItem()};
        }
        HashMap<Integer, ItemStack> itemStacks = InventoryManagerUtils.getPlayerInventoryUsableItemSlotMap(useItem);
        for (var itemSlot : itemStacks.keySet()) {
            ItemStack itemStack = itemStacks.get(itemSlot);
            // 检查耐久是否发起警告(剩余耐久<=检查值)
            if (InventoryManagerUtils.isItemDamageWarning(itemStack, 10)) {
                continue;
            }
            // 检查目标方块是否可以瞬间破坏的(0.05秒内完成也就是1tick)
            if (InventoryManagerUtils.isInstantBreakingBlock(block, itemStack)) {
                InventoryManagerUtils.switchToItemSlot(itemSlot);        // 切换到物品
                interactionManager.attackBlock(blockPos, direction);     // 攻击方块
                return world.getBlockState(blockPos).isAir();
            }
        }
        return false;
    }

    /**
     * 破坏方块 (当前如果不能瞬间破坏，则下一个tick继续运行)
     *
     * @param blockPos  目标方块位置
     * @param direction 攻击的方向
     * @param useItem   使用什么物品进行破坏
     * @return true为破坏成功，false表示还在进行中
     */
    public static boolean breakBlock(BlockPos blockPos, Direction direction, Item... useItem) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        ClientWorld world = minecraftClient.world;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        if (player == null || world == null || interactionManager == null || blockPos == null) {
            return false;
        }
        // 获取玩家背包中可以使用的物品清单
        if (useItem.length == 0) {
            useItem = new Item[]{player.getInventory().getMainHandStack().getItem()};
        }
        HashMap<Integer, ItemStack> itemStacks = InventoryManagerUtils.getPlayerInventoryUsableItemSlotMap(useItem);
        for (var itemSlot : itemStacks.keySet()) {
            InventoryManagerUtils.switchToItemSlot(itemSlot);    // 切换到物品
            // 更新方块正在破坏进程
            if (interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
                minecraftClient.particleManager.addBlockBreakingParticles(blockPos, direction);
            }
            // 检查是否已经成功
            return world.getBlockState(blockPos).isAir();
        }
        return false;
    }

}