package net.numericly.superprinter.utils;

import net.minecraft.client.input.Input;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Utils {
    public static void setSneaking(boolean sneaking) {
        assert mc.player != null;

        PlayerInput input = mc.player.input.playerInput;

        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(
            new PlayerInput(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.jump(),
                sneaking,
                input.sprint()
            )
        ));
    }

    public static boolean isWithinBlockInteractionRange(BlockPos pos) {
        double d = 4.5 + 1.0;
        return new Box(pos).squaredMagnitude(mc.player.getEyePos()) < d * d;
    }
}
