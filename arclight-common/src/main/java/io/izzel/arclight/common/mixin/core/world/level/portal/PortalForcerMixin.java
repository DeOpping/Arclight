package io.izzel.arclight.common.mixin.core.world.level.portal;

import io.izzel.arclight.common.bridge.core.world.TeleporterBridge;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalForcer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = PortalForcer.class, priority = 1500)
public abstract class PortalForcerMixin implements TeleporterBridge {

    // @formatter:off
    @Shadow public abstract Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis);
    @Shadow @Final protected ServerLevel level;
    // @formatter:on

    private transient int arclight$searchRadius = -1;

    @Override
    public void bridge$pushSearchRadius(int searchRadius) {
        this.arclight$searchRadius = searchRadius;
    }

    @ModifyVariable(method = "findClosestPortalPosition", ordinal = 0, at = @At(value = "STORE", ordinal = 0))
    private int arclight$useSearchRadius(int i) {
        return this.arclight$searchRadius == -1 ? i : this.arclight$searchRadius;
    }

    @ModifyArg(method = "createPortal", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;spiralAround(Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Ljava/lang/Iterable;"))
    private int arclight$changeRadius(int i) {
        return this.arclight$createRadius == -1 ? i : this.arclight$createRadius;
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$captureBlocks1(ServerLevel serverWorld, BlockPos pos, BlockState state) {
        if (this.arclight$populator == null) {
            this.arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.arclight$populator.setBlock(pos, state, 3);
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$captureBlocks2(ServerLevel serverWorld, BlockPos pos, BlockState state, int flags) {
        if (this.arclight$populator == null) {
            this.arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.arclight$populator.setBlock(pos, state, flags);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "createPortal", cancellable = true, at = @At("RETURN"))
    private void arclight$portalCreate(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        CraftWorld craftWorld = this.level.bridge$getWorld();
        List<org.bukkit.block.BlockState> blockStates;
        if (this.arclight$populator == null) {
            blockStates = new ArrayList<>();
        } else {
            blockStates = (List) this.arclight$populator.getList();
        }
        PortalCreateEvent event = new PortalCreateEvent(blockStates, craftWorld, (this.arclight$entity == null) ? null : this.arclight$entity.bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.NETHER_PAIR);

        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        if (this.arclight$populator != null) {
            this.arclight$populator.updateList();
        }
    }

    private transient BlockStateListPopulator arclight$populator;
    private transient Entity arclight$entity;
    private transient int arclight$createRadius = -1;

    @Override
    public void bridge$pushPortalCreate(Entity entity, int createRadius) {
        this.arclight$entity = entity;
        this.arclight$createRadius = createRadius;
    }
}
