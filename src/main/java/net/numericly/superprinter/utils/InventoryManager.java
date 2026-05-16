package net.numericly.superprinter.utils;

import net.numericly.superprinter.modules.ModulePrinter;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryManager {

    public static boolean autoDupe;
    public static long inventoryWaitTime;

    static Map<Integer, Long> slotUpdates = new HashMap<>();

    static long[] lastUsed = new long[9];

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(InventoryManager.class);
    }

    public static boolean switchItem(Item item) {
        assert mc.player != null;

        if (mc.player.getGameMode() == GameMode.CREATIVE) {
            ItemStack stack = new ItemStack(item, 1);

            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), stack));
            mc.player.playerScreenHandler.getSlot(36 + mc.player.getInventory().getSelectedSlot()).setStack(stack);

            return true;
        }

        int selectedSlot = mc.player.getInventory().getSelectedSlot();

        if (mc.player.getInventory().getStack(selectedSlot).getItem() == item) {
            if (getSlotUpdate(selectedSlot)) {
                return false;
            }

            lastUsed[selectedSlot] = System.currentTimeMillis();

            return dupeCheck(selectedSlot);
        }

        FindItemResult hotbarResult = InvUtils.findInHotbar(item);

        if (hotbarResult.found()) {
            if (getSlotUpdate(hotbarResult.slot())) {
                return false;
            }

            mc.player.getInventory().setSelectedSlot(hotbarResult.slot());

            lastUsed[hotbarResult.slot()] = System.currentTimeMillis();

            return dupeCheck(hotbarResult.slot());
        }


        FindItemResult result = InvUtils.find(item);

        if (result.found()) {
            if (getSlotUpdate(result.slot())) {
                return false;
            }

            int to = findAvailableSlot();

            if (to == -1) {
                return false;
            }

            int from = result.slot();

            InvUtils.quickSwap().fromId(to).to(from);

            setSlotUpdate(to);
            setSlotUpdate(from);
        }

        return false;
    }

    private static boolean dupeCheck(int slot) {
        assert mc.player != null;

        if (!autoDupe) {
            return true;
        }

        ItemStack stack = mc.player.getInventory().getStack(slot);
        int halfStackSize = stack.getItem().getMaxCount() / 4;

        return stack.getCount() > halfStackSize;
    }

    private static int findAvailableSlot() {
        int best = -1;
        long bestTime = System.currentTimeMillis();

        for (int i = 1; i < lastUsed.length; i++) {
            if (lastUsed[i] < bestTime && !getSlotUpdate(i)) {
                bestTime = lastUsed[i];
                best = i;
            }
        }

        return best;
    }

    public static void setSlotUpdate(Integer v) {
        slotUpdates.put(v, System.currentTimeMillis());
    }


    public static boolean getSlotUpdate(Integer v) {
        Long value = slotUpdates.get(v);

        if (value == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - value;

        if (elapsed > inventoryWaitTime) {
            slotUpdates.remove(v);
            return false;
        } else {
            return true;
        }
    }
}
