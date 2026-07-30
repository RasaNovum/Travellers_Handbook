package net.rasanovum.travellershandbook.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rasanovum.travellershandbook.client.TravellersHandbookConfig;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.TravellersHandbook;
import net.rasanovum.travellershandbook.data.JournalEntry;

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
    private static final int MAX_RUNE_LINE_WIDTH = 94;
    private static final int MAX_TITLE_LINES = 2;
    private static final int RIGHT_PAGE_CONTENT_BOTTOM = 188;
    private int leftPos;
    private int topPos;

    private final Map<String, List<JournalEntry>> groupedEntries = new LinkedHashMap<>();
    private final Set<String> expandedCategories = new HashSet<>();
    private final List<UiRow> visibleRows = new ArrayList<>();
    private int selectedRowIndex = -1;

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
        for (JournalEntry entry : TravellersHandbookClient.CLIENT_ENTRIES_CACHE) {
            groupedEntries.computeIfAbsent(entry.category(), k -> new ArrayList<>()).add(entry);
        }
        if (!groupedEntries.isEmpty() && expandedCategories.isEmpty()) {
            expandedCategories.add(groupedEntries.keySet().iterator().next());
        }
    }

    private void rebuildVisibleRows() {
        visibleRows.clear();

        for (Map.Entry<String, List<JournalEntry>> group : groupedEntries.entrySet()) {
            String categoryName = group.getKey();
            boolean isExpanded = expandedCategories.contains(categoryName);

            Component headerComponent = Component.literal("§l" + categoryName);
            FormattedCharSequence headerVisualText = headerComponent.getVisualOrderText();

            visibleRows.add(new UiRow(categoryName, null, isExpanded, headerVisualText));

            if (isExpanded) {
                for (JournalEntry entry : group.getValue()) {

                    Component desc = Component.translatable("gui.travellershandbook.locked_description");
                    boolean isLocked = entry.description().equals(desc.getString());

                    if (isLocked) {
                        String displayTitleString = "altfont_" + entry.title();
                        String originalTitle = entry.title();

                        if (originalTitle.length() > 16) {
                            originalTitle = originalTitle.substring(0, 13) + "...";
                        }

                        Component singleLineComponent = Component.literal(originalTitle).withStyle(style -> style.withFont(Minecraft.ALT_FONT));
                        JournalEntry displayEntry = new JournalEntry(entry.category(), displayTitleString, entry.description(), entry.advancementRequirement());

                        visibleRows.add(new UiRow(categoryName, displayEntry, false, singleLineComponent.getVisualOrderText()));
                    } else {
                        String displayTitleString = entry.title();
                        Component fullTitleText = Component.literal(displayTitleString);
                        List<FormattedCharSequence> splitTitleLines = this.font.split(fullTitleText, MAX_TITLE_WIDTH);
                        if (splitTitleLines.isEmpty()) continue;

                        JournalEntry displayEntry = new JournalEntry(entry.category(), displayTitleString, entry.description(), entry.advancementRequirement());
                        visibleRows.add(new UiRow(categoryName, displayEntry, false, splitTitleLines.get(0)));

                        for (int i = 1; i < splitTitleLines.size(); i++) {
                            UiRow extensionRow = new UiRow(categoryName, displayEntry, false, splitTitleLines.get(i));
                            extensionRow.isTitleExtension = true;
                            visibleRows.add(extensionRow);
                        }
                    }
                }
            }
        }
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
            int maxLeftScrollIndex = Math.max(0, visibleRows.size() - maxVisibleRowsOnLeftPage);
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
                        isHighlighted = row.entry.advancementRequirement().equals(hoveredRow.entry.advancementRequirement());
                    }
                }

                boolean isSelected = false;
                if (selectedRowIndex >= 0 && selectedRowIndex < visibleRows.size()) {
                    UiRow selectedRow = visibleRows.get(selectedRowIndex);
                    if (!row.isHeader && !selectedRow.isHeader) {
                        isSelected = row.entry.advancementRequirement().equals(selectedRow.entry.advancementRequirement());
                    }
                }

                int color = isHighlighted ? 0xFF007700 : (row.isHeader ? 0xFF444444 : 0xFF666666);

                if (row.isHeader) {
                    guiGraphics.drawString(this.font, row.renderedTextLine, leftPageX, currentY, color, false);
                    boolean categoryHasUnread = TravellersHandbookClient.UNREAD_ENTRIES_SET.stream().anyMatch(unreadRequirementId ->
                            TravellersHandbookClient.CLIENT_ENTRIES_CACHE.stream().anyMatch(cached ->
                                    cached.advancementRequirement().equals(unreadRequirementId) && cached.category().equals(row.categoryName)));

                    if (categoryHasUnread) {
                        // int categoryTextWidth = this.font.width(row.renderedTextLine);
                        int notificationX = leftPageX - 5;

                        guiGraphics.drawString(this.font, "§l!", notificationX, currentY, 0xFFE69138, false);
                    }
                } else {
                    int indentX = leftPageX + 12;
                    boolean isUnread = TravellersHandbookClient.UNREAD_ENTRIES_SET.contains(row.entry.advancementRequirement());

                    if (!row.isTitleExtension) {
                        if (isSelected) {
                            guiGraphics.drawString(this.font, "> ", leftPageX + 4, currentY, 0xFF007700, false);
                        } else if (isUnread) {
                            guiGraphics.drawString(this.font, "§l!", leftPageX + 6, currentY, 0xFFE69138, false);
                        }
                    }

                    Component description = Component.translatable("gui.travellershandbook.locked_description");
                    if (row.entry.description().equals(description.getString())) {

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
            if (visibleRows.size() > maxVisibleRowsOnLeftPage) {
                int leftBarX = this.leftPos + 156; //126
                int leftBarTrackY = this.topPos + 52; //32
                int leftBarTrackHeight = maxVisibleRowsOnLeftPage * 10;

                int thumbHeight = Math.max(8, (int) ((double) maxVisibleRowsOnLeftPage / visibleRows.size() * leftBarTrackHeight));
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
                JournalEntry activeEntry = activeRow.entry;

                boolean isLockedTitle = activeEntry.title().startsWith("altfont_");
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

                Component bodyText = Component.literal(activeEntry.description());
                Component lockedDescCheck = Component.translatable("gui.travellershandbook.locked_description");
                boolean isRightPageLocked = activeEntry.description().equals(lockedDescCheck.getString());
                if (isRightPageLocked) {
                    bodyText = Component.literal(createObfuscatedBody(activeEntry))
                            .withStyle(style -> style.withFont(Minecraft.ALT_FONT));
                }

                int dynamicWrapWidth = isRightPageLocked ? MAX_RUNE_LINE_WIDTH : MAX_PAGE_WIDTH;
                List<FormattedCharSequence> lines = this.font.split(bodyText, dynamicWrapWidth);
                this.totalWrappedLines = lines.size();
                this.maxVisibleLinesOnPage = Math.max(0, (this.topPos + RIGHT_PAGE_CONTENT_BOTTOM - rightPageY) / 9);

                int maxRightScrollIndex = Math.max(0, this.totalWrappedLines - maxVisibleLinesOnPage);
                if (this.rightPageScrollAmount > maxRightScrollIndex) {
                    this.rightPageScrollAmount = maxRightScrollIndex;
                }
                int startLineOffset = (int) this.rightPageScrollAmount;

                for (int i = 0; i < maxVisibleLinesOnPage; i++) {
                    int lineIndex = startLineOffset + i;
                    if (lineIndex >= this.totalWrappedLines) break;

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
                                rebuildVisibleRows();
                            } else {
                                if (this.selectedRowIndex == actualDataIndex) {
                                    this.selectedRowIndex = -1;
                                } else {
                                    this.selectedRowIndex = actualDataIndex;

                                    if (clickedRow.entry != null) {
                                        TravellersHandbookClient.UNREAD_ENTRIES_SET.remove(
                                                clickedRow.entry.advancementRequirement()
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
            if (visibleRows.size() > maxVisibleRowsOnLeftPage) {
                int maxLeftScroll = visibleRows.size() - maxVisibleRowsOnLeftPage;
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

    private static class UiRow {
        public final String categoryName;
        public final JournalEntry entry;
        public final boolean isHeader;
        public final boolean isExpanded;

        public final FormattedCharSequence renderedTextLine;
        public boolean isTitleExtension = false;

        public UiRow(String categoryName, JournalEntry entry, boolean isExpanded, FormattedCharSequence renderedTextLine) {
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

    private String createObfuscatedBody(JournalEntry entry) {
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

    private String createObfuscatedTitle(JournalEntry entry) {
        Random random = new Random(getEntrySeed(entry));
        String[] words = {"ancient", "cipher", "forgotten", "hidden", "mystery", "runes", "sealed", "veiled"};
        return words[random.nextInt(words.length)] + " " + words[random.nextInt(words.length)];
    }

    private long getEntrySeed(JournalEntry entry) {
        return Objects.hash(entry.category(), entry.title(), entry.advancementRequirement());
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
