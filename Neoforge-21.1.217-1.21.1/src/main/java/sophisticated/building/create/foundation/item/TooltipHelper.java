package sophisticated.building.create.foundation.item;

import com.google.common.base.Strings;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.lang.ClientFontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.create.foundation.utility.Lang;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TooltipHelper {

	public static final int maxWidthPerLine = 200;
	public static final Map<String, ItemDescription> cachedTooltips = new HashMap<>();
	private static boolean gogglesMode;
	private static final Map<Item, Supplier<String>> tooltipReferrals = new HashMap<>();

	public static MutableComponent holdShift(ItemDescription.Palette color, boolean highlighted) {
		return Lang.translateDirect("tooltip.holdForDescription", Lang.translateDirect("tooltip.keyShift")
			.withStyle(ChatFormatting.GRAY))
			.withStyle(ChatFormatting.DARK_GRAY);
	}

	public static void addHint(List<Component> tooltip, String hintKey, Object... messageParams) {
		Component spacing = Components.literal("");
		tooltip.add(spacing.plainCopy()
			.append(Lang.translateDirect(hintKey + ".title"))
			.withStyle(ChatFormatting.GOLD));
		Component hint = Lang.translateDirect(hintKey);
		List<Component> cutComponent = TooltipHelper.cutTextComponent(hint, ChatFormatting.GRAY, ChatFormatting.WHITE);
		for (Component component : cutComponent)
			tooltip.add(spacing.plainCopy()
				.append(component));
	}

	public static void referTo(ItemLike item, Supplier<? extends ItemLike> itemWithTooltip) {
		tooltipReferrals.put(item.asItem(), () -> itemWithTooltip.get()
			.asItem()
			.getDescriptionId());
	}

	public static void referTo(ItemLike item, String string) {
		tooltipReferrals.put(item.asItem(), () -> string);
	}

	@Deprecated
	public static List<String> cutString(Component s, ChatFormatting defaultColor, ChatFormatting highlightColor) {
		return cutString(s.getString(), defaultColor, highlightColor, 0);
	}

	@Deprecated
	public static List<String> cutString(String s, ChatFormatting defaultColor, ChatFormatting highlightColor,
		int indent) {
		String markedUp = s.replaceAll("_([^_]+)_", highlightColor + "$1" + defaultColor);

		List<String> words = new LinkedList<>();
		BreakIterator iterator = BreakIterator.getLineInstance(Minecraft.getInstance().getLocale());
		iterator.setText(markedUp);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
			String word = markedUp.substring(start, end);
			words.add(word);
		}

		Font font = Minecraft.getInstance().font;
		List<String> lines = ClientFontHelper.cutString(font, markedUp, maxWidthPerLine);

		String lineStart = Strings.repeat(" ", indent);
		List<String> formattedLines = new ArrayList<>(lines.size());
		String format = defaultColor.toString();
		for (String line : lines) {
			String formattedLine = format + lineStart + line;
			formattedLines.add(formattedLine);
		}
		return formattedLines;
	}

	public static List<Component> cutStringTextComponent(String c, ChatFormatting defaultColor,
		ChatFormatting highlightColor) {
		return cutTextComponent(Components.literal(c), defaultColor, highlightColor, 0);
	}

	public static List<Component> cutTextComponent(Component c, ChatFormatting defaultColor,
		ChatFormatting highlightColor) {
		return cutTextComponent(c, defaultColor, highlightColor, 0);
	}

	public static List<Component> cutStringTextComponent(String c, ChatFormatting defaultColor,
		ChatFormatting highlightColor, int indent) {
		return cutTextComponent(Components.literal(c), defaultColor, highlightColor, indent);
	}

	public static List<Component> cutTextComponent(Component c, ChatFormatting defaultColor,
		ChatFormatting highlightColor, int indent) {
		String s = c.getString();

		String markedUp = s;

		List<String> words = new LinkedList<>();
		BreakIterator iterator = BreakIterator.getLineInstance(Minecraft.getInstance().getLocale());
		iterator.setText(markedUp);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
			String word = markedUp.substring(start, end);
			words.add(word);
		}

		Font font = Minecraft.getInstance().font;
		List<String> lines = new LinkedList<>();
		StringBuilder currentLine = new StringBuilder();
		int width = 0;
		for (String word : words) {
			int newWidth = font.width(word.replaceAll("_", ""));
			if (width + newWidth > maxWidthPerLine) {
				if (width > 0) {
					String line = currentLine.toString();
					lines.add(line);
					currentLine = new StringBuilder();
					width = 0;
				} else {
					lines.add(word);
					continue;
				}
			}
			currentLine.append(word);
			width += newWidth;
		}
		if (width > 0) {
			lines.add(currentLine.toString());
		}

		MutableComponent lineStart = Components.literal(Strings.repeat(" ", indent));
		lineStart.withStyle(defaultColor);
		List<Component> formattedLines = new ArrayList<>(lines.size());
		Couple<ChatFormatting> f = Couple.create(highlightColor, defaultColor);

		boolean currentlyHighlighted = false;
		for (String string : lines) {
			MutableComponent currentComponent = lineStart.plainCopy();
			String[] split = string.split("_");
			for (String part : split) {
				currentComponent.append(Components.literal(part).withStyle(f.get(currentlyHighlighted)));
				currentlyHighlighted = !currentlyHighlighted;
			}

			formattedLines.add(currentComponent);
			currentlyHighlighted = !currentlyHighlighted;
		}

		return formattedLines;
	}

	private static boolean findTooltip(ItemStack stack) {
		String key = getTooltipTranslationKey(stack);
		if (I18n.exists(key)) {
			cachedTooltips.put(key, buildToolTip(key, stack));
			return true;
		}
		cachedTooltips.put(key, ItemDescription.MISSING);
		return false;
	}

	private static ItemDescription buildToolTip(String translationKey, ItemStack stack) {
		ItemDescription tooltip = new ItemDescription(ItemDescription.Palette.Blue);
		String summaryKey = translationKey + ".summary";

		if (I18n.exists(summaryKey))
			tooltip = tooltip.withSummary(Components.literal(I18n.get(summaryKey)));

		for (int i = 1; i < 100; i++) {
			String conditionKey = translationKey + ".condition" + i;
			String behaviourKey = translationKey + ".behaviour" + i;
			if (!I18n.exists(conditionKey))
				break;
			if (i == 1)
				tooltip.getLinesOnShift()
					.add(Components.immutableEmpty());
			tooltip.withBehaviour(I18n.get(conditionKey), I18n.get(behaviourKey));
		}

		for (int i = 1; i < 100; i++) {
			String controlKey = translationKey + ".control" + i;
			String actionKey = translationKey + ".action" + i;
			if (!I18n.exists(controlKey))
				break;
			tooltip.withControl(I18n.get(controlKey), I18n.get(actionKey));
		}

		return tooltip.createTabs();
	}

	public static String getTooltipTranslationKey(ItemStack stack) {
		Item item = stack.getItem();
		if (tooltipReferrals.containsKey(item))
			return tooltipReferrals.get(item)
				.get() + ".tooltip";
		return item.getDescriptionId(stack) + ".tooltip";
	}
}
