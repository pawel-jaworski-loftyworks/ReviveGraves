package de.programmierin.revivegraves.entity;

import de.programmierin.revivegraves.ReviveGraves;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class GravestoneBlockEntity extends BlockEntity {
    private UUID owner;
    private String ownerName;
    private UUID hologram;
    private GameMode originalGameMode;
    private String skinTextureValue;
    private String skinTextureSignature;
    private long creationTick = -1;
    private List<StackWithSlot> storedItems = new ArrayList<>();
    private int storedXp = 0;

    public GravestoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVESTONE, pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markDirty();
        if (getWorld() != null && !getWorld().isClient()) {
            getWorld().updateListeners(getPos(), getCachedState(), getCachedState(), 3);
        }
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName != null && ownerName.length() > 16
                ? ownerName.substring(0, 16) : ownerName;
        markDirty();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public UUID getOwner() {
        return owner;
    }

    public UUID getHologram() {
        return hologram;
    }

    public void discardHologram(ServerWorld world) {
        if (hologram == null) return;
        Entity holo = world.getEntity(hologram);
        if (holo != null) holo.discard();
    }

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }

    public void setOriginalGameMode(GameMode mode) {
        this.originalGameMode = mode;
        markDirty();
    }

    public void setSkinTexture(String value, String signature) {
        this.skinTextureValue = value;
        this.skinTextureSignature = signature;
        markDirty();
    }

    public String getSkinTextureValue() {
        return skinTextureValue;
    }

    public String getSkinTextureSignature() {
        return skinTextureSignature;
    }

    public long getCreationTick() {
        return creationTick;
    }

    public void setCreationTick(long tick) {
        this.creationTick = tick;
        markDirty();
    }

    public List<StackWithSlot> getStoredItems() {
        return storedItems;
    }

    public void setStoredItems(List<StackWithSlot> items) {
        this.storedItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        markDirty();
    }

    public int getStoredXp() {
        return storedXp;
    }

    public void setStoredXp(int xp) {
        this.storedXp = xp;
        markDirty();
    }

    public void spawnHologram(ServerWorld world) {
        if (owner == null || hologram != null) return;

        ServerPlayerEntity player = world.getServer()
                .getPlayerManager()
                .getPlayer(owner);
        if (player == null) return;
        String name = player.getGameProfile().name();

        NbtCompound tag = new NbtCompound();
        tag.putString("id", "minecraft:armor_stand");
        tag.putBoolean("Invisible", true);
        tag.putBoolean("NoGravity", true);
        tag.putBoolean("Invulnerable", true);
        tag.putBoolean("Small", true);
        tag.putBoolean("NoBasePlate", true);
        tag.putBoolean("Marker", true);

        Entity loaded = EntityType.loadEntityWithPassengers(tag, world, SpawnReason.TRIGGERED, Function.identity());
        if (!(loaded instanceof ArmorStandEntity stand)) return;

        stand.setCustomName(Text.literal(name));
        stand.setCustomNameVisible(true);

        double x = getPos().getX() + 0.5;
        double y = getPos().getY() + 0.8;
        double z = getPos().getZ() + 0.5;
        stand.refreshPositionAndAngles(x, y, z, 0f, 0f);

        world.spawnEntity(stand);
        this.hologram = stand.getUuid();
        markDirty();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    protected void writeData(net.minecraft.storage.WriteView view) {
        super.writeData(view);
        if (owner != null) {
            view.putString("Owner", owner.toString());
        }
        if (ownerName != null) {
            view.putString("OwnerName", ownerName);
        }
        if (hologram != null) {
            view.putString("Hologram", hologram.toString());
        }
        if (originalGameMode != null) {
            view.putString("OriginalGameMode", originalGameMode.name());
        }
        if (skinTextureValue != null) {
            view.putString("SkinTexture", skinTextureValue);
        }
        if (skinTextureSignature != null) {
            view.putString("SkinSignature", skinTextureSignature);
        }
        if (creationTick >= 0) {
            view.putString("CreationTick", String.valueOf(creationTick));
        }
        if (!storedItems.isEmpty()) {
            net.minecraft.storage.WriteView.ListAppender<StackWithSlot> list =
                    view.getListAppender("StoredItems", StackWithSlot.CODEC);
            for (StackWithSlot item : storedItems) {
                list.add(item);
            }
        }
        if (storedXp > 0) {
            view.putInt("StoredXp", storedXp);
        }
    }

    @Override
    protected void readData(net.minecraft.storage.ReadView view) {
        super.readData(view);
        view.getOptionalString("Owner").ifPresent(u -> {
            try {
                this.owner = UUID.fromString(u);
            } catch (IllegalArgumentException e) {
                ReviveGraves.LOGGER.warn("Invalid Owner UUID in gravestone NBT: {}", u);
            }
        });
        view.getOptionalString("OwnerName").ifPresent(n ->
            this.ownerName = n.length() > 16 ? n.substring(0, 16) : n
        );
        view.getOptionalString("Hologram").ifPresent(u -> {
            try {
                this.hologram = UUID.fromString(u);
            } catch (IllegalArgumentException e) {
                ReviveGraves.LOGGER.warn("Invalid Hologram UUID in gravestone NBT: {}", u);
            }
        });
        view.getOptionalString("OriginalGameMode").ifPresent(g -> {
            try {
                this.originalGameMode = GameMode.valueOf(g);
            } catch (IllegalArgumentException e) {
                ReviveGraves.LOGGER.warn("Invalid GameMode in gravestone NBT: {}", g);
            }
        });
        view.getOptionalString("SkinTexture").ifPresent(v -> this.skinTextureValue = v);
        view.getOptionalString("SkinSignature").ifPresent(v -> this.skinTextureSignature = v);
        view.getOptionalString("CreationTick").ifPresent(t -> {
            try {
                this.creationTick = Long.parseLong(t);
            } catch (NumberFormatException e) {
                ReviveGraves.LOGGER.warn("Invalid CreationTick in gravestone NBT: {}", t);
            }
        });
        view.getOptionalTypedListView("StoredItems", StackWithSlot.CODEC).ifPresent(list -> {
            this.storedItems = new ArrayList<>();
            for (StackWithSlot item : list) {
                this.storedItems.add(item);
            }
        });
        this.storedXp = view.getInt("StoredXp", 0);
    }
}
