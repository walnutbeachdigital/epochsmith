package com.walnutbeachdigital.epochsmith.information;

import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.block.WorkpieceHolder;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Crafts on its own. Teach it a recipe by sneak-right-clicking with the item you want,
 * then feed it ingredients (by hand, hopper or pipe). Each craft costs 320 energy,
 * at up to 64 energy per tick, so about four crafts a second on full power.
 */
public class AssemblerBlockEntity extends BlockEntity implements WorkpieceHolder, StatusDescriber {

    public static final int SLOTS = 9;
    public static final int ENERGY_PER_CRAFT = 320;
    public static final int ENERGY_PER_TICK = 64;
    private static final int REFRESH_INTERVAL = 20;

    @Nullable
    private ResourceLocation recipeId = null;
    @Nullable
    private RecipeHolder<CraftingRecipe> recipe = null;
    /** What the programmed recipe makes, shown on top of the machine. */
    private ItemStack target = ItemStack.EMPTY;

    private final ItemStackHandler ingredients = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isIngredient(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler outputs = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Hoppers and pipes: insert ingredients, extract results. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS * 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < SLOTS ? ingredients.getStackInSlot(slot) : outputs.getStackInSlot(slot - SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < SLOTS ? ingredients.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < SLOTS ? ItemStack.EMPTY : outputs.extractItem(slot - SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < SLOTS && isIngredient(stack);
        }
    };

    private int progress = 0;
    private List<BlockPos> sources = List.of();

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLER.get(), pos, state);
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    @Override
    public ItemStack getWorkpiece() {
        return target;
    }

    // ---- Programming

