package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.FireBlockBridge;
import io.izzel.arclight.common.mod.mixins.annotation.Inline;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin extends BaseFireBlockMixin implements FireBlockBridge {

    // @formatter:off
    @Shadow protected abstract BlockState getStateForPlacement(BlockGetter blockReader, BlockPos pos);
    @Shadow @Final private Object2IntMap<net.minecraft.world.level.block.Block> burnOdds;
    // @formatter:on

    @Inline
    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$fireSpread(ServerLevel world, BlockPos mutablePos, BlockState newState, int flags,
                                       BlockState state, ServerLevel worldIn, BlockPos pos) {
        if (world.getBlockState(mutablePos).getBlock() != Blocks.FIRE) {
            if (!CraftEventFactory.callBlockIgniteEvent(world, mutablePos, pos).isCancelled()) {
                return CraftEventFactory.handleBlockSpreadEvent(world, pos, mutablePos, newState, flags);
            }
        }
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean arclight$extinguish1(ServerLevel world, BlockPos pos, boolean isMoving) {
        if (!CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.defaultBlockState()).isCancelled()) {
            world.removeBlock(pos, isMoving);
        }
        return false;
    }

    @Inject(method = "checkBurnOut", require = 0, cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$blockBurn(Level worldIn, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        Block theBlock = CraftBlock.at(worldIn, pos);
        Block sourceBlock = CraftBlock.at(worldIn, ArclightCaptures.getTickingPosition());
        BlockBurnEvent event = new BlockBurnEvent(theBlock, sourceBlock);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (worldIn.getBlockState(pos).getBlock() instanceof TntBlock && !CraftEventFactory.callTNTPrimeEvent(worldIn, pos, TNTPrimeEvent.PrimeCause.FIRE, null, ArclightCaptures.getTickingPosition())) {
            ci.cancel();
        }
    }

    @Redirect(method = "updateShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState arclight$blockFade(net.minecraft.world.level.block.Block block, BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (!(worldIn instanceof Level)) {
            return Blocks.AIR.defaultBlockState();
        }
        CraftBlockState blockState = CraftBlockStates.getBlockState(worldIn, currentPos);
        blockState.setData(Blocks.AIR.defaultBlockState());
        BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return this.getStateForPlacement(worldIn, currentPos).setValue(FireBlock.AGE, stateIn.getValue(FireBlock.AGE));
        } else {
            return blockState.getHandle();
        }
    }

    @Override
    public boolean bridge$canBurn(net.minecraft.world.level.block.Block block) {
        return this.burnOdds.containsKey(block);
    }
}
