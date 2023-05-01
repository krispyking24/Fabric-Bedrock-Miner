package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.FluidTags;

import java.util.HashMap;


public class InventoryManagerUtils {
    public static void switchToItem(ItemConvertible item) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (player == null || interactionManager == null || clientPlayNetworkHandler == null) {
            return;
        }
        PlayerInventory playerInventory = player.getInventory();

        int i = playerInventory.getSlotWithStack(new ItemStack(item));

        if (item.equals(Items.DIAMOND_PICKAXE)) {
            i = getEfficientTool(playerInventory);
        }

        if (i != -1) {
            if (PlayerInventory.isValidHotbarIndex(i)) {
                playerInventory.selectedSlot = i;
            } else {
                interactionManager.pickFromInventory(i);
            }
            clientPlayNetworkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot));
        }
    }

    private static int getEfficientTool(PlayerInventory playerInventory) {
        for (int i = 0; i < playerInventory.main.size(); ++i) {
            if (getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) > 45f) {
                return i;
            }
        }
        return -1;
    }

    /*** 检查是否可以立即开采活塞 ***/
    public static boolean canInstantlyMinePiston() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        if (player == null) {
            return false;
        }
        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.size(); i++) {
            if (getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) > 45f) {
                return true;
            }
        }
        return false;
    }

    /*** 获取方块破坏速度 ***/
    private static float getBlockBreakingSpeed(BlockState block, int slot) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        if (player == null) {
            return 0;
        }

        PlayerInventory playerInventory = player.getInventory();
        ItemStack stack = playerInventory.getStack(slot);

        float f = stack.getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
            ItemStack itemStack = player.getInventory().getStack(slot);
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k;
            StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.MINING_FATIGUE);   //采矿疲劳;
            if (statusEffect == null) {
                return 0;
            }
            k = switch (statusEffect.getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            f *= k;
        }

        if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
            f /= 5.0F;
        }

        if (!player.isOnGround()) {
            f /= 5.0F;
        }

        return f;
    }

    /*** 获取物品数量 ***/
    public static int getInventoryItemCount(ItemConvertible item) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player == null) return 0;
        PlayerInventory playerInventory = minecraftClient.player.getInventory();
        return playerInventory.count(item.asItem());
    }

    public static String warningMessage() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.interactionManager != null && !"survival".equals(minecraftClient.interactionManager.getCurrentGameMode().getName())) {
            return "bedrockminer.fail.missing.survival";
        }

        if (InventoryManagerUtils.getInventoryItemCount(Blocks.PISTON) < 2) {
            return "bedrockminer.fail.missing.piston";
        }

        if (InventoryManagerUtils.getInventoryItemCount(Blocks.REDSTONE_TORCH) < 1) {
            return "bedrockminer.fail.missing.redstonetorch";
        }

        if (InventoryManagerUtils.getInventoryItemCount(Blocks.SLIME_BLOCK) < 1) {
            return "bedrockminer.fail.missing.slime";
        }

        if (!InventoryManagerUtils.canInstantlyMinePiston()) {
            return "bedrockminer.fail.missing.instantmine";
        }
        return null;
    }


    public static HashMap<Integer, ItemStack> getPlayerInventoryUsableItemSlotMap(Item... targetItem) {
        HashMap<Integer, ItemStack> map = new HashMap<>();

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (player == null || interactionManager == null || clientPlayNetworkHandler == null) {
            return map;
        }

        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.main.size(); i++) {
            ItemStack itemStack = playerInventory.main.get(i);
            for (Item item : targetItem) {
                if (itemStack.isOf(item)) {
                    map.put(i, itemStack);
                }
            }
        }
        return map;
    }

    public static void switchToItemSlot(int itemSlot) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (player == null || interactionManager == null || clientPlayNetworkHandler == null) {
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        if (PlayerInventory.isValidHotbarIndex(itemSlot)) {
            playerInventory.selectedSlot = itemSlot;
        } else {
            interactionManager.pickFromInventory(itemSlot);
        }
        clientPlayNetworkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot));
    }

    public static boolean isItemDamageWarning(ItemStack itemStack, int minDamage) {
        int damageMax = itemStack.getMaxDamage();       // 最大值耐久
        if (damageMax > 0) {
            int damage = itemStack.getDamage();         // 已使用耐久
            int damageSurplus = damageMax - damage;     // 剩余耐久
            return damageSurplus <= minDamage;
        }
        return false;
    }

    public static boolean switchToItem(Item targetItem) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (player == null || interactionManager == null || clientPlayNetworkHandler == null) {
            return false;
        }

        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.main.size(); i++) {
            ItemStack itemStack = playerInventory.main.get(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            // Debug.info();
            // Debug.info("[%s][物品堆]: %s", i, itemStack);
            // Debug.info("[%s][名称]: %s (%s)", i, itemStack.getTranslationKey(), itemStack.getName().getString());
            // Debug.info("[%s][NBT]: %s", i, itemStack.getNbt());
            // Debug.info("[%s][playerInventory.isValidHotbarIndex]: %s", i, PlayerInventory.isValidHotbarIndex(i));
            // Debug.info("[%s][playerInventory.selectedSlot]: %s", i, playerInventory.selectedSlot);

            if (itemStack.isOf(targetItem)) {
                // Debug.info("[%s][目标物品]: 是", i);

                if (PlayerInventory.isValidHotbarIndex(i)) {
                    playerInventory.selectedSlot = i;
                } else {
                    interactionManager.pickFromInventory(i);
                }
                clientPlayNetworkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot));
            }
        }

        return false;
    }

    /**
     * 是否可以瞬间破坏方块
     *
     * @param block     待破坏的方块
     * @param itemStack 使用什么物品堆来破坏方块
     * @return true为可以瞬间破坏
     */
    public static boolean isInstantBreakingBlock(Block block, ItemStack itemStack) {
        float hardness = block.getHardness();                       // 当前方块硬度
        if (hardness < 0) return false;                             // 无硬度(如基岩无法破坏)
        float speed = getBlockBreakingSpeed(block, itemStack);      // 当前破坏速度
        return speed > (hardness * 30);
    }

    /**
     * 获取方块破坏所需的总时间
     *
     * @param block     待破坏的方块
     * @param itemStack 使用什么物品堆来破坏方块
     * @return 时间 (秒)
     */
    public static float getBlockBreakingTotalTime(Block block, ItemStack itemStack) {
        float hardness = block.getHardness();                       // 当前方块硬度
        if (hardness < 0) return -1;
        float speed = getBlockBreakingSpeed(block, itemStack);      // 当前破坏速度
        return (float) ((hardness * 1.5) / speed);
    }

    /**
     * 获取方块破坏速度
     *
     * @param block     待破坏的方块
     * @param itemStack 使用什么物品堆来破坏方块
     * @return 破坏的速度(可以理解为每秒能破坏多少)
     */
    public static float getBlockBreakingSpeed(Block block, ItemStack itemStack) {
        // 使用的物品堆破坏方块时最小速度,最小值为1(当工具不同速度加成也不同,比1大)
        float f = itemStack.getMiningSpeedMultiplier(block.getDefaultState());
        if (f > 1.0F) {
            // 获取物品堆的效率附魔等级
            int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        PlayerEntity player = minecraftClient.player;
        if (player != null) {

            // 急迫Buff
            if (StatusEffectUtil.hasHaste(player)) {
                f *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
            }

            // 挖掘疲劳Buff
            if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.MINING_FATIGUE);
                if (statusEffect != null) {
                    float g = switch (statusEffect.getAmplifier()) {
                        case 0 -> 0.3F;
                        case 1 -> 0.09F;
                        case 2 -> 0.0027F;
                        default -> 8.1E-4F;
                    };
                    f *= g;
                }

            }

            // 头部是否在水中且,没有水下速掘Buff
            if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
                f /= 5.0F;
            }

            // 玩家在地面上
            if (!player.isOnGround()) {
                f /= 5.0F;
            }
        }

        return f;
    }
}
