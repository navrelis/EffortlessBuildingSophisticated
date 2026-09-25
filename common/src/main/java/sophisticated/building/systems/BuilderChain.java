package sophisticated.building.systems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.ClientConfig;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.client.ClientBreakCountdown;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.network.message.ServerBreakBlocksPacket;
import sophisticated.building.network.message.ServerPlaceBlocksPacket;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.BlockUtilities;
import sophisticated.building.utilities.BreakToolHelper;
import sophisticated.building.utilities.ClientBlockUtilities;
import sophisticated.building.utilities.ReplaceRules;
import sophisticated.building.utilities.SurvivalHelper;
import sophisticated.building.platform.Services;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Receives block placed events, then finds additional blocks we want to place through various systems,
// and then sends them to the server to be placed
// Uses chain of responsibility pattern
public class BuilderChain {

    private final BlockSet blocks = new BlockSet();
    private net.minecraft.world.item.Item previousHeldItem;
    private int soundTime = 0;
    private BlockEntry startPosForPlacing;
    private BlockPos startPosForBreaking;
    private BlockHitResult lookingAtNear;
    //Can be near or far depending on abilities
    //Only updated when we are in IDLE state
    private BlockHitResult lookingAt;
    
    // Caching for performance optimization
    private Vec3 lastPlayerPos;
    private Vec3 lastPlayerLook;
    private int ticksSinceLastFullUpdate = 0;
    private static final int MIN_TICKS_BETWEEN_UPDATES = 1; // Update at most every N ticks when idle
    private boolean forceUpdate = true;

    public enum BuildingState {
        IDLE,
        PLACING,
        BREAKING
    }

    //What we are currently doing
    private BuildingState buildingState = BuildingState.IDLE;

    public enum AbilitiesState {
        CAN_PLACE_AND_BREAK,
        CAN_BREAK,
        NONE
    }

    //Whether we can place or break blocks, determined by what we are looking at and what we are holding
    private AbilitiesState abilitiesState = AbilitiesState.CAN_PLACE_AND_BREAK;

    //Survival break plan for the current tick's blocks (null when not breaking, or in creative)
    @Nullable
    private BreakToolHelper.BreakPlan lastBreakPlan;

    public void onRightClick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel world = mc.level;
        if (player == null || world == null) return;

        if (ClientBlockUtilities.determineIfLookingAtInteractiveObject(mc, world) && !player.isShiftKeyDown()) {
            cancel();
            return;
        }

        if (abilitiesState != AbilitiesState.CAN_PLACE_AND_BREAK || buildingState == BuildingState.BREAKING) {
            cancel();
            return;
        }

        if (buildingState == BuildingState.IDLE) {
            buildingState = BuildingState.PLACING;
        }

        BuildModeEnum buildMode = SophisticatedBuildingClient.BUILD_MODES.getBuildMode();

