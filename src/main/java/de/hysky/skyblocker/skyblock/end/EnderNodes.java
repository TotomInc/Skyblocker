package de.hysky.skyblocker.skyblock.end;

import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.events.ParticleEvents;
import de.hysky.skyblocker.utils.Utils;
import de.hysky.skyblocker.utils.scheduler.Scheduler;
import de.hysky.skyblocker.utils.waypoint.Waypoint;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.block.Blocks;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EnderNodes {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Map<BlockPos, EnderNode> enderNodes = new HashMap<>();
    // Remove EnderNodes if no particles detected for 10 seconds
    private static final long PARTICLE_TIMEOUT_MS = 10_000;

    @Init
    public static void init() {
        // Check every 5 ticks (0.25 seconds) for faster detection
        Scheduler.INSTANCE.scheduleCyclic(EnderNodes::update, 5);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(EnderNodes::render);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            enderNodes.remove(pos);
            return ActionResult.PASS;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ParticleEvents.FROM_SERVER.register(EnderNodes::onParticle);
    }

    private static void onParticle(ParticleS2CPacket packet) {
        if (!shouldProcess()) return;
        ParticleType<?> particleType = packet.getParameters().getType();
        if (!ParticleTypes.PORTAL.getType().equals(particleType) && !ParticleTypes.WITCH.getType().equals(particleType))
            return;

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        double xFrac = MathHelper.floorMod(x, 1);
        double yFrac = MathHelper.floorMod(y, 1);
        double zFrac = MathHelper.floorMod(z, 1);
        BlockPos pos;
        Direction direction;
        if (yFrac == 0.25) {
            pos = BlockPos.ofFloored(x, y - 1, z);
            direction = Direction.UP;
        } else if (yFrac == 0.75) {
            pos = BlockPos.ofFloored(x, y + 1, z);
            direction = Direction.DOWN;
        } else if (xFrac == 0.25) {
            pos = BlockPos.ofFloored(x - 1, y, z);
            direction = Direction.EAST;
        } else if (xFrac == 0.75) {
            pos = BlockPos.ofFloored(x + 1, y, z);
            direction = Direction.WEST;
        } else if (zFrac == 0.25) {
            pos = BlockPos.ofFloored(x, y, z - 1);
            direction = Direction.SOUTH;
        } else if (zFrac == 0.75) {
            pos = BlockPos.ofFloored(x, y, z + 1);
            direction = Direction.NORTH;
        } else {
            return;
        }

        EnderNode enderNode = enderNodes.computeIfAbsent(pos, EnderNode::new);
        IntIntPair particles = enderNode.particles.get(direction);
        if (ParticleTypes.PORTAL.getType().equals(particleType)) {
            particles.left(particles.leftInt() + 1);
        } else if (ParticleTypes.WITCH.getType().equals(particleType)) {
            particles.right(particles.rightInt() + 1);
        }
        // Update the last seen timestamp whenever particles are detected
        enderNode.lastParticleSeen = System.currentTimeMillis();
    }

    private static void update() {
        if (shouldProcess() && client.world != null) {
            long currentTimeMillis = System.currentTimeMillis();
            // Use iterator to safely remove nodes while iterating
            Iterator<Map.Entry<BlockPos, EnderNode>> iterator = enderNodes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, EnderNode> entry = iterator.next();
                EnderNode enderNode = entry.getValue();
                BlockPos pos = entry.getKey();

                // Remove if the block is bedrock (mined by someone)
                if (client.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                    iterator.remove();
                    continue;
                }

                // Failsafe: Remove if no particles detected for the timeout period
                // This ensures stale nodes are cleaned up even if bedrock check misses them
                if (enderNode.isConfirmed && currentTimeMillis - enderNode.lastParticleSeen > PARTICLE_TIMEOUT_MS) {
                    iterator.remove();
                    continue;
                }

                enderNode.updateWaypoint();
            }
        }
    }

    private static void render(WorldRenderContext context) {
        if (shouldProcess()) {
            for (EnderNode enderNode : enderNodes.values()) {
                enderNode.render(context);
            }
        }
    }

    private static boolean shouldProcess() {
        return SkyblockerConfigManager.get().otherLocations.end.enableEnderNodeHelper && Utils.isInTheEnd();
    }

    private static void reset() {
        enderNodes.clear();
    }

    public static class EnderNode extends Waypoint {
        private static final float LINE_WIDTH = 1.0f;

        private final Map<Direction, IntIntPair> particles = Map.of(
                Direction.UP, new IntIntMutablePair(0, 0),
                Direction.DOWN, new IntIntMutablePair(0, 0),
                Direction.EAST, new IntIntMutablePair(0, 0),
                Direction.WEST, new IntIntMutablePair(0, 0),
                Direction.SOUTH, new IntIntMutablePair(0, 0),
                Direction.NORTH, new IntIntMutablePair(0, 0)
        );
        private long lastConfirmed;
        private long lastParticleSeen;
        private boolean isConfirmed = false;

        private EnderNode(BlockPos pos) {
            super(pos, () -> SkyblockerConfigManager.get().otherLocations.end.enderNodeWaypointType,
                  getColorComponents(), getAlpha(), LINE_WIDTH, true);
            this.lastParticleSeen = System.currentTimeMillis();
        }

        private static float[] getColorComponents() {
            java.awt.Color configColor = SkyblockerConfigManager.get().otherLocations.end.enderNodeColor;
            return new float[]{
                configColor.getRed() / 255f,
                configColor.getGreen() / 255f,
                configColor.getBlue() / 255f
            };
        }

        private static float getAlpha() {
            return SkyblockerConfigManager.get().otherLocations.end.enderNodeColor.getAlpha() / 255f;
        }

        private void updateWaypoint() {
            long currentTimeMillis = System.currentTimeMillis();

            // More lenient detection: only need 5 particles of each type per direction
            // Check every 500ms to confirm nodes faster
            if (isConfirmed || lastConfirmed + 500 > currentTimeMillis || client.world == null) return;

            // Check if we have enough particles on at least one direction to confirm this is an ender node
            boolean hasEnoughParticles = particles.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().leftInt() >= 5 && entry.getValue().rightInt() >= 5);

            if (hasEnoughParticles) {
                lastConfirmed = currentTimeMillis;
                isConfirmed = true;
                // Reset particle counters
                for (Map.Entry<Direction, IntIntPair> entry : particles.entrySet()) {
                    entry.getValue().left(0);
                    entry.getValue().right(0);
                }
            }
        }

        @Override
        public boolean shouldRender() {
            // Render as long as it's enabled and confirmed
            // No time limit - will render until the block is mined
            return isEnabled() && isConfirmed;
        }

        @Override
        public boolean shouldRenderThroughWalls() {
            // Always render through walls
            return true;
        }
    }
}
