package net.blay09.mods.cookingforblockheads.tile;

import com.google.common.collect.Lists;
import net.blay09.mods.balm.api.block.entity.BalmBlockEntity;
import net.blay09.mods.balm.api.block.entity.CustomRenderBoundingBox;
import net.blay09.mods.balm.api.container.BalmContainerProvider;
import net.blay09.mods.balm.api.container.DefaultContainer;
import net.blay09.mods.balm.api.menu.BalmMenuProvider;
import net.blay09.mods.balm.api.provider.BalmProvider;
import net.blay09.mods.cookingforblockheads.CookingForBlockheadsConfig;
import net.blay09.mods.cookingforblockheads.api.capability.DefaultKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.api.capability.IKitchenItemProvider;
import net.blay09.mods.cookingforblockheads.block.CounterBlock;
import net.blay09.mods.cookingforblockheads.menu.CounterMenu;
import net.blay09.mods.cookingforblockheads.tile.util.DoorAnimator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CounterBlockEntity extends BalmBlockEntity implements BalmMenuProvider, IMutableNameable, BalmContainerProvider, CustomRenderBoundingBox {

    private final int containerSize = CookingForBlockheadsConfig.getActive().largeCounters ? 54 : 27;

    private final DefaultContainer container = new DefaultContainer(containerSize) {
        @Override
        public void setChanged() {
            CounterBlockEntity.this.setChanged();
        }
    };

    private final DefaultKitchenItemProvider itemProvider = new DefaultKitchenItemProvider(container);
    private final DoorAnimator doorAnimator = new DoorAnimator(this, 1, 2);

    private Component customName;

    private boolean isDirty;

    private DyeColor color = DyeColor.WHITE;

    public CounterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.counter.get(), pos, state);
    }

    public CounterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        doorAnimator.setOpenRadius(2);
        doorAnimator.setSoundEventOpen(SoundEvents.CHEST_OPEN);
        doorAnimator.setSoundEventClose(SoundEvents.CHEST_CLOSE);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CounterBlockEntity blockEntity) {
        blockEntity.clientTick(level, pos, state);
    }

    public void clientTick(Level level, BlockPos pos, BlockState state) {
        doorAnimator.update();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CounterBlockEntity blockEntity) {
        blockEntity.serverTick(level, pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (isDirty) {
            sync();
            isDirty = false;
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        return doorAnimator.receiveClientEvent(id, type) || super.triggerEvent(id, type);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag itemHandlerCompound = tag.getCompound("ItemHandler");
        if (CookingForBlockheadsConfig.getActive().largeCounters && itemHandlerCompound.getInt("Size") < 54) {
            itemHandlerCompound.putInt("Size", 54);
        }

        container.deserialize(itemHandlerCompound);

        color = DyeColor.byId(tag.getByte("Color"));

        if (tag.contains("CustomName", Tag.TAG_STRING)) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        }

        if (tag.contains("IsForcedOpen", Tag.TAG_BYTE)) {
            doorAnimator.setForcedOpen(tag.getBoolean("IsForcedOpen"));
        }

        if (tag.contains("NumPlayersUsing", Tag.TAG_BYTE)) {
            doorAnimator.setNumPlayersUsing(tag.getByte("NumPlayersUsing"));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put("ItemHandler", container.serialize());
        tag.putByte("Color", (byte) color.getId());

        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        }
    }

    @Override
    public void writeUpdateTag(CompoundTag tag) {
        saveAdditional(tag);
        tag.putBoolean("IsForcedOpen", doorAnimator.isForcedOpen());
        tag.putByte("NumPlayersUsing", (byte) doorAnimator.getNumPlayersUsing());
    }

    @Override
    public List<BalmProvider<?>> getProviders() {
        return Lists.newArrayList(new BalmProvider<>(IKitchenItemProvider.class, itemProvider));
    }

    public DoorAnimator getDoorAnimator() {
        return doorAnimator;
    }

    public Direction getFacing() {
        BlockState blockState = getBlockState();
        return blockState.hasProperty(BlockStateProperties.FACING) ? blockState.getValue(BlockStateProperties.FACING) : Direction.NORTH;
    }

    public boolean isFlipped() {
        BlockState blockState = getBlockState();
        return blockState.hasProperty(CounterBlock.FLIPPED) && blockState.getValue(CounterBlock.FLIPPED);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new CounterMenu(i, playerInventory, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 1, 2));
    }

    @Override
    public Component getName() {
        return customName != null ? customName : getDefaultName();
    }

    @Override
    public void setCustomName(Component customName) {
        this.customName = customName;
        setChanged();
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return customName;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public Component getDefaultName() {
        return new TranslatableComponent("container.cookingforblockheads.counter");
    }

    @Override
    public void setChanged() {
        isDirty = true;
        super.setChanged();
    }

    @Override
    public Container getContainer() {
        return container;
    }
}
