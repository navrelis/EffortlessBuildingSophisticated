package sophisticated.building.buildmode;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import sophisticated.building.AllIcons;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.network.message.PerformRedoPacket;
import sophisticated.building.network.message.PerformUndoPacket;
import sophisticated.building.systems.BuildSettings;

@OnlyIn(Dist.CLIENT)
public class ModeOptions {

	private static ActionEnum buildSpeed = ActionEnum.NORMAL_SPEED;
	private static ActionEnum fill = ActionEnum.FULL;
	private static ActionEnum cubeFill = ActionEnum.CUBE_FULL;
	private static ActionEnum raisedEdge = ActionEnum.SHORT_EDGE;
	private static ActionEnum lineThickness = ActionEnum.THICKNESS_1;
	private static ActionEnum circleStart = ActionEnum.CIRCLE_START_CORNER;
	private static ActionEnum terrainNoise = ActionEnum.TERRAIN_NOISE_ON;
	private static ActionEnum terrainType = ActionEnum.TERRAIN_MOUND;

	public static ActionEnum getOptionSetting(OptionEnum option) {
		switch (option) {
			case BUILD_SPEED:
				return getBuildSpeed();
			case FILL:
				return getFill();
			case CUBE_FILL:
				return getCubeFill();
			case RAISED_EDGE:
				return getRaisedEdge();
			case LINE_THICKNESS:
				return getLineThickness();
			case CIRCLE_START:
				return getCircleStart();
			case TERRAIN_NOISE:
				return getTerrainNoise();
			case TERRAIN_TYPE:
				return getTerrainType();
			default:
				return null;
		}
	}

	public static ActionEnum getBuildSpeed() {
		return buildSpeed;
	}

	public static ActionEnum getFill() {
		return fill;
	}

	public static ActionEnum getCubeFill() {
		return cubeFill;
	}

	public static ActionEnum getRaisedEdge() {
		return raisedEdge;
	}

	public static ActionEnum getLineThickness() {
		return lineThickness;
	}

	public static ActionEnum getCircleStart() {
		return circleStart;
	}

	public static ActionEnum getTerrainNoise() {
		return terrainNoise;
	}

	public static ActionEnum getTerrainType() {
		return terrainType;
	}

