package net.numericly.superprinter.utils.tasks;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static net.minecraft.util.math.Direction.*;

public class PlaceContextRotation extends ItemPlacementContext {

    private final boolean sneaking;
    private final float lookYaw;
    private final float lookPitch;

    protected PlaceContextRotation(World level, float lookYaw, float lookPitch, Hand interactionHand, ItemStack itemStack, BlockHitResult blockHitResult) {
        super(level, null, interactionHand, itemStack, blockHitResult);
        this.lookYaw = lookYaw;
        this.lookPitch = lookPitch;

        assert mc.player != null;

        this.sneaking = mc.player.isSneaking();
    }

    public float getLookYaw() {
        return lookYaw;
    }

    public float getLookPitch() {
        return lookPitch;
    }

    @Override
    public Direction getPlayerLookDirection() {
        return getEntityFacingOrder(lookPitch, lookYaw)[0];
    }

    @Override
    public Direction getVerticalPlayerLookDirection() {
        return getLookDirectionForAxis(lookPitch, lookYaw, Direction.Axis.Y);
    }

    @Override
    public Direction[] getPlacementDirections() {
        Direction[] directions = getEntityFacingOrder(lookPitch, lookYaw);
        if (this.canReplaceExisting) {
            return directions;
        } else {
            Direction direction = this.getSide();
            int i = 0;

            while (i < directions.length && directions[i] != direction.getOpposite()) {
                i++;
            }

            if (i > 0) {
                System.arraycopy(directions, 0, directions, 1, i);
                directions[0] = direction.getOpposite();
            }

            return directions;
        }
    }

    @Override
    public Direction getHorizontalPlayerFacing() {
        return Direction.fromHorizontalDegrees(lookYaw);
    }

    @Override
    public boolean shouldCancelInteraction() {
        return sneaking;
    }

    @Override
    public float getPlayerYaw() {
        return lookYaw;
    }

    public static Direction getLookDirectionForAxis(float pitch, float yaw, Direction.Axis axis) {
        return switch (axis) {
            case X -> EAST.pointsTo(yaw) ? EAST : WEST;
            case Y -> pitch < 0.0F ? UP : DOWN;
            case Z -> SOUTH.pointsTo(yaw) ? SOUTH : NORTH;
        };
    }

    public static Direction[] getEntityFacingOrder(float pitch, float yaw) {
        float f = pitch * (float) (Math.PI / 180.0);
        float g = -yaw * (float) (Math.PI / 180.0);
        float h = MathHelper.sin(f);
        float i = MathHelper.cos(f);
        float j = MathHelper.sin(g);
        float k = MathHelper.cos(g);
        boolean bl = j > 0.0F;
        boolean bl2 = h < 0.0F;
        boolean bl3 = k > 0.0F;
        float l = bl ? j : -j;
        float m = bl2 ? -h : h;
        float n = bl3 ? k : -k;
        float o = l * i;
        float p = n * i;
        Direction direction = bl ? EAST : WEST;
        Direction direction2 = bl2 ? UP : DOWN;
        Direction direction3 = bl3 ? SOUTH : NORTH;
        if (l > n) {
            if (m > o) {
                return listClosest(direction2, direction, direction3);
            } else {
                return p > m ? listClosest(direction, direction3, direction2) : listClosest(direction, direction2, direction3);
            }
        } else if (m > p) {
            return listClosest(direction2, direction3, direction);
        } else {
            return o > m ? listClosest(direction3, direction, direction2) : listClosest(direction3, direction2, direction);
        }
    }

    /**
     * Helper function that returns the 3 directions given, followed by the 3 opposite given in opposite order.
     */
    private static Direction[] listClosest(Direction first, Direction second, Direction third) {
        return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
    }
}
