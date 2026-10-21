package net.rasanovum.travellershandbook.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.rasanovum.travellershandbook.TravellersHandbookConfig;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalRequirement;
import net.rasanovum.travellershandbook.networking.JournalNetworkManager;

import java.util.*;

public class JournalScreen extends Screen {
    private static final ResourceLocation CUSTOM_BOOK_TEXTURE =
            TravellersHandbook.id("textures/gui/journal_background.png");
    private static final ResourceLocation JOURNAL_OVERLAY_TEXTURE =
            TravellersHandbook.id("textures/gui/journal_overlay.png");

    private final int imageWidth = 335;
    private final int imageHeight = 220;
    private static final int MAX_TITLE_WIDTH = 88;
    private static final int MAX_PAGE_WIDTH = 100;
    private static final int MAX_CATEGORY_WIDTH = 86;
    private static final int MAX_RUNE_LINE_WIDTH = 94;
    private static final int MAX_TITLE_LINES = 2;
    private static final int RIGHT_PAGE_CONTENT_BOTTOM = 188;
    private static final int TRAILING_SPACER_LINES = 1;
    private static final char[] RUNES = {
            'a', 'b', 'c', 'd', 'e', 'g', 'h', 'j', 'k', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'y', 'z'
    };
    private static final String[] CIPHER_CONSONANTS = {
            "b", "c", "d", "f", "g", "h", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w", "z"
    };
    private static final String[] CIPHER_VOWELS = {"a", "e", "i", "o", "u"};
    private static final String[] OBFUSCATED_TITLE_WORDS = {
            "ancient", "cipher", "forgotten", "hidden", "mystery", "runes", "sealed", "veiled"
    };
    private int leftPos;
    private int topPos;

    private final Map<String, List<JournalNetworkManager.SyncEntry>> groupedEntries = new LinkedHashMap<>();
    private final Set<String> expandedCategories = new HashSet<>();
    private final Set<String> expandedSubcategories = new HashSet<>();
    private final List<UiRow> visibleRows = new ArrayList<>();
    private final List<Integer> visibleRowOffsets = new ArrayList<>();
    private int selectedRowIndex = -1;
    private String selectedEntryId;
    private String selectedGroupKey;
    private boolean hasInitializedGroups;
    private boolean rowsHideLockedCategories;
    private Language rowsLanguage;
    private Font rowsFont;

    private double leftPageScrollAmount = 0;
    private final int maxVisibleRowsOnLeftPage = 14;
    private int totalLeftContentHeight;

    private double rightPageScrollAmount = 0;
    private int rightContentHeight;
    private int rightViewportHeight;

