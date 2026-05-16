package net.numericly.superprinter.utils.tasks;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlaceContextNoRotation extends ItemPlacementContext {

    private final boolean isSneaking;

    protected PlaceContextNoRotation(World level, Hand interactionHand, ItemStack itemStack, BlockHitResult blockHitResult) {
        super(level, null, interactionHand, itemStack, blockHitResult);

        assert mc.player != null;

        this.isSneaking = mc.player.isSneaking();
    }

    @Override
    public @NotNull Direction getPlayerLookDirection() {
        throw new RuntimeException();
    }

    @Override
    public @NotNull Direction getVerticalPlayerLookDirection() {
        throw new RuntimeException();
    }

    @Override
    public Direction @NotNull [] getPlacementDirections() {
        throw new RuntimeException();
    }

    @Override
    public @NotNull Direction getHorizontalPlayerFacing() {
        throw new RuntimeException();
    }

    @Override
    public boolean shouldCancelInteraction() {
        return isSneaking;
    }

    @Override
    public float getPlayerYaw() {
        throw new RuntimeException();
    }
}
