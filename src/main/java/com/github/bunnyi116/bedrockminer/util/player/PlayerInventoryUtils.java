package com.github.bunnyi116.bedrockminer.util.player;


import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;

public class PlayerInventoryUtils {
    public static NonNullList<ItemStack> getMainStacks(Inventory playerInventory){
        //#if MC > 12104
        return playerInventory.getNonEquipmentItems();
        //#else
        //$$ return playerInventory.items;
        //#endif
    }

    public static int getSelectedSlot() {
        //#if MC > 12104
        return player.getInventory().getSelectedSlot();
        //#else
        //$$ return player.getInventory().selected;
        //#endif
    }

    public static void setSelectedSlot(int slot) {
        //#if MC > 12104
        player.getInventory().setSelectedSlot(slot);
        //#else
        //$$ player.getInventory().selected = slot;
        //#endif
    }
}