    /** Every crafting recipe that makes this item, in a fixed order. */
    private static List<RecipeHolder<CraftingRecipe>> recipesFor(Level level, ItemStack wanted) {
        List<RecipeHolder<CraftingRecipe>> found = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe value = holder.value();
            if (!value.isSpecial() && !value.getIngredients().isEmpty()
                    && ItemStack.isSameItem(value.getResultItem(level.registryAccess()), wanted)) {
                found.add(holder);
            }
        }
        found.sort((a, b) -> a.id().compareTo(b.id()));
        return found;
    }

    /**
     * Teaches the assembler to make this item. Programming the same item again
     * switches to its next recipe, if it has more than one. Returns a message for the player.
     */
    public Component program(ItemStack wanted) {
        if (level == null) {
            return Component.empty();
        }
        List<RecipeHolder<CraftingRecipe>> options = recipesFor(level, wanted);
        if (options.isEmpty()) {
            return Component.translatable("message.epochsmith.assembler.no_recipe", wanted.getHoverName());
        }
        int choice = 0;
        if (recipe != null && ItemStack.isSameItem(target, wanted)) {
            choice = (options.indexOf(recipe) + 1) % options.size();
        }
        setRecipe(options.get(choice));
        return Component.translatable("message.epochsmith.assembler.programmed", target.getHoverName(),
                choice + 1, options.size(), describeIngredients());
    }

    private void setRecipe(@Nullable RecipeHolder<CraftingRecipe> holder) {
        recipe = holder;
        recipeId = holder == null ? null : holder.id();
        target = holder == null || level == null ? ItemStack.EMPTY : holder.value().getResultItem(level.registryAccess()).copy();
        progress = 0;
        changed();
    }

    /** "2 Oak Planks, 1 Stick", from the first matching item of each ingredient. */
    private Component describeIngredients() {
        List<ItemStack> needed = new ArrayList<>();
        for (Ingredient ingredient : neededIngredients()) {
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) {
                continue;
            }
            ItemStack example = items[0];
            ItemStack same = null;
            for (ItemStack counted : needed) {
                if (ItemStack.isSameItem(counted, example)) {
                    same = counted;
                }
            }
            if (same == null) {
                needed.add(example.copyWithCount(1));
            } else {
                same.grow(1);
            }
        }
        Component text = Component.empty();
        for (int i = 0; i < needed.size(); i++) {
            ItemStack stack = needed.get(i);
            text = Component.empty().append(text).append((i > 0 ? ", " : "") + stack.getCount() + " ")
                    .append(stack.getHoverName());
        }
        return text;
    }

    @Nullable
    private RecipeHolder<CraftingRecipe> getRecipe() {
        if (recipe == null && recipeId != null && level != null) {
            level.getRecipeManager().byKey(recipeId).ifPresent(holder -> {
                if (holder.value() instanceof CraftingRecipe) {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<CraftingRecipe> crafting = (RecipeHolder<CraftingRecipe>) holder;
                    recipe = crafting;
                }
            });
            if (recipe == null) {
                recipeId = null; // the recipe no longer exists (a datapack or mod was removed)
            }
        }
        return recipe;
    }

    private List<Ingredient> neededIngredients() {
        List<Ingredient> needed = new ArrayList<>();
        RecipeHolder<CraftingRecipe> holder = getRecipe();
        if (holder != null) {
            for (Ingredient ingredient : holder.value().getIngredients()) {
                if (!ingredient.isEmpty()) {
                    needed.add(ingredient);
                }
            }
        }
        return needed;
    }

    private boolean isIngredient(ItemStack stack) {
        for (Ingredient ingredient : neededIngredients()) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    // ---- Loading and collecting

    public boolean insert(ItemStack stack) {
        int before = stack.getCount();
        ItemStack rest = stack.copy();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            rest = ingredients.insertItem(i, rest, false);
        }
        stack.setCount(rest.getCount());
        return stack.getCount() < before;
    }

    public boolean isProgrammed() {
        return getRecipe() != null;
    }

    public boolean accepts(ItemStack stack) {
        return isIngredient(stack);
    }

    public List<ItemStack> takeOutputs() {
        return takeAll(outputs);
    }

    /** Takes back the ingredients and forgets the recipe. */
    public List<ItemStack> reset() {
        List<ItemStack> taken = takeAll(ingredients);
        setRecipe(null);
        return taken;
    }

    public List<ItemStack> takeEverything() {
        List<ItemStack> all = takeAll(ingredients);
        all.addAll(takeAll(outputs));
        return all;
    }

    private static List<ItemStack> takeAll(ItemStackHandler handler) {
        List<ItemStack> taken = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    public Component describe() {
        if (!isProgrammed()) {
            return Component.translatable("message.epochsmith.assembler.unprogrammed");
        }
        int made = 0;
        for (int i = 0; i < SLOTS; i++) {
            made += outputs.getStackInSlot(i).getCount();
        }
        String state = getBlockState().getValue(BlockStateProperties.LIT) ? "message.epochsmith.assembler.working"
                : findIngredients() == null ? "message.epochsmith.assembler.waiting"
                : "message.epochsmith.assembler.unpowered";
        return Component.translatable("message.epochsmith.assembler.status", target.getHoverName(), made,
                Component.translatable(state, describeIngredients()));
    }

    // ---- Crafting

    /**
     * Works out which ingredient slots one craft would use: one item per ingredient.
     * Returns how many to take from each slot, or null if something is missing.
     */
    @Nullable
    private int[] findIngredients() {
        int[] use = new int[SLOTS];
        for (Ingredient ingredient : neededIngredients()) {
            boolean found = false;
            for (int i = 0; i < SLOTS && !found; i++) {
                ItemStack stack = ingredients.getStackInSlot(i);
                if (stack.getCount() > use[i] && ingredient.test(stack)) {
                    use[i]++;
                    found = true;
                }
            }
            if (!found) {
                return null;
            }
        }
        return use;
    }

    private boolean fits(List<ItemStack> results) {
        ItemStackHandler test = new ItemStackHandler(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            test.setStackInSlot(i, outputs.getStackInSlot(i).copy());
        }
        for (ItemStack result : results) {
            ItemStack rest = result.copy();
            for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
                rest = test.insertItem(i, rest, false);
            }
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void store(ItemStack result) {
        ItemStack rest = result.copy();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            rest = outputs.insertItem(i, rest, false);
        }
    }

    /** The result plus anything left behind (like an empty bucket from a milk bucket). */
    private List<ItemStack> resultsFor(int[] use) {
        List<ItemStack> results = new ArrayList<>();
        results.add(target.copy());
        for (int i = 0; i < SLOTS; i++) {
            if (use[i] > 0) {
                ItemStack leftover = ingredients.getStackInSlot(i).getCraftingRemainingItem();
                if (!leftover.isEmpty()) {
                    results.add(leftover.copyWithCount(leftover.getCount() * use[i]));
                }
            }
        }
        return results;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AssemblerBlockEntity assembler) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            assembler.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean working = false;
        int[] use = assembler.isProgrammed() ? assembler.findIngredients() : null;
        if (use != null && assembler.fits(assembler.resultsFor(use))) {
            int drawn = ElectricNetwork.draw(level, assembler.sources, ENERGY_PER_TICK, false);
            working = drawn > 0;
            assembler.progress += drawn;
            if (assembler.progress >= ENERGY_PER_CRAFT) {
                assembler.progress -= ENERGY_PER_CRAFT;
                for (ItemStack result : assembler.resultsFor(use)) {
                    assembler.store(result);
                }
                for (int i = 0; i < SLOTS; i++) {
                    if (use[i] > 0) {
                        assembler.ingredients.extractItem(i, use[i], false);
                    }
                }
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.25F, 1.8F);
            }
            assembler.setChanged();
        } else {
            assembler.progress = 0;
        }
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Saving and syncing

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
        }
        tag.put("Target", target.saveOptional(registries));
        tag.put("Ingredients", ingredients.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        recipeId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
        recipe = null;
        target = ItemStack.parseOptional(registries, tag.getCompound("Target"));
        ingredients.deserializeNBT(registries, tag.getCompound("Ingredients"));
        outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        progress = tag.getInt("Progress");
    }

    /** The client only needs to know what's being made, to show it on top. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Target", target.saveOptional(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        target = ItemStack.parseOptional(registries, tag.getCompound("Target"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