    public JournalScreen() {
        super(Component.translatable("gui.travellershandbook.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        this.groupData();
        this.rebuildVisibleRows();
    }

    private void groupData() {
        groupedEntries.clear();
        for (JournalNetworkManager.SyncEntry entry : TravellersHandbookClient.CLIENT_ENTRIES_CACHE) {
            groupedEntries.computeIfAbsent(entry.category(), k -> new ArrayList<>()).add(entry);
        }
        if (!groupedEntries.isEmpty() && !hasInitializedGroups) {
            expandedCategories.add(groupedEntries.keySet().iterator().next());
            for (List<JournalNetworkManager.SyncEntry> entries : groupedEntries.values()) {
                for (JournalNetworkManager.SyncEntry entry : entries) {
                    if (!entry.subcategory().isEmpty()) {
                        expandedSubcategories.add(groupKey(entry.category(), entry.subcategory()));
                    }
                }
            }
            hasInitializedGroups = true;
        }
    }

    private void rebuildVisibleRows() {
        rowsHideLockedCategories = TravellersHandbookConfig.hideLockedCategories;
        rowsLanguage = Language.getInstance();
        rowsFont = this.font;
        visibleRows.clear();
        visibleRowOffsets.clear();
        totalLeftContentHeight = 0;

        for (Map.Entry<String, List<JournalNetworkManager.SyncEntry>> group : groupedEntries.entrySet()) {
            String categoryName = group.getKey();
            if (rowsHideLockedCategories && shouldHideCategory(group.getValue())) {
                continue;
            }
            boolean isExpanded = expandedCategories.contains(categoryName);

            String categoryLabel = categoryName;
            boolean truncated = false;
            while (this.font.width(Component.literal(categoryLabel).withStyle(net.minecraft.ChatFormatting.BOLD)) > MAX_CATEGORY_WIDTH
                    && categoryLabel.length() > 1) {
                categoryLabel = categoryLabel.substring(0, categoryLabel.length() - 1);
                truncated = true;
            }
            String ellipsis = truncated ? "..." : "";
            while (truncated && this.font.width(Component.literal(categoryLabel + ellipsis)
                    .withStyle(net.minecraft.ChatFormatting.BOLD)) > MAX_CATEGORY_WIDTH && categoryLabel.length() > 1) {
                categoryLabel = categoryLabel.substring(0, categoryLabel.length() - 1);
            }
            String headerText = categoryLabel + ellipsis;
            Component headerComponent = Component.literal(headerText).withStyle(net.minecraft.ChatFormatting.BOLD);
            FormattedCharSequence headerVisualText = headerComponent.getVisualOrderText();

            addRow(new UiRow(categoryName, "", null, isExpanded, headerVisualText, 10));

            if (isExpanded) {
                LinkedHashMap<String, List<JournalNetworkManager.SyncEntry>> subgroups = new LinkedHashMap<>();
                for (JournalNetworkManager.SyncEntry entry : group.getValue()) {
                    subgroups.computeIfAbsent(entry.subcategory(), ignored -> new ArrayList<>()).add(entry);
                }
                for (Map.Entry<String, List<JournalNetworkManager.SyncEntry>> subgroup : subgroups.entrySet()) {
                    String subcategoryName = subgroup.getKey();
                    String subgroupKey = groupKey(categoryName, subcategoryName);
                    if (!subcategoryName.isEmpty()) {
                        boolean subcategoryExpanded = expandedSubcategories.contains(subgroupKey);
                        String subcategoryLabel = truncateHeaderLabel(subcategoryName, MAX_CATEGORY_WIDTH - 12);
                        Component subcategoryComponent = Component.literal(subcategoryLabel).withStyle(net.minecraft.ChatFormatting.BOLD);
                        addRow(new UiRow(categoryName, subcategoryName, null, subcategoryExpanded,
                                subcategoryComponent.getVisualOrderText(), 10));
                        if (!subcategoryExpanded) {
                            continue;
                        }
                    }
                    for (JournalNetworkManager.SyncEntry entry : subgroup.getValue()) {
                        addEntryRows(categoryName, subcategoryName, entry);
                    }
                }
            }
        }

        if (selectedEntryId != null) {
            selectedRowIndex = -1;
            for (int i = 0; i < visibleRows.size(); i++) {
                UiRow row = visibleRows.get(i);
                if (!row.isHeader && row.entry.id().equals(selectedEntryId)) {
                    selectedRowIndex = i;
                    break;
                }
            }
            if (selectedRowIndex < 0) {
                selectedEntryId = null;
            }
        }
        if (selectedEntryId == null && selectedGroupKey != null) {
            selectedRowIndex = -1;
            for (int i = 0; i < visibleRows.size(); i++) {
                UiRow row = visibleRows.get(i);
                if (row.isHeader && row.groupKey().equals(selectedGroupKey)) {
                    selectedRowIndex = i;
                    break;
                }
            }
            if (selectedRowIndex < 0) {
                selectedGroupKey = null;
            }
        }
    }

    private void addEntryRows(String categoryName, String subcategoryName, JournalNetworkManager.SyncEntry entry) {
        boolean isLocked = !entry.unlocked();
        int titleWidth = subcategoryName.isEmpty() ? MAX_TITLE_WIDTH : MAX_TITLE_WIDTH - 12;
        List<FormattedCharSequence> splitTitleLines;
        if (isLocked) {
            String originalTitle = entry.title();
            if (originalTitle.length() > 16) {
                originalTitle = originalTitle.substring(0, 13) + "...";
            }
            Component singleLineComponent = Component.literal(originalTitle).withStyle(style -> style.withFont(Minecraft.ALT_FONT));
            splitTitleLines = List.of(singleLineComponent.getVisualOrderText());
        } else {
            splitTitleLines = this.font.split(Component.literal(entry.title()), titleWidth);
            if (splitTitleLines.isEmpty()) {
                return;
            }
        }
        for (int i = 0; i < splitTitleLines.size(); i++) {
            UiRow row = new UiRow(categoryName, subcategoryName, entry, false, splitTitleLines.get(i), 10);
            row.isTitleExtension = i > 0;
            if (i == splitTitleLines.size() - 1) {
                row.height += 2;
            }
            addRow(row);
        }
    }

    private void addRow(UiRow row) {
        visibleRowOffsets.add(totalLeftContentHeight);
        visibleRows.add(row);
        totalLeftContentHeight += row.height;
    }

    private String groupKey(String categoryName, String subcategoryName) {
        return categoryName + "\u0000" + subcategoryName;
    }

    private String truncateHeaderLabel(String label, int maxWidth) {
        String visible = label;
        boolean truncated = false;
        while (this.font.width(Component.literal(visible).withStyle(net.minecraft.ChatFormatting.BOLD)) > maxWidth
                && visible.length() > 1) {
            visible = visible.substring(0, visible.length() - 1);
            truncated = true;
        }
        if (!truncated) {
            return visible;
        }
        while (this.font.width(Component.literal(visible + "...").withStyle(net.minecraft.ChatFormatting.BOLD)) > maxWidth
                && visible.length() > 1) {
            visible = visible.substring(0, visible.length() - 1);
        }
        return visible + "...";
    }

    private boolean shouldHideCategory(List<JournalNetworkManager.SyncEntry> entries) {
        return !entries.isEmpty() && entries.stream()
                .allMatch(entry -> entry.hasOnlyLocks() && !entry.unlocked());
    }

    public void refreshEntries() {
        groupData();
        rebuildVisibleRows();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (rowsHideLockedCategories != TravellersHandbookConfig.hideLockedCategories
                || rowsLanguage != Language.getInstance() || rowsFont != this.font) {
            this.rebuildVisibleRows();
        }
        int hoveredRowIndex = -1;

        RenderSystem.setShaderTexture(0, CUSTOM_BOOK_TEXTURE);
        guiGraphics.blit(CUSTOM_BOOK_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);


        int indexLeftX = this.leftPos + 54;
        int indexRowStartY = this.topPos + 52;
        int leftPageX = this.leftPos + 54; //20
        int leftPageY = this.topPos + 38; //18
        Component indexHeader = Component.translatable("gui.travellershandbook.index").withStyle(net.minecraft.ChatFormatting.UNDERLINE);
        Component noEntries = Component.translatable("gui.travellershandbook.no_entries");
        int leftPageTitleX = leftPageX + 10;
        guiGraphics.drawString(this.font, indexHeader, leftPageTitleX, leftPageY, 0xFF222222, false);
        leftPageY += 14;

        if (visibleRows.isEmpty()) {
            guiGraphics.drawString(this.font, noEntries, leftPageX, leftPageY, 0xFF555555, false);
        } else {
            int leftViewportHeight = maxVisibleRowsOnLeftPage * 10;
            int maxLeftScroll = Math.max(0, totalLeftContentHeight - leftViewportHeight);
            this.leftPageScrollAmount = Mth.clamp(this.leftPageScrollAmount, 0, maxLeftScroll);

            if (mouseX >= indexLeftX && mouseX <= this.leftPos + 154) {
                for (int rowIndex = 0; rowIndex < visibleRows.size(); rowIndex++) {
                    int rowTop = indexRowStartY + visibleRowOffsets.get(rowIndex) - (int) this.leftPageScrollAmount;
                    UiRow row = visibleRows.get(rowIndex);
                    if (mouseY >= rowTop && mouseY < rowTop + 10
                            && mouseY >= indexRowStartY && mouseY < indexRowStartY + leftViewportHeight
                            && rowTop < indexRowStartY + leftViewportHeight
                            && rowTop + row.height > indexRowStartY) {
                        hoveredRowIndex = rowIndex;
                        break;
                    }
                }
            }

            guiGraphics.enableScissor(indexLeftX, indexRowStartY, this.leftPos + 154, indexRowStartY + leftViewportHeight);
            for (int rowIndex = 0; rowIndex < visibleRows.size(); rowIndex++) {
                UiRow row = visibleRows.get(rowIndex);
                int currentY = indexRowStartY + visibleRowOffsets.get(rowIndex) - (int) this.leftPageScrollAmount;
                if (currentY + row.height <= indexRowStartY || currentY >= indexRowStartY + leftViewportHeight) {
                    continue;
                }

                boolean isHighlighted = false;
                if (hoveredRowIndex >= 0 && hoveredRowIndex < visibleRows.size()) {
                    UiRow hoveredRow = visibleRows.get(hoveredRowIndex);
                    if (row.isHeader && hoveredRow.isHeader) {
                        isHighlighted = row.groupKey().equals(hoveredRow.groupKey());
                    } else if (!row.isHeader && !hoveredRow.isHeader) {
                        isHighlighted = row.entry.id().equals(hoveredRow.entry.id());
                    }
                }

                boolean isSelected = false;
                if (selectedRowIndex >= 0 && selectedRowIndex < visibleRows.size()) {
                    UiRow selectedRow = visibleRows.get(selectedRowIndex);
                    if (!row.isHeader && !selectedRow.isHeader) {
                        isSelected = row.entry.id().equals(selectedRow.entry.id());
                    }
                }

                int color = isHighlighted ? 0xFF007700 : (row.isHeader ? 0xFF444444 : 0xFF666666);

                if (row.isHeader) {
                    int headerX = row.isSubcategoryHeader ? leftPageX + 12 : leftPageX;
                    guiGraphics.drawString(this.font, row.renderedTextLine, headerX, currentY, color, false);
                    if (!row.isSubcategoryHeader) {
                        boolean categoryComplete = categoryComplete(row.categoryName);
                        boolean categoryHasUnread = groupedEntries.get(row.categoryName).stream()
                                .anyMatch(entry -> TravellersHandbookClient.UNREAD_ENTRIES_SET.contains(entry.id()));

                        if (categoryComplete) {
                            drawCheckMarker(guiGraphics, this.leftPos + 145, currentY + 2);
                        } else if (categoryHasUnread) {
                            guiGraphics.drawString(this.font, "§l!", this.leftPos + 145, currentY, 0xFFE69138, false);
                        }
                    }
                } else {
                    int indentX = leftPageX + (row.subcategoryName.isEmpty() ? 12 : 24);
                    boolean isUnread = TravellersHandbookClient.UNREAD_ENTRIES_SET.contains(row.entry.id());

                    if (!row.isTitleExtension) {
                        int markerX = indentX - 10;
                        if (isSelected) {
                            guiGraphics.fill(markerX + 1, currentY + 2, markerX + 2, currentY + 7, 0xFF007700);
                            guiGraphics.fill(markerX + 2, currentY + 3, markerX + 3, currentY + 6, 0xFF007700);
                            guiGraphics.fill(markerX + 3, currentY + 4, markerX + 4, currentY + 5, 0xFF007700);
                        } else if (row.entry.hasGoals() && row.entry.unlocked()) {
                            if (row.entry.completed()) {
                                drawCheckMarker(guiGraphics, markerX, currentY + 2);
                            } else {
                                drawEmptyCircleMarker(guiGraphics, markerX, currentY + 2);
                            }
                        } else if (isUnread) {
                            guiGraphics.drawString(this.font, "§l!", indentX - 6, currentY, 0xFFE69138, false);
                        }
                    }

                    if (!row.entry.unlocked()) {

                        TextExtractor extractor = new TextExtractor();
                        row.renderedTextLine.accept(extractor);
                        String currentLineText = extractor.getText();

                        String scrambledRuneLine = scrambleToRunes(currentLineText, rowIndex * 73L, this.minecraft.player.tickCount);

                        int runeLineWidth = row.subcategoryName.isEmpty() ? MAX_TITLE_WIDTH : MAX_TITLE_WIDTH - 12;
                        guiGraphics.drawString(this.font, fitRuneLine(scrambledRuneLine, runeLineWidth), indentX, currentY, 0xFF887766, false);

                    } else {
                        guiGraphics.drawString(this.font, row.renderedTextLine, indentX, currentY, color, false);
                    }
                }
            }
            guiGraphics.disableScissor();

            if (totalLeftContentHeight > leftViewportHeight) {
                int leftBarX = this.leftPos + 156;
                int leftBarTrackY = this.topPos + 52;
                int leftBarTrackHeight = leftViewportHeight;
                int thumbHeight = Math.max(8, (int) ((double) leftViewportHeight / totalLeftContentHeight * leftBarTrackHeight));
                int thumbYOffset = maxLeftScroll == 0 ? 0 : (int) ((this.leftPageScrollAmount / maxLeftScroll) * (leftBarTrackHeight - thumbHeight));
                guiGraphics.fill(leftBarX, leftBarTrackY + thumbYOffset, leftBarX + 2, leftBarTrackY + thumbYOffset + thumbHeight, 0x88444444);
            }
        }

        // render right page
        int rightPageX = this.leftPos + 180; //146 182
        int rightPageY = this.topPos + 38; //18
        this.rightContentHeight = 0;
        this.rightViewportHeight = 0;

        if (selectedRowIndex >= 0 && selectedRowIndex < visibleRows.size() && !visibleRows.isEmpty()) {
            UiRow activeRow = visibleRows.get(selectedRowIndex);

            if (!activeRow.isHeader) {
                JournalNetworkManager.SyncEntry activeEntry = activeRow.entry;

                boolean isLockedTitle = !activeEntry.unlocked();
                String titleToWrap = isLockedTitle ? createObfuscatedTitle(activeEntry) : activeEntry.title();
                int titleWidth = isLockedTitle ? MAX_RUNE_LINE_WIDTH : MAX_PAGE_WIDTH;
                Component titleComponent = isLockedTitle
                        ? Component.literal(titleToWrap).withStyle(style -> style.withFont(Minecraft.ALT_FONT))
                        : Component.literal(titleToWrap);
                List<FormattedCharSequence> wrappedTitleLines = this.font.split(titleComponent, titleWidth);
                int titleLineCount = Math.min(wrappedTitleLines.size(), MAX_TITLE_LINES);
                long entrySeed = getEntrySeed(activeEntry);
                for (int i = 0; i < titleLineCount; i++) {
                    FormattedCharSequence titleLine = wrappedTitleLines.get(i);
                    if (isLockedTitle) {
                        TextExtractor extractor = new TextExtractor();
                        titleLine.accept(extractor);
                        String runeText = scrambleToRunes(extractor.getText(), entrySeed + i * 31L, this.minecraft.player.tickCount);
                        titleLine = fitRuneLine(runeText, MAX_PAGE_WIDTH);
                    }
                    guiGraphics.drawString(this.font, titleLine, rightPageX, rightPageY + i * 9, isLockedTitle ? 0xFF887766 : 0xFF111111, false);
                }
                rightPageY += titleLineCount * 9 + 2;

                String groupLabel = activeEntry.category() + (activeEntry.subcategory().isEmpty()
                        ? "" : " / " + activeEntry.subcategory());
                List<FormattedCharSequence> groupLines = this.font.split(
                        Component.literal(groupLabel).withStyle(net.minecraft.ChatFormatting.ITALIC), MAX_PAGE_WIDTH);
                int groupLineCount = Math.min(2, groupLines.size());
                for (int i = 0; i < groupLineCount; i++) {
                    guiGraphics.drawString(this.font, groupLines.get(i), rightPageX, rightPageY + i * 9, 0xFF666666, false);
                }
                rightPageY += groupLineCount * 9 + 4;

                boolean isRightPageLocked = !activeEntry.unlocked();
                Component bodyText = Component.literal(activeEntry.description());
                if (isRightPageLocked) {
                    bodyText = Component.literal(createObfuscatedBody(activeEntry))
                            .withStyle(style -> style.withFont(Minecraft.ALT_FONT));
                }

                int dynamicWrapWidth = isRightPageLocked ? MAX_RUNE_LINE_WIDTH : MAX_PAGE_WIDTH;
                List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(bodyText, dynamicWrapWidth));
                if (!isRightPageLocked) {
                    lines.add(Component.literal("").getVisualOrderText());
                    for (JournalNetworkManager.RequirementProgress progress : activeEntry.requirements()) {
                        JournalRequirement requirement = progress.requirement();
                        Component progressText = Component.translatable(requirementTranslationKey(requirement),
                                requirementName(requirement), progress.progress(), requirement.amount());
                        lines.addAll(this.font.split(progressText, MAX_PAGE_WIDTH));
                    }
                }
                this.rightContentHeight = (lines.size() + TRAILING_SPACER_LINES) * 9;
                this.rightViewportHeight = Math.max(0, this.topPos + RIGHT_PAGE_CONTENT_BOTTOM - rightPageY);
                int maxRightScroll = Math.max(0, rightContentHeight - rightViewportHeight);
                this.rightPageScrollAmount = Mth.clamp(this.rightPageScrollAmount, 0, maxRightScroll);
                int scrollOffset = (int) this.rightPageScrollAmount;
                guiGraphics.enableScissor(rightPageX, rightPageY, rightPageX + MAX_PAGE_WIDTH,
                        rightPageY + rightViewportHeight);

                for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                    int currentLineY = rightPageY + lineIndex * 9 - scrollOffset;
                    if (currentLineY + 9 <= rightPageY || currentLineY >= rightPageY + rightViewportHeight) {
                        continue;
                    }
                    if (isRightPageLocked) {
                        TextExtractor extractor = new TextExtractor();
                        lines.get(lineIndex).accept(extractor);
                        String rawLineText = extractor.getText();

                        String randomizedLine = scrambleToRunes(rawLineText, getEntrySeed(activeEntry) + lineIndex * 117L, this.minecraft.player.tickCount);

                        guiGraphics.drawString(this.font, fitRuneLine(randomizedLine, MAX_PAGE_WIDTH), rightPageX, currentLineY, 0xFF554433, false);
                    } else {
                        guiGraphics.drawString(this.font, lines.get(lineIndex), rightPageX, currentLineY, 0xFF333333, false);
                    }
                }

                guiGraphics.disableScissor();
                if (maxRightScroll > 0 && rightViewportHeight > 0) {
                    int barX = this.leftPos + 288; //252
                    int barTrackY = rightPageY;
                    int barTrackHeight = rightViewportHeight;
                    int thumbHeight = Math.min(barTrackHeight,
                            Math.max(8, (int) ((double) rightViewportHeight / rightContentHeight * barTrackHeight)));
                    int thumbYOffset = (int) ((this.rightPageScrollAmount / maxRightScroll) * (barTrackHeight - thumbHeight));
                    guiGraphics.fill(barX, barTrackY + thumbYOffset, barX + 2, barTrackY + thumbYOffset + thumbHeight, 0x88444444);
                }
            } else {
                this.rightContentHeight = 0;
                this.rightViewportHeight = 0;
                String headerText = activeRow.isSubcategoryHeader
                        ? activeRow.categoryName + " / " + activeRow.subcategoryName
                        : activeRow.categoryName;
                Component categoryHeader = Component.literal(headerText).withStyle(net.minecraft.ChatFormatting.BOLD);
                List<FormattedCharSequence> headerLines = this.font.split(categoryHeader, MAX_PAGE_WIDTH);
                int headerLineCount = Math.min(3, headerLines.size());
                for (int i = 0; i < headerLineCount; i++) {
                    guiGraphics.drawString(this.font, headerLines.get(i), rightPageX, rightPageY + i * 9, 0xFF111111, false);
                }
                rightPageY += headerLineCount * 9 + 6;

                String helpKey = activeRow.isSubcategoryHeader
                        ? (activeRow.isExpanded ? "gui.travellershandbook.help.subcategory_expanded" : "gui.travellershandbook.help.subcategory_collapsed")
                        : (activeRow.isExpanded ? "gui.travellershandbook.help.expanded" : "gui.travellershandbook.help.collapsed");
                Component helperMsg = Component.translatable(helpKey);

                List<FormattedCharSequence> lines = this.font.split(helperMsg, MAX_PAGE_WIDTH);
                for (int i = 0; i < lines.size(); i++) {
                    guiGraphics.drawString(this.font, lines.get(i), rightPageX, rightPageY + (i * 9), 0xFF777777, false);
                }
            }
        }

        renderJournalOverlay(guiGraphics);
    }