        //Find out if we should place blocks now
        if (buildMode.instance.onClick(blocks)) {
            buildingState = BuildingState.IDLE;

            if (!blocks.isEmpty()) {
                //Vanilla places the first block itself; nothing to send if that is the only one
                blocks.skipFirst = vanillaHandlesFirst(buildMode);
                if (!blocks.hasUnskippedEntries()) return;

                // Randomize block states fresh before sending to server
                // Use fresh random selection per block position.
                randomizeBlockStatesFresh(player, player.getItemInHand(InteractionHand.MAIN_HAND));

                //Survival replace: the server first mines the blocks in the way, so the appear animation waits for it
                Set<BlockPos> minedPositions = new HashSet<>();
                if (!player.isCreative() && SophisticatedBuildingClient.BUILD_SETTINGS.isQuickReplacing()) {
                    for (BlockEntry entry : findMinedEntries()) minedPositions.add(entry.blockPos);
                }
                if (minedPositions.isEmpty()) {
                    SophisticatedBuildingClient.BLOCK_PREVIEWS.onBlocksPlaced(blocks);
                } else {
                    ClientBreakCountdown.addPendingPlacement(new BlockSet(blocks), minedPositions);
                }
                ClientBlockUtilities.playSoundIfFurtherThanNormal(player, blocks.getLastBlockEntry(), false);
                player.swing(InteractionHand.MAIN_HAND);

                long placeTime = player.level.getGameTime();
                if (blocks.size() > 1) placeTime += ClientConfig.visuals.appearAnimationLength.get();
                Services.NETWORK.sendToServer(new ServerPlaceBlocksPacket(blocks, placeTime));
            }
        }
    }

    public void onLeftClick() {

        if (abilitiesState == AbilitiesState.NONE || buildingState == BuildingState.PLACING) {
            cancel();
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!AttachmentHandler.canBreakFar(player)) return;

        if (buildingState == BuildingState.IDLE){
            buildingState = BuildingState.BREAKING;

            //Use new start position for breaking, because we assumed the player was gonna place
            blocks.setStartPos(new BlockEntry(startPosForBreaking));
            SophisticatedBuildingClient.BUILD_MODIFIERS.findCoordinates(blocks, player);
            SophisticatedBuildingClient.BUILDER_FILTER.filterOnCoordinates(blocks, player);
            findExistingBlockStates(player.level);
            SophisticatedBuildingClient.BUILDER_FILTER.filterOnExistingBlockStates(blocks, player);
        }

        BuildModeEnum buildMode = SophisticatedBuildingClient.BUILD_MODES.getBuildMode();

        //Find out if we should break blocks now
        if (buildMode.instance.onClick(blocks)) {
            buildingState = BuildingState.IDLE;

            if (!blocks.isEmpty()) {
                //Vanilla mines the first block itself; nothing to send if that is the only one.
                //Set for this send before anything reads it (setStartPos/clear never reset it).
                blocks.skipFirst = vanillaHandlesFirst(buildMode);
                if (!blocks.hasUnskippedEntries()) return;

                if (!player.isCreative()) {
                    BlockPos skipPos = blocks.skipFirst ? blocks.firstPos : null;

                    //The set may have changed since the last onTick plan; re-plan against the final set.
                    lastBreakPlan = BreakToolHelper.planClient(player, blocks, skipPos);
                    int invalidCount = countInvalid(blocks);
                    int validCount = 0;
                    for (BlockEntry entry : blocks) {
                        if (entry.invalid) continue;
                        if (blocks.isSkipped(entry)) continue;
                        validCount++;
                    }
                    if (validCount <= 0) {
                        SophisticatedBuilding.logTranslate(player, "", "sophisticatedbuilding.message.survival_break_nothing", "", true);
                        cancel();
                        return;
                    }
                    if (invalidCount > 0) {
                        SophisticatedBuilding.logTranslate(player, invalidCount + " ", "sophisticatedbuilding.message.survival_break_partial", "", true);
                    }
                }

                ClientBlockUtilities.playSoundIfFurtherThanNormal(player, blocks.getLastBlockEntry(), true);
                player.swing(InteractionHand.MAIN_HAND);

                if (player.isCreative()) {
                    // Creative breaking is instant server-side; keep the immediate dissolve animation.
                    SophisticatedBuildingClient.BLOCK_PREVIEWS.onBlocksBroken(blocks);
                } else {
                    // Survival breaking is delayed server-side; defer the dissolve animation until
                    // the matching BreakCountdownPacket's countdown reaches 0 (T-S10). The red
                    // selection outline stays visible on these blocks via BlockPreviews' "pending"
                    // cluster in the meantime.
                    ClientBreakCountdown.addPending(new BlockSet(blocks));
                }

                Services.NETWORK.sendToServer(new ServerBreakBlocksPacket(blocks));
            }
        }
    }

    //Vanilla places or mines the first block itself only in Disable mode without Quick Replace (the server cancels it
    //otherwise), so only then the server must skip it
    private static boolean vanillaHandlesFirst(BuildModeEnum buildMode) {
        return ReplaceRules.vanillaHandlesFirst(buildMode == BuildModeEnum.DISABLED,
                SophisticatedBuildingClient.BUILD_SETTINGS.isQuickReplacing());
    }

    private static int countInvalid(BlockSet blocks) {
        int count = 0;
        for (BlockEntry entry : blocks) {
            if (entry.invalid) count++;
        }
        return count;
    }

    public void onTick() {
        // Tick the BlockSet cooldown for rate-limited logging
        BlockSet.ClientSide.tickCooldown();
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel world = mc.level;
        
        // Check if we need a full update based on player movement/look changes
        Vec3 currentPos = player.position();
        Vec3 currentLook = player.getLookAngle();
        
        boolean playerMoved = lastPlayerPos == null || 
            currentPos.distanceToSqr(lastPlayerPos) > 0.001 ||
            currentLook.distanceToSqr(lastPlayerLook) > 0.0001;
        
        ticksSinceLastFullUpdate++;
        
        // Skip expensive calculations if player hasn't moved much and we're idle
        // This can be disabled via config if users experience update issues
        boolean throttlingEnabled = ClientConfig.performance.enableUpdateThrottling.get();
        if (throttlingEnabled && !forceUpdate && !playerMoved && buildingState == BuildingState.IDLE && 
            ticksSinceLastFullUpdate < MIN_TICKS_BETWEEN_UPDATES) {
            return;
        }

        HashSet<BlockPos> previousCoordinates = new HashSet<>(blocks.getCoordinates());
        blocks.clear();
        startPosForPlacing = null;
        startPosForBreaking = null;
        lookingAtNear = null;

        abilitiesState = determineAbilities(mc, player, world);
        if (abilitiesState == AbilitiesState.NONE) {
            lastPlayerPos = currentPos;
            lastPlayerLook = currentLook;
            return;
        }

        BuildModeEnum buildMode = SophisticatedBuildingClient.BUILD_MODES.getBuildMode();

        if (buildingState == BuildingState.IDLE) {
            //Find start position
            BlockEntry startEntry = findStartPosition(player, buildMode);
            if (startEntry != null) {
                blocks.setStartPos(startEntry);
            } else {
                //We aren't placing or breaking blocks, and we have no start position
                abilitiesState = AbilitiesState.NONE;
                return;
            }
        }

        SophisticatedBuildingClient.BUILD_MODES.findCoordinates(blocks, player);
        SophisticatedBuildingClient.BUILD_MODIFIERS.findCoordinates(blocks, player);
        SophisticatedBuildingClient.BUILDER_FILTER.filterOnCoordinates(blocks, player);

        //Vanilla alone handles a single block in Disable mode (with Quick Replace the mod replaces it instead)
        boolean vanillaHandlesFirst = vanillaHandlesFirst(buildMode);
        if (vanillaHandlesFirst && blocks.size() <= 1) {
            abilitiesState = AbilitiesState.NONE;
            return;
        }

        findExistingBlockStates(world);
        SophisticatedBuildingClient.BUILDER_FILTER.filterOnExistingBlockStates(blocks, player);

        if (getPretendBuildingState() == BuildingState.BREAKING && !player.isCreative()) {
            BlockPos skipPos = vanillaHandlesFirst ? blocks.firstPos : null;
            lastBreakPlan = BreakToolHelper.planClient(player, blocks, skipPos);
        } else {
            lastBreakPlan = null;
        }

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        findNewBlockStates(player, heldItem); //includes filtering on new blockstates

        //Check if any changes are made
        if (previousHeldItem != heldItem.getItem() || !previousCoordinates.equals(blocks.getCoordinates())) {
            onBlocksChanged(player);
        }

        previousHeldItem = heldItem.getItem();
        
        // Update cache tracking
        lastPlayerPos = currentPos;
        lastPlayerLook = currentLook;
        ticksSinceLastFullUpdate = 0;
        forceUpdate = false;
    }
    
    // Call this when something changes that requires a full recalculation
    public void requestUpdate() {
        forceUpdate = true;
    }

    //Whether we can place or break blocks, determined by what we are looking at and what we are holding
    private AbilitiesState determineAbilities(Minecraft mc, Player player, Level world) {

        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            lookingAtNear = (BlockHitResult) hitResult;
        }

        ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean blockInHand = CompatHelper.isItemBlockProxy(itemStack);
        boolean lookingAtInteractiveObject = ClientBlockUtilities.determineIfLookingAtInteractiveObject(mc, world);
        boolean isShiftKeyDown = player.isShiftKeyDown();

        if (lookingAtInteractiveObject && !isShiftKeyDown)
            return AbilitiesState.NONE;

        if (!blockInHand)
            return AbilitiesState.CAN_BREAK;

        return AbilitiesState.CAN_PLACE_AND_BREAK;
    }

    private BlockEntry findStartPosition(Player player, BuildModeEnum buildMode) {

        int maxReach = AttachmentHandler.getPlacementReach(player, false);

        //Determine if we should look far or nearby
        boolean shouldLookAtNear = buildMode == BuildModeEnum.DISABLED || maxReach < 3;
        if (shouldLookAtNear) {
            lookingAt = lookingAtNear;
        } else {
            lookingAt = ClientBlockUtilities.getLookingAtFar(player);
        }
        if (lookingAt == null || lookingAt.getType() == HitResult.Type.MISS) return null;

        BlockPos startPos = lookingAt.getBlockPos();

        //Check if out of reach
        if (!shouldLookAtNear && player.blockPosition().distSqr(startPos) > maxReach * maxReach) return null;

        startPosForBreaking = startPos;

        if (abilitiesState == AbilitiesState.CAN_PLACE_AND_BREAK) {
            //Calculate start position for placing

            //Offset in direction of sidehit if not quickreplace and not replaceable
            boolean shouldOffsetStartPosition = SophisticatedBuildingClient.BUILD_SETTINGS.shouldOffsetStartPosition();
            boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
            boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos);
            if (!shouldOffsetStartPosition && !replaceable && !becomesDoubleSlab) {
                startPos = startPos.relative(lookingAt.getDirection());
            }

        } else {
            //We can only break

            //Do not break far if we are not allowed to
            if (!shouldLookAtNear && !AttachmentHandler.canBreakFar(player)) return null;
        }

        BlockEntry blockEntry = new BlockEntry(startPos);
        startPosForPlacing = blockEntry;
        return blockEntry;
    }

    private void findExistingBlockStates(Level world) {
        for (BlockEntry blockEntry : blocks) {
            blockEntry.existingBlockState = world.getBlockState(blockEntry.blockPos);
        }
    }

    private void findNewBlockStates(Player player, ItemStack heldItem) {
        if (buildingState == BuildingState.BREAKING) return;

        Direction originalDirection = player.getDirection();
        Direction clickedFace = lookingAt.getDirection();
        Vec3 relativeHitVec = lookingAt.getLocation().subtract(Vec3.atLowerCornerOf(lookingAt.getBlockPos()));

        //Keep track of itemstack usage
        SophisticatedBuildingClient.ITEM_USAGE_TRACKER.initialize();

        Iterator<Map.Entry<BlockPos, BlockEntry>> iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            BlockEntry blockEntry = iter.next().getValue();

            //Determine itemstack - pass position for per-position randomization
            ItemStack itemStack = determineItemStack(player, heldItem, blockEntry.blockPos);
            if (itemStack == null || itemStack.isEmpty()) {
                iter.remove();
                continue;
            }

            //Find new blockstate
            blockEntry.invalid = false;
            blockEntry.setItemAndFindNewBlockState(itemStack, player.level, player, originalDirection, clickedFace, relativeHitVec);

            //Filter on new blockstate
            if (SophisticatedBuildingClient.BUILDER_FILTER.filterOnNewBlockState(blockEntry, player)) {
                iter.remove();
                continue;
            }

            //No placeable state (e.g. getStateForPlacement failed): keep it invalid and uncounted
            if (blockEntry.newBlockState == null) {
                blockEntry.invalid = true;
            }
        }

        if (!player.isCreative()) {
            //Survival places the same block only as a merge (slab to double slab, one more candle...), the server skips the rest
            for (BlockEntry blockEntry : blocks) {
                if (blockEntry.invalid || blockEntry.newBlockState == null) continue;
                if (!BlockUtilities.needsMining(blockEntry.existingBlockState)) continue;
                if (!blockEntry.existingBlockState.is(blockEntry.newBlockState.getBlock())) continue;
                if (!BlockUtilities.isOneStepMerge(blockEntry.existingBlockState, blockEntry.newBlockState)) blockEntry.invalid = true;
            }

            //Survival replace: blocks in the way get mined; the ones no available tool can mine are marked invalid
            if (SophisticatedBuildingClient.BUILD_SETTINGS.isQuickReplacing()) {
                BreakToolHelper.planClient(player, findMinedEntries(), null);
            }
        }

        //Increase itemstack usage if not filtered out or invalid
        //Mark invalid if the player does not have enough of that item
        for (BlockEntry blockEntry : blocks) {
            if (blockEntry.invalid) continue;
            blockEntry.invalid = !SophisticatedBuildingClient.ITEM_USAGE_TRACKER.increaseUsageCount(blockEntry.item, 1, player);
        }

        SophisticatedBuildingClient.ITEM_USAGE_TRACKER.calculateMissingItems(player);
    }

    //Valid entries that would replace a block that has to be mined first (merges into the same block are not mined)
    private List<BlockEntry> findMinedEntries() {
        List<BlockEntry> mined = new ArrayList<>();
        for (BlockEntry blockEntry : blocks) {
            if (blockEntry.invalid || blockEntry.newBlockState == null) continue;
            if (!BlockUtilities.needsMining(blockEntry.existingBlockState)) continue;
            if (blockEntry.existingBlockState.is(blockEntry.newBlockState.getBlock())) continue;
            mined.add(blockEntry);
        }
        return mined;
    }

    /**
     * Randomize block states fresh right before placing.
     * Uses fresh random selection per block position in randomizer bag builds.
     * Only affects randomizer bag items - regular blocks are unchanged.
     */
    private void randomizeBlockStatesFresh(Player player, ItemStack heldItem) {
        if (!CompatHelper.isItemBlockProxy(heldItem, false)) return;
        if (heldItem.getItem() instanceof BlockItem) return;
        if (lookingAt == null || lookingAt.getType() != HitResult.Type.BLOCK) return;
        
        Direction originalDirection = player.getDirection();
        Direction clickedFace = lookingAt.getDirection();
        Vec3 relativeHitVec = lookingAt.getLocation().subtract(Vec3.atLowerCornerOf(lookingAt.getBlockPos()));

        for (BlockEntry blockEntry : blocks) {
            // Get a fresh random block for each position
            ItemStack itemStack = CompatHelper.getItemBlockFromStackFresh(heldItem, player);
            if (itemStack == null || itemStack.isEmpty()) continue;
            
            blockEntry.setItemAndFindNewBlockState(itemStack, player.level, player, originalDirection, clickedFace, relativeHitVec);
        }
    }

    private ItemStack determineItemStack(Player player, ItemStack heldItem, BlockPos blockPos) {
        if (heldItem.getItem() instanceof BlockItem) {
            return heldItem;
        }

        if (CompatHelper.isItemBlockProxy(heldItem, false)) {
            // Use per-position cached random selection for preview stability
            // Each position gets its own stable random, only from available inventory
            return CompatHelper.getItemBlockForPosition(heldItem, blockPos, player);
        }

        return null;
    }

    private void onBlocksChanged(Player player) {

        //Play sound (max once every tick)
        if (blocks.size() > 1 && soundTime < ClientEvents.ticksInGame) {
            soundTime = ClientEvents.ticksInGame;

            if (blocks.getLastBlockEntry() != null && blocks.getLastBlockEntry().newBlockState != null) {
                BlockState lastBlockState = blocks.getLastBlockEntry().newBlockState;
                SoundType soundType = Services.BLOCK_EVENTS.getSoundType(lastBlockState, player.level, blocks.lastPos, player);
                SoundEvent soundEvent = buildingState == BuildingState.BREAKING ? soundType.getBreakSound() : soundType.getPlaceSound();
                player.level.playSound(player, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 0.3f, 0.8f);
            }
        }
    }

    public void cancel() {
        if (buildingState == BuildingState.IDLE) return;
        buildingState = BuildingState.IDLE;
        SophisticatedBuildingClient.BUILD_MODES.onCancel();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.UI_TOAST_OUT, 4, 1);
        }
    }

    public BlockSet getBlocks() {
        return blocks;
    }

    public BuildingState getBuildingState() {
        return buildingState;
    }

    public AbilitiesState getAbilitiesState() {
        return abilitiesState;
    }

    public BuildingState getPretendBuildingState() {
        if (buildingState != BuildingState.IDLE) return buildingState;
        if (abilitiesState == AbilitiesState.CAN_PLACE_AND_BREAK) return BuildingState.PLACING;
        if (abilitiesState == AbilitiesState.CAN_BREAK) return BuildingState.BREAKING;
        return BuildingState.IDLE;
    }

    public BlockEntry getStartPosForPlacing() {
        return startPosForPlacing;
    }

    public BlockPos getStartPosForBreaking() {
        return startPosForBreaking;
    }

    public BlockEntry getStartPos() {
        if (getPretendBuildingState() == BuildingState.BREAKING) return new BlockEntry(getStartPosForBreaking());
        return getStartPosForPlacing();
    }

    public BlockHitResult getLookingAtNear() {
        return lookingAtNear;
    }

    @Nullable
    public BreakToolHelper.BreakPlan getBreakPlan() {
        return lastBreakPlan;
    }
}

