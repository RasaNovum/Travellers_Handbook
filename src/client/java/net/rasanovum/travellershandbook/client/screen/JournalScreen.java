package net.rasanovum.travellershandbook.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
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
    private static final int MAX_TITLE_WIDTH = 90;
    private static final int MAX_PAGE_WIDTH = 100;
    private static final int MAX_CATEGORY_WIDTH = 86;
    private static final int MAX_RUNE_LINE_WIDTH = 94;
    private static final int MAX_TITLE_LINES = 2;
    private static final int RIGHT_PAGE_CONTENT_BOTTOM = 188;
    private static final int TRAILING_SPACER_LINES = 1;
    private int leftPos;
    private int topPos;

    private final Map<String, List<JournalNetworkManager.SyncEntry>> groupedEntries = new LinkedHashMap<>();
    private final Set<String> expandedCategories = new HashSet<>();
    private final List<UiRow> visibleRows = new ArrayList<>();
    private int selectedRowIndex = -1;
    private String selectedEntryId;
    private String selectedCategoryName;
    private boolean hasInitializedGroups;

    private double leftPageScrollAmount = 0;
    private final int maxVisibleRowsOnLeftPage = 14;

    private double rightPageScrollAmount = 0;
    private int totalWrappedLines = 0;
    private int maxVisibleLinesOnPage = 14;

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
            hasInitializedGroups = true;
        }
    }

    private void rebuildVisibleRows() {
        visibleRows.clear();

        for (Map.Entry<String, List<JournalNetworkManager.SyncEntry>> group : groupedEntries.entrySet()) {
            String categoryName = group.getKey();
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

            visibleRows.add(new UiRow(categoryName, null, isExpanded, headerVisualText));

            if (isExpanded) {
                for (JournalNetworkManager.SyncEntry entry : group.getValue()) {
                    boolean isLocked = !entry.unlocked();

                    if (isLocked) {
                        String originalTitle = entry.title();

                        if (originalTitle.length() > 16) {
                            originalTitle = originalTitle.substring(0, 13) + "...";
                        }

                        Component singleLineComponent = Component.literal(originalTitle).withStyle(style -> style.withFont(Minecraft.ALT_FONT));
                        visibleRows.add(new UiRow(categoryName, entry, false, singleLineComponent.getVisualOrderText()));
                    } else {
                        String displayTitleString = entry.title();
                        Component fullTitleText = Component.literal(displayTitleString);
                        List<FormattedCharSequence> splitTitleLines = this.font.split(fullTitleText, MAX_TITLE_WIDTH);
                        if (splitTitleLines.isEmpty()) continue;

                        visibleRows.add(new UiRow(categoryName, entry, false, splitTitleLines.get(0)));

                        for (int i = 1; i < splitTitleLines.size(); i++) {
                            UiRow extensionRow = new UiRow(categoryName, entry, false, splitTitleLines.get(i));
                            extensionRow.isTitleExtension = true;
                            visibleRows.add(extensionRow);
                        }
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
        if (selectedEntryId == null && selectedCategoryName != null) {
            selectedRowIndex = -1;
            for (int i = 0; i < visibleRows.size(); i++) {
                UiRow row = visibleRows.get(i);
                if (row.isHeader && row.categoryName.equals(selectedCategoryName)) {
                    selectedRowIndex = i;
                    break;
                }
            }
            if (selectedRowIndex < 0) {
                selectedCategoryName = null;
            }
        }
    }

    public void refreshEntries() {
        groupData();
        rebuildVisibleRows();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.rebuildVisibleRows();
        int hoveredRowIndex = -1;

        RenderSystem.setShaderTexture(0, CUSTOM_BOOK_TEXTURE);
        guiGraphics.blit(CUSTOM_BOOK_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);


        // render left page
        int indexLeftX = this.leftPos + 54; //24
        int indexRowStartY = this.topPos + 52; //32
        int startRowOffset = (int) this.leftPageScrollAmount;

        if (mouseX >= indexLeftX && mouseX <= this.leftPos + 154) { //124
            for (int i = 0; i < Math.min(visibleRows.size(), maxVisibleRowsOnLeftPage); i++) {
                int rTop = indexRowStartY + (i * 10);
                if (mouseY >= rTop && mouseY <= rTop + 9) {
                    int hoveredIndex = startRowOffset + i;
                    if (hoveredIndex < visibleRows.size()) {
                        hoveredRowIndex = hoveredIndex;
                    }
                }
            }
        }

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
            int leftScrollableRows = visibleRows.size() + TRAILING_SPACER_LINES;
            int maxLeftScrollIndex = Math.max(0, leftScrollableRows - maxVisibleRowsOnLeftPage);
            if (this.leftPageScrollAmount > maxLeftScrollIndex) {
                this.leftPageScrollAmount = maxLeftScrollIndex;
            }

            for (int i = 0; i < Math.min(visibleRows.size(), maxVisibleRowsOnLeftPage); i++) {
                int rowIndex = startRowOffset + i;
                if (rowIndex >= visibleRows.size()) break;

                UiRow row = visibleRows.get(rowIndex);
                int currentY = leftPageY + (i * 10);

                boolean isHighlighted = false;
                if (hoveredRowIndex >= 0 && hoveredRowIndex < visibleRows.size()) {
                    UiRow hoveredRow = visibleRows.get(hoveredRowIndex);
                    if (row.isHeader && hoveredRow.isHeader) {
                        isHighlighted = row.categoryName.equals(hoveredRow.categoryName);
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
                    guiGraphics.drawString(this.font, row.renderedTextLine, leftPageX, currentY, color, false);
                    boolean categoryComplete = categoryComplete(row.categoryName);
                    boolean categoryHasUnread = TravellersHandbookClient.UNREAD_ENTRIES_SET.stream().anyMatch(unreadEntryId ->
                            TravellersHandbookClient.CLIENT_ENTRIES_CACHE.stream().anyMatch(cached ->
                                    cached.id().equals(unreadEntryId) && cached.category().equals(row.categoryName)));

                    if (categoryComplete) {
                        drawCheckMarker(guiGraphics, this.leftPos + 145, currentY + 2);
                    } else if (categoryHasUnread) {
                        guiGraphics.drawString(this.font, "§l!", this.leftPos + 145, currentY, 0xFFE69138, false);
                    }
                } else {
                    int indentX = leftPageX + 12;
                    boolean isUnread = TravellersHandbookClient.UNREAD_ENTRIES_SET.contains(row.entry.id());

                    if (!row.isTitleExtension) {
                        if (isSelected) {
                            guiGraphics.drawString(this.font, ">", leftPageX - 5, currentY, 0xFF007700, false);
                        }
                        if (row.entry.hasGoals() && row.entry.unlocked()) {
                            if (row.entry.completed()) {
                                drawCheckMarker(guiGraphics, leftPageX + 2, currentY + 2);
                            } else {
                                drawEmptyCircleMarker(guiGraphics, leftPageX + 2, currentY + 2);
                            }
                        } else if (isUnread) {
                            guiGraphics.drawString(this.font, "§l!", leftPageX + 6, currentY, 0xFFE69138, false);
                        }
                    }

                    if (!row.entry.unlocked()) {

                        TextExtractor extractor = new TextExtractor();
                        row.renderedTextLine.accept(extractor);
                        String currentLineText = extractor.getText();

                        String scrambledRuneLine = scrambleToRunes(currentLineText, rowIndex * 73L, this.minecraft.player.tickCount);

                        Component finalRuneLine = Component.literal(scrambledRuneLine).withStyle(style -> style.withFont(Minecraft.ALT_FONT));
                        guiGraphics.drawString(this.font, finalRuneLine.getVisualOrderText(), indentX, currentY, 0xFF887766, false);

                    } else {
                        guiGraphics.drawString(this.font, row.renderedTextLine, indentX, currentY, color, false);
                    }
                }
            }

            // left page scrollbar
            if (leftScrollableRows > maxVisibleRowsOnLeftPage) {
                int leftBarX = this.leftPos + 156; //126
                int leftBarTrackY = this.topPos + 52; //32
                int leftBarTrackHeight = maxVisibleRowsOnLeftPage * 10;

                int thumbHeight = Math.max(8, (int) ((double) maxVisibleRowsOnLeftPage / leftScrollableRows * leftBarTrackHeight));
                int thumbYOffset = (int) ((this.leftPageScrollAmount / maxLeftScrollIndex) * (leftBarTrackHeight - thumbHeight));
                guiGraphics.fill(leftBarX, leftBarTrackY + thumbYOffset, leftBarX + 2, leftBarTrackY + thumbYOffset + thumbHeight, 0x88444444);
            }
        }

        // render right page
        int rightPageX = this.leftPos + 180; //146 182
        int rightPageY = this.topPos + 38; //18

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
                        titleLine = Component.literal(runeText).withStyle(style -> style.withFont(Minecraft.ALT_FONT)).getVisualOrderText();
                    }
                    guiGraphics.drawString(this.font, titleLine, rightPageX, rightPageY + i * 9, isLockedTitle ? 0xFF887766 : 0xFF111111, false);
                }
                rightPageY += titleLineCount * 9 + 2;

                guiGraphics.drawString(this.font, "§o" + activeEntry.category(), rightPageX, rightPageY, 0xFF666666, false);
                rightPageY += 13;

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
                this.totalWrappedLines = lines.size() + TRAILING_SPACER_LINES;
                this.maxVisibleLinesOnPage = Math.max(0, (this.topPos + RIGHT_PAGE_CONTENT_BOTTOM - rightPageY) / 9);

                int maxRightScrollIndex = Math.max(0, this.totalWrappedLines - maxVisibleLinesOnPage);
                if (this.rightPageScrollAmount > maxRightScrollIndex) {
                    this.rightPageScrollAmount = maxRightScrollIndex;
                }
                int startLineOffset = (int) this.rightPageScrollAmount;

                for (int i = 0; i < maxVisibleLinesOnPage; i++) {
                    int lineIndex = startLineOffset + i;
                    if (lineIndex >= lines.size()) break;

                    int currentLineY = rightPageY + (i * 9);
                    if (isRightPageLocked) {
                        TextExtractor extractor = new TextExtractor();
                        lines.get(lineIndex).accept(extractor);
                        String rawLineText = extractor.getText();

                        String randomizedLine = scrambleToRunes(rawLineText, getEntrySeed(activeEntry) + lineIndex * 117L, this.minecraft.player.tickCount);

                        Component runeBodyLine = Component.literal(randomizedLine).withStyle(style -> style.withFont(Minecraft.ALT_FONT));
                        guiGraphics.drawString(this.font, runeBodyLine.getVisualOrderText(), rightPageX, currentLineY, 0xFF554433, false);
                    } else {
                        guiGraphics.drawString(this.font, lines.get(lineIndex), rightPageX, currentLineY, 0xFF333333, false);
                    }
                }

                // right page scrollbar
                if (this.totalWrappedLines > maxVisibleLinesOnPage) {
                    int barX = this.leftPos + 288; //252
                    int barTrackY = this.topPos + 62; //42
                    int barTrackHeight = maxVisibleLinesOnPage * 9;

                    int thumbHeight = Math.max(8, (int) ((double) maxVisibleLinesOnPage / this.totalWrappedLines * barTrackHeight));
                    int thumbYOffset = (int) ((this.rightPageScrollAmount / maxRightScrollIndex) * (barTrackHeight - thumbHeight));
                    guiGraphics.fill(barX, barTrackY + thumbYOffset, barX + 2, barTrackY + thumbYOffset + thumbHeight, 0x88444444);
                }
            } else {
                Component categoryHeader = Component.literal(activeRow.categoryName).withStyle(net.minecraft.ChatFormatting.BOLD);
                guiGraphics.drawString(this.font, categoryHeader, rightPageX, rightPageY, 0xFF111111, false);
                rightPageY += 15;

                String helpKey = activeRow.isExpanded ? "gui.travellershandbook.help.expanded" : "gui.travellershandbook.help.collapsed";
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
            int leftPageX = this.leftPos + 54; //24
            int rowStartY = this.topPos + 52; //32
            int startRowOffset = (int) this.leftPageScrollAmount;

            if (mouseX >= leftPageX && mouseX <= this.leftPos + 144) { //124
                for (int i = 0; i < Math.min(visibleRows.size(), maxVisibleRowsOnLeftPage); i++) {
                    int rowTop = rowStartY + (i * 10);
                    int rowBottom = rowTop + 9;

                    if (mouseY >= rowTop && mouseY <= rowBottom) {
                        int actualDataIndex = startRowOffset + i;

                        if (actualDataIndex < visibleRows.size()) {
                            UiRow clickedRow = visibleRows.get(actualDataIndex);
                            this.rightPageScrollAmount = 0;

                            if (clickedRow.isHeader) {
                                if (expandedCategories.contains(clickedRow.categoryName)) {
                                    expandedCategories.remove(clickedRow.categoryName);
                                } else {
                                    expandedCategories.add(clickedRow.categoryName);
                                }
                                this.selectedCategoryName = clickedRow.categoryName;
                                this.selectedEntryId = null;
                                rebuildVisibleRows();
                            } else {
                                if (this.selectedRowIndex == actualDataIndex) {
                                    this.selectedRowIndex = -1;
                                    this.selectedEntryId = null;
                                    this.selectedCategoryName = null;
                                } else {
                                    this.selectedRowIndex = actualDataIndex;

                                    if (clickedRow.entry != null) {
                                        this.selectedEntryId = clickedRow.entry.id();
                                        this.selectedCategoryName = clickedRow.categoryName;
                                        TravellersHandbookClient.UNREAD_ENTRIES_SET.remove(
                                                clickedRow.entry.id()
                                        );
                                    }
                                }
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
            int leftScrollableRows = visibleRows.size() + TRAILING_SPACER_LINES;
            if (leftScrollableRows > maxVisibleRowsOnLeftPage) {
                int maxLeftScroll = leftScrollableRows - maxVisibleRowsOnLeftPage;
                this.leftPageScrollAmount = Mth.clamp(this.leftPageScrollAmount - (delta * 1.5), 0, maxLeftScroll);
                return true;
            }
        } else if (mouseX >= centerCrease && mouseX <= this.leftPos + this.imageWidth) {
            if (this.totalWrappedLines > maxVisibleLinesOnPage) {
                int maxRightScroll = this.totalWrappedLines - maxVisibleLinesOnPage;
                this.rightPageScrollAmount = Mth.clamp(this.rightPageScrollAmount - (delta * 1.5), 0, maxRightScroll);
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
        for (JournalNetworkManager.SyncEntry entry : TravellersHandbookClient.CLIENT_ENTRIES_CACHE) {
            if (entry.category().equals(categoryName) && entry.hasRequirements()) {
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
        public final JournalNetworkManager.SyncEntry entry;
        public final boolean isHeader;
        public final boolean isExpanded;

        public final FormattedCharSequence renderedTextLine;
        public boolean isTitleExtension = false;

        public UiRow(String categoryName, JournalNetworkManager.SyncEntry entry, boolean isExpanded, FormattedCharSequence renderedTextLine) {
            this.categoryName = categoryName;
            this.entry = entry;
            this.isHeader = (entry == null);
            this.isExpanded = isExpanded;
            this.renderedTextLine = renderedTextLine;
        }
    }

    private String scrambleToRunes(String text, long baseSeed, long currentTick) {
        StringBuilder scrambled = new StringBuilder();

        long animatedTick = TravellersHandbookConfig.reduceRuneMotion ? 0L : currentTick / 2L;

        // set of runes that are similar width
        char[] fixedWidthRunes = {
                'a', 'b', 'c', 'd', 'e', 'g', 'h', 'j', 'k', 'n',
                'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'y', 'z'
        };

        for (int pos = 0; pos < text.length(); pos++) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '.' || c == ',' || c == '!' || c == '?') {
                scrambled.append(' ');
            } else {
                long uniqueCharSeed = baseSeed + (pos * 19L) + animatedTick;
                java.util.Random rand = new java.util.Random(uniqueCharSeed);

                char uniformRune = fixedWidthRunes[rand.nextInt(fixedWidthRunes.length)];
                scrambled.append(uniformRune); // add one of the random runes from the fixed set of runes
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
        String[] consonants = {"b", "c", "d", "f", "g", "h", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w", "z"};
        String[] vowels = {"a", "e", "i", "o", "u"};
        // most words are short enough for several to share a rune-font line with occasional longer words being placed in to appear more natural
        int length = random.nextInt(10) == 0 ? 6 + random.nextInt(2) : 2 + random.nextInt(4);
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String[] letters = i % 2 == 0 ? consonants : vowels;
            word.append(letters[random.nextInt(letters.length)]);
        }
        return word.toString();
    }

    private String createObfuscatedTitle(JournalNetworkManager.SyncEntry entry) {
        Random random = new Random(getEntrySeed(entry));
        String[] words = {"ancient", "cipher", "forgotten", "hidden", "mystery", "runes", "sealed", "veiled"};
        return words[random.nextInt(words.length)] + " " + words[random.nextInt(words.length)];
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

        if (requirement.type() != JournalRequirement.Type.ITEM) {
            return Component.literal(requirement.id());
        }

        try {
            Item item = BuiltInRegistries.ITEM.get(TravellersHandbook.parseId(requirement.id()));
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