	public static void performAction(Player player, ActionEnum action) {
		if (action == null) return;

		switch (action) {
			case UNDO -> PacketDistributor.sendToServer(new PerformUndoPacket());
			case REDO -> PacketDistributor.sendToServer(new PerformRedoPacket());
			case OPEN_MODIFIER_SETTINGS -> ClientEvents.openModifierSettings();
			case OPEN_PLAYER_SETTINGS -> ClientEvents.openPlayerSettings();
			case PREVIOUS_BUILD_MODE -> SophisticatedBuildingClient.BUILD_MODES.activatePreviousBuildMode();
			case DISABLE_BUILD_MODE_TOGGLE -> SophisticatedBuildingClient.BUILD_MODES.activateDisableBuildModeToggle();

			case REPLACE_ONLY_AIR -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.ONLY_AIR);
			case REPLACE_BLOCKS_AND_AIR -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.BLOCKS_AND_AIR);
			case REPLACE_ONLY_BLOCKS -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.ONLY_BLOCKS);
			case REPLACE_FILTERED_BY_OFFHAND -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.FILTERED_BY_OFFHAND);
			case TOGGLE_PROTECT_TILE_ENTITIES -> SophisticatedBuildingClient.BUILD_SETTINGS.toggleProtectTileEntities();
			case TOGGLE_MINI_PREVIEW -> {
				boolean enabled = SophisticatedBuildingClient.BLOCK_PREVIEWS.toggleMiniBlockPreview();
				SophisticatedBuilding.logTranslate(player, "", enabled ? "sophisticatedbuilding.action.toggle_mini_preview.enabled" : "sophisticatedbuilding.action.toggle_mini_preview.disabled", "", true);
			}

			case NORMAL_SPEED -> buildSpeed = ActionEnum.NORMAL_SPEED;
			case FAST_SPEED -> buildSpeed = ActionEnum.FAST_SPEED;

			case FULL -> fill = ActionEnum.FULL;
			case HOLLOW -> fill = ActionEnum.HOLLOW;

			case CUBE_FULL -> cubeFill = ActionEnum.CUBE_FULL;
			case CUBE_HOLLOW -> cubeFill = ActionEnum.CUBE_HOLLOW;
			case CUBE_SKELETON -> cubeFill = ActionEnum.CUBE_SKELETON;

			case SHORT_EDGE -> raisedEdge = ActionEnum.SHORT_EDGE;
			case LONG_EDGE -> raisedEdge = ActionEnum.LONG_EDGE;

			case THICKNESS_1 -> lineThickness = ActionEnum.THICKNESS_1;
			case THICKNESS_3 -> lineThickness = ActionEnum.THICKNESS_3;
			case THICKNESS_5 -> lineThickness = ActionEnum.THICKNESS_5;

			case CIRCLE_START_CENTER -> circleStart = ActionEnum.CIRCLE_START_CENTER;
			case CIRCLE_START_CORNER -> circleStart = ActionEnum.CIRCLE_START_CORNER;

			case TERRAIN_NOISE_OFF -> terrainNoise = ActionEnum.TERRAIN_NOISE_OFF;
			case TERRAIN_NOISE_ON -> terrainNoise = ActionEnum.TERRAIN_NOISE_ON;

			case TERRAIN_MOUND -> terrainType = ActionEnum.TERRAIN_MOUND;
			case TERRAIN_SLOPE -> terrainType = ActionEnum.TERRAIN_SLOPE;
			case TERRAIN_FLAT -> terrainType = ActionEnum.TERRAIN_FLAT;
			case TERRAIN_MOUNTAIN -> terrainType = ActionEnum.TERRAIN_MOUNTAIN;
			case TERRAIN_WALL -> terrainType = ActionEnum.TERRAIN_WALL;
		}

		if (player.level().isClientSide &&
			action != ActionEnum.OPEN_MODIFIER_SETTINGS &&
			action != ActionEnum.OPEN_PLAYER_SETTINGS &&
			action != ActionEnum.PREVIOUS_BUILD_MODE &&
			action != ActionEnum.DISABLE_BUILD_MODE_TOGGLE) {

			SophisticatedBuilding.logTranslate(player, "", action.getNameKey(), "", true);
		}
	}

	public enum ActionEnum {
		UNDO("undo", AllIcons.I_UNDO),
		REDO("redo", AllIcons.I_REDO),
		OPEN_MODIFIER_SETTINGS("open_modifier_settings", AllIcons.I_SETTINGS),
		OPEN_PLAYER_SETTINGS("open_player_settings", AllIcons.I_SETTINGS),
		PREVIOUS_BUILD_MODE("previous_build_mode", AllIcons.I_SINGLE),
		DISABLE_BUILD_MODE_TOGGLE("disable_build_mode_toggle", AllIcons.I_DISABLE),

		REPLACE_ONLY_AIR("replace_only_air", AllIcons.I_REPLACE_AIR),
		REPLACE_BLOCKS_AND_AIR("replace_blocks_and_air", AllIcons.I_REPLACE_BLOCKS_AND_AIR),
		REPLACE_ONLY_BLOCKS("replace_only_blocks", AllIcons.I_REPLACE_BLOCKS),
		REPLACE_FILTERED_BY_OFFHAND("replace_filtered_by_offhand", AllIcons.I_REPLACE_OFFHAND_FILTERED),
		TOGGLE_PROTECT_TILE_ENTITIES("toggle_protect_tile_entities", AllIcons.I_PROTECT_TILE_ENTITIES),
		TOGGLE_MINI_PREVIEW("toggle_mini_preview", AllIcons.I_MINI_PREVIEW),

		NORMAL_SPEED("normal_speed", AllIcons.I_NORMAL_SPEED),
		FAST_SPEED("fast_speed", AllIcons.I_FAST_SPEED),

		FULL("full", AllIcons.I_FILLED),
		HOLLOW("hollow", AllIcons.I_HOLLOW),

		CUBE_FULL("full", AllIcons.I_CUBE_FILLED),
		CUBE_HOLLOW("hollow", AllIcons.I_CUBE_HOLLOW),
		CUBE_SKELETON("skeleton", AllIcons.I_CUBE_SKELETON),

		SHORT_EDGE("short_edge", AllIcons.I_SHORT_EDGE),
		LONG_EDGE("long_edge", AllIcons.I_LONG_EDGE),

		THICKNESS_1("thickness_1", AllIcons.I_THICKNESS_1),
		THICKNESS_3("thickness_3", AllIcons.I_THICKNESS_3),
		THICKNESS_5("thickness_5", AllIcons.I_THICKNESS_5),

		CIRCLE_START_CORNER("start_corner", AllIcons.I_CIRCLE_START_CORNER),
		CIRCLE_START_CENTER("start_center", AllIcons.I_CIRCLE_START_CENTER),

		TERRAIN_NOISE_OFF("terrain_noise_off", AllIcons.I_TERRAIN_NOISE_OFF),
		TERRAIN_NOISE_ON("terrain_noise_on", AllIcons.I_TERRAIN_NOISE_ON),

		TERRAIN_MOUND("terrain_mound", AllIcons.I_TERRAIN_MOUND),
		TERRAIN_SLOPE("terrain_slope", AllIcons.I_TERRAIN_SLOPE),
		TERRAIN_FLAT("terrain_flat", AllIcons.I_TERRAIN_FLAT),
		TERRAIN_MOUNTAIN("terrain_mountain", AllIcons.I_TERRAIN_MOUNTAIN),
		TERRAIN_WALL("terrain_wall", AllIcons.I_TERRAIN_WALL);

		public String name;
		public AllIcons icon;

		ActionEnum(String name, AllIcons icon) {
			this.name = name;
			this.icon = icon;
		}
		
		public String getName() {
			return name;
		}
		
		public String getNameKey() {
			return "sophisticatedbuilding.action." + name;
		}
		
		public String getDescriptionKey() {
			return "sophisticatedbuilding.action." + name + ".description";
		}
	}

	public enum OptionEnum {
		BUILD_SPEED("sophisticatedbuilding.action.build_speed", ActionEnum.NORMAL_SPEED, ActionEnum.FAST_SPEED),
		FILL("sophisticatedbuilding.action.filling", ActionEnum.FULL, ActionEnum.HOLLOW),
		CUBE_FILL("sophisticatedbuilding.action.filling", ActionEnum.CUBE_FULL, ActionEnum.CUBE_HOLLOW, ActionEnum.CUBE_SKELETON),
		RAISED_EDGE("sophisticatedbuilding.action.raised_edge", ActionEnum.SHORT_EDGE, ActionEnum.LONG_EDGE),
		LINE_THICKNESS("sophisticatedbuilding.action.thickness", ActionEnum.THICKNESS_1, ActionEnum.THICKNESS_3, ActionEnum.THICKNESS_5),
		CIRCLE_START("sophisticatedbuilding.action.circle_start", ActionEnum.CIRCLE_START_CORNER, ActionEnum.CIRCLE_START_CENTER),
		TERRAIN_NOISE("sophisticatedbuilding.action.terrain_noise", ActionEnum.TERRAIN_NOISE_OFF, ActionEnum.TERRAIN_NOISE_ON),
		TERRAIN_TYPE("sophisticatedbuilding.action.terrain_type", ActionEnum.TERRAIN_MOUND, ActionEnum.TERRAIN_SLOPE, ActionEnum.TERRAIN_FLAT, ActionEnum.TERRAIN_MOUNTAIN, ActionEnum.TERRAIN_WALL);

		public String name;
		public ActionEnum[] actions;

		OptionEnum(String name, ActionEnum... actions) {
			this.name = name;
			this.actions = actions;
		}
	}
}
