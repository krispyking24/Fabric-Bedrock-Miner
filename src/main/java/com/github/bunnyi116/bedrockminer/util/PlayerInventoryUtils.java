package com.github.bunnyi116.bedrockminer.util;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;

public class PlayerInventoryUtils {
    public static DefaultedList<ItemStack> getMainStacks(PlayerInventory playerInventory){
        //#if MC > 12104
        return playerInventory.getMainStacks();
        //#else
        //$$ return playerInventory.main;
        //#endif
    }

    public static int getSelectedSlot() {
        //#if MC > 12104
        return player.getInventory().getSelectedSlot();
        //#else
        //$$ return player.getInventory().selectedSlot;
        //#endif
    }

    public static void setSelectedSlot(int slot) {
        //#if MC > 12104
        player.getInventory().setSelectedSlot(slot);
        //#else
        //$$ player.getInventory().selectedSlot = slot;
        //#endif
    }
}