    private void renderJournalOverlay(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 800.0F);
        blitWithAlpha(guiGraphics, JOURNAL_OVERLAY_TEXTURE, this.leftPos, this.topPos);
        guiGraphics.pose().popPose();
    }

    private void blitWithAlpha(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        guiGraphics.flush();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int leftPageX = this.leftPos + 54;
            int rowStartY = this.topPos + 52;

            if (mouseX >= leftPageX && mouseX <= this.leftPos + 154) {
                for (int actualDataIndex = 0; actualDataIndex < visibleRows.size(); actualDataIndex++) {
                    UiRow clickedRow = visibleRows.get(actualDataIndex);
                    int rowTop = rowStartY + visibleRowOffsets.get(actualDataIndex) - (int) this.leftPageScrollAmount;
                    int rowBottom = rowTop + clickedRow.height;

                    if (mouseY >= rowTop && mouseY < rowTop + 10
                            && mouseY >= rowStartY && mouseY < rowStartY + maxVisibleRowsOnLeftPage * 10
                            && rowTop < rowStartY + maxVisibleRowsOnLeftPage * 10
                            && rowBottom > rowStartY) {
                        this.rightPageScrollAmount = 0;

                        if (clickedRow.isHeader) {
                            if (clickedRow.isSubcategoryHeader) {
                                if (expandedSubcategories.contains(clickedRow.groupKey())) {
                                    expandedSubcategories.remove(clickedRow.groupKey());
                                } else {
                                    expandedSubcategories.add(clickedRow.groupKey());
                                }
                            } else if (expandedCategories.contains(clickedRow.categoryName)) {
                                expandedCategories.remove(clickedRow.categoryName);
                            } else {
                                expandedCategories.add(clickedRow.categoryName);
                            }
                            this.selectedGroupKey = clickedRow.groupKey();
                            this.selectedEntryId = null;
                            rebuildVisibleRows();
                        } else if (this.selectedEntryId != null && clickedRow.entry.id().equals(this.selectedEntryId)) {
                            this.selectedRowIndex = -1;
                            this.selectedEntryId = null;
                            this.selectedGroupKey = null;
                        } else {
                            this.selectedRowIndex = actualDataIndex;
                            this.selectedEntryId = clickedRow.entry.id();
                            this.selectedGroupKey = clickedRow.groupKey();
                            TravellersHandbookClient.UNREAD_ENTRIES_SET.remove(clickedRow.entry.id());
                        }

                        this.minecraft.getSoundManager().play(
                                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                        net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 0.85F
                                )
                        );
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    //? if <1.21 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double delta = verticalAmount;
    //?}
        int centerCrease = this.leftPos + 165; //135

        if (mouseX >= this.leftPos && mouseX < centerCrease) {
            int leftViewportHeight = maxVisibleRowsOnLeftPage * 10;
            int maxLeftScroll = Math.max(0, totalLeftContentHeight - leftViewportHeight);
            if (maxLeftScroll > 0) {
                this.leftPageScrollAmount = Mth.clamp(this.leftPageScrollAmount - (delta * 12), 0, maxLeftScroll);
                return true;
            }
        } else if (mouseX >= centerCrease && mouseX <= this.leftPos + this.imageWidth) {
            int maxRightScroll = Math.max(0, rightContentHeight - rightViewportHeight);
            if (maxRightScroll > 0) {
                this.rightPageScrollAmount = Mth.clamp(this.rightPageScrollAmount - (delta * 12), 0, maxRightScroll);
                return true;
            }
        }
        //? if <1.21 {
        /*return super.mouseScrolled(mouseX, mouseY, delta);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        //?}
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyMapping openKeyBind = TravellersHandbookClient.getOpenJournalKey();
        if (openKeyBind != null && openKeyBind.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean categoryComplete(String categoryName) {
        int requirements = 0;
        int completed = 0;
        for (JournalNetworkManager.SyncEntry entry : groupedEntries.get(categoryName)) {
            if (entry.hasRequirements()) {
                requirements++;
                if (entry.completed()) {
                    completed++;
                }
            }
        }
        return requirements > 0 && requirements == completed;
    }

    private void drawEmptyCircleMarker(GuiGraphics guiGraphics, int x, int y) {
        int color = 0xFF888888;
        guiGraphics.fill(x + 1, y, x + 5, y + 1, color);
        guiGraphics.fill(x, y + 1, x + 1, y + 5, color);
        guiGraphics.fill(x + 5, y + 1, x + 6, y + 5, color);
        guiGraphics.fill(x + 1, y + 5, x + 5, y + 6, color);
    }

    private void drawCheckMarker(GuiGraphics guiGraphics, int x, int y) {
        int color = 0xFF3FA34D;
        guiGraphics.fill(x, y + 3, x + 2, y + 5, color);
        guiGraphics.fill(x + 1, y + 4, x + 3, y + 6, color);
        guiGraphics.fill(x + 2, y + 3, x + 4, y + 5, color);
        guiGraphics.fill(x + 3, y + 2, x + 5, y + 4, color);
        guiGraphics.fill(x + 4, y + 1, x + 6, y + 3, color);
    }

    private static class UiRow {
        public final String categoryName;
        public final String subcategoryName;
        public final JournalNetworkManager.SyncEntry entry;
        public final boolean isHeader;
        public final boolean isSubcategoryHeader;
        public final boolean isExpanded;
        public int height;

        public final FormattedCharSequence renderedTextLine;
        public boolean isTitleExtension = false;

        public UiRow(String categoryName, String subcategoryName, JournalNetworkManager.SyncEntry entry,
                     boolean isExpanded, FormattedCharSequence renderedTextLine, int height) {
            this.categoryName = categoryName;
            this.subcategoryName = subcategoryName;
            this.entry = entry;
            this.isHeader = (entry == null);
            this.isSubcategoryHeader = this.isHeader && !subcategoryName.isEmpty();
            this.isExpanded = isExpanded;
            this.renderedTextLine = renderedTextLine;
            this.height = height;
        }

        public String groupKey() {
            return subcategoryName.isEmpty() ? categoryName : categoryName + "\u0000" + subcategoryName;
        }
    }

    private FormattedCharSequence fitRuneLine(String text, int maxWidth) {
        Component runeLine = Component.literal(text).withStyle(style -> style.withFont(Minecraft.ALT_FONT));
        return Component.literal(this.font.substrByWidth(runeLine, maxWidth).getString())
                .setStyle(runeLine.getStyle()).getVisualOrderText();
    }

    private String scrambleToRunes(String text, long baseSeed, long currentTick) {
        StringBuilder scrambled = new StringBuilder();

        long animatedTick = TravellersHandbookConfig.reduceRuneMotion ? 0L : currentTick / 2L;

        Random random = new Random(0L);

        for (int pos = 0; pos < text.length(); pos++) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '.' || c == ',' || c == '!' || c == '?') {
                scrambled.append(' ');
            } else {
                long uniqueCharSeed = baseSeed + (pos * 19L) + animatedTick;
                random.setSeed(uniqueCharSeed);

                char rune = RUNES[random.nextInt(RUNES.length)];
                scrambled.append(rune);
            }
        }

        return scrambled.toString();
    }

    private String createObfuscatedBody(JournalNetworkManager.SyncEntry entry) {
        Random random = new Random(getEntrySeed(entry) ^ 0x5DEECE66DL);
        int targetLength = 190 + random.nextInt(61);
        StringBuilder text = new StringBuilder();
        int wordsInSentence = 0;
        while (text.length() < targetLength) {
            if (!text.isEmpty()) text.append(' ');
            text.append(createCipherWord(random));
            wordsInSentence++;
            if (wordsInSentence >= 4 + random.nextInt(7)) {
                text.append('.');
                wordsInSentence = 0;
            }
        }
        return text.toString();
    }

    private String createCipherWord(Random random) {
        // most words are short enough for several to share a rune-font line with occasional longer words being placed in to appear more natural
        int length = random.nextInt(10) == 0 ? 6 + random.nextInt(2) : 2 + random.nextInt(4);
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String[] letters = i % 2 == 0 ? CIPHER_CONSONANTS : CIPHER_VOWELS;
            word.append(letters[random.nextInt(letters.length)]);
        }
        return word.toString();
    }

    private String createObfuscatedTitle(JournalNetworkManager.SyncEntry entry) {
        Random random = new Random(getEntrySeed(entry));
        return OBFUSCATED_TITLE_WORDS[random.nextInt(OBFUSCATED_TITLE_WORDS.length)] + " "
                + OBFUSCATED_TITLE_WORDS[random.nextInt(OBFUSCATED_TITLE_WORDS.length)];
    }

    private long getEntrySeed(JournalNetworkManager.SyncEntry entry) {
        return Objects.hash(entry.id());
    }

    private String requirementTranslationKey(JournalRequirement requirement) {
        if (!requirement.displayName().isEmpty()) {
            return "gui.travellershandbook.requirement.named_progress";
        }
        return switch (requirement.type()) {
            case ADVANCEMENT -> "gui.travellershandbook.requirement.advancement_progress";
            case ITEM -> "gui.travellershandbook.requirement.item_progress";
            case STRUCTURE -> "gui.travellershandbook.requirement.structure_progress";
            case ENTRY -> "gui.travellershandbook.requirement.entry_progress";
            case MINE -> "gui.travellershandbook.requirement.mine_progress";
            case CRAFT -> "gui.travellershandbook.requirement.craft_progress";
        };
    }

    private Component requirementName(JournalRequirement requirement) {
        if (!requirement.displayName().isEmpty()) {
            return Component.literal(requirement.displayName());
        }

        if (requirement.type() == JournalRequirement.Type.ENTRY) {
            return TravellersHandbookClient.CLIENT_ENTRIES_CACHE.stream()
                    .filter(entry -> entry.id().equals(requirement.id()))
                    .map(JournalNetworkManager.SyncEntry::title)
                    .findFirst()
                    .map(Component::literal)
                    .orElseGet(() -> Component.literal(requirement.id()));
        }

        if (requirement.type() == JournalRequirement.Type.MINE) {
            if (requirement.id().startsWith("#")) {
                return Component.literal(requirement.id());
            }
            try {
                ResourceLocation id = TravellersHandbook.parseId(requirement.id());
                if (BuiltInRegistries.BLOCK.containsKey(id)) {
                    Block block = BuiltInRegistries.BLOCK.get(id);
                    return block.getName();
                }
            } catch (Exception ignored) {
            }
            return Component.literal(requirement.id());
        }

        if (requirement.type() != JournalRequirement.Type.ITEM
                && requirement.type() != JournalRequirement.Type.CRAFT) {
            return Component.literal(requirement.id());
        }

        try {
            ResourceLocation id = TravellersHandbook.parseId(requirement.id());
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                return Component.literal(requirement.id());
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            return item.getDescription();
        } catch (Exception ignored) {
            return Component.literal(requirement.id());
        }
    }

    private static class TextExtractor implements net.minecraft.util.FormattedCharSink {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public boolean accept(int index, net.minecraft.network.chat.Style style, int codePoint) {
            buffer.appendCodePoint(codePoint);
            return true;
        }

        public String getText() {
            return buffer.toString();
        }
    }
}
