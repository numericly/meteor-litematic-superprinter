package net.numericly.superprinter.modules;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;
import net.numericly.superprinter.SuperPrinter;
import net.numericly.superprinter.utils.InventoryManager;
import net.numericly.superprinter.utils.Utils;
import net.numericly.superprinter.utils.tasks.Task;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.numericly.superprinter.utils.tasks.Tasks;

public class ModulePrinter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

    private final Setting<Double> bpt = sgGeneral.add(new DoubleSetting.Builder()
        .name("blocks/tick")
        .description("How many blocks place per tick. (1.5 is paper max)")
        .defaultValue(1.5)
        .min(0)
        .sliderRange(0.1, 2)
        .build()
    );

    public final Setting<Integer> inventoryWaitTime = sgGeneral.add(new IntSetting.Builder()
        .name("inventory-wait-time")
        .description("Time to wait for inventory updates in ms.")
        .defaultValue(300)
        .min(0)
        .sliderRange(0, 1000)
        .onModuleActivated(setting -> InventoryManager.inventoryWaitTime = setting.get())
        .onChanged(value -> InventoryManager.inventoryWaitTime = value)
        .build()
    );

    public final Setting<Boolean> autoDupe = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-dupes")
        .description("Automatically dupes stack if at less than half.")
        .defaultValue(true)
        .onModuleActivated(setting -> InventoryManager.autoDupe = setting.get())
        .onChanged(value -> InventoryManager.autoDupe = value)
        .build()
    );

    public final Setting<String> dupeCommand = sgGeneral.add(new StringSetting.Builder()
        .name("dupe-command")
        .description("The dupe command to double the lowest stack of your held item.")
        .defaultValue("dupe")
        .visible(autoDupe::get)
        .build()
    );

    public final Setting<Integer> dupeWaitTime = sgGeneral.add(new IntSetting.Builder()
        .name("dupe-wait-time")
        .description("Time to wait for auto dupe to complete in ms.")
        .defaultValue(200)
        .min(0)
        .sliderRange(0, 1000)
        .visible(autoDupe::get)
        .build()
    );

    private final Setting<Boolean> renderBlocks = sgRendering.add(new BoolSetting.Builder()
        .name("render-placed-blocks")
        .description("Renders block placements.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time for the rendering to fade, in milliseconds.")
        .defaultValue(400)
        .sliderRange(100, 3000)
        .min(0)
        .visible(renderBlocks::get)
        .build()
    );

    private boolean dupeNext = false;
    private long dupedAt = 0;

    private final List<Task> tasks = new ArrayList<>();

    record CompletedTask(Long time, BlockPos location) {}

    private final List<CompletedTask> completedTasks = new ArrayList<>();

    public ModulePrinter() {
        super(SuperPrinter.CATEGORY, "printer", "Prints litematica schematics.");
    }

    @Override
    public void onActivate() {
        completedTasks.clear();
    }

    double extraBlock = 0.0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (dupeNext) {
            dupedAt = System.currentTimeMillis();
            mc.getNetworkHandler().sendChatCommand(dupeCommand.get());
            dupeNext = false;
        }

        completedTasks.removeIf(s -> System.currentTimeMillis() - s.time() > fadeTime.get());

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (worldSchematic == null) {
            completedTasks.clear();
            toggle();
            return;
        }

        BlockIterator.register(6, 6, (pos, blockState) -> {
            BlockState required = worldSchematic.getBlockState(pos);
            BlockState current = mc.world.getBlockState(pos);

            if (
                Utils.isWithinBlockInteractionRange(pos) &&
                !required.isAir() &&
                DataManager.getRenderLayerRange().isPositionWithinRange(pos)
            ) {
                Task task = Tasks.getOrCreateTask(required, current, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
                if (task != null) {
                    tasks.add(task);
                }
            }
        });

        BlockIterator.after(() -> {
            tasks.sort(Task.NEAREST);

            int blocksAllowed = (int) Math.floor(bpt.get());

            if (extraBlock >= 1 && tasks.size() > blocksAllowed) {
                blocksAllowed++;
                extraBlock = 0.0F;
            }

            long sinceDuped = System.currentTimeMillis() - dupedAt;

            if (autoDupe.get() && mc.player.getGameMode() != GameMode.CREATIVE && sinceDuped > dupeWaitTime.get()) {
                ItemStack stack = mc.player.getMainHandStack();
                int halfStackSize = stack.getItem().getMaxCount() / 2;

                if (stack.getCount() <= halfStackSize) {
                    dupeNext = true;

                    return;
                }
            }

            int executed = 0;

            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.removeFirst();

                if (executed >= blocksAllowed) {
                    break;
                }

                if (task.execute()) {
                    completedTasks.add(new CompletedTask(System.currentTimeMillis(), task.getLocation()));
                    executed++;
                }

            }

            tasks.clear();

            extraBlock += bpt.get() % 1;
        });
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderBlocks.get()) {
            completedTasks.forEach(s -> {
                long elapsed = System.currentTimeMillis() - s.time();
                double fadeAmount = Math.max(0, 1 - (double) elapsed / (double) fadeTime.get());

                event.renderer.box(s.location(), fade(sideColor.get(), fadeAmount), fade(lineColor.get(), fadeAmount), shapeMode.get(), 0);
            });
        }
    }

    private Color fade(Color color, double amount) {
        Color newColor = color.copy();

        newColor.a((int) ((double) color.a * amount));

        return newColor;
    }
}
