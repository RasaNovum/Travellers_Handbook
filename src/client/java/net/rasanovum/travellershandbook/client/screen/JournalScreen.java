package net.rasanovum.travellershandbook.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rasanovum.travellershandbook.client.TravellersHandbookClient;
import net.rasanovum.travellershandbook.data.JournalEntry;

import java.util.*;

public class JournalScreen extends Screen {
    private static final ResourceLocation CUSTOM_BOOK_TEXTURE =
            new ResourceLocation("travellers-handbook", "textures/gui/journal_background.png");

    private final int imageWidth = 271;
    private final int imageHeight = 180;
    private int leftPos;
    private int topPos;

    private final Map<String, List<JournalEntry>> groupedEntries = new LinkedHashMap<>();
    private final Set<String> expandedCategories = new HashSet<>();
    private final List<UiRow> visibleRows = new ArrayList<>();
    private int selectedRowIndex = -1;

    private double leftPageScrollAmount = 0;
    private final int maxVisibleRowsOnLeftPage = 13;

    private double rightPageScrollAmount = 0;
    private int totalWrappedLines = 0;
    private final int maxVisibleLinesOnPage = 14;

    public JournalScreen() {
        super(Component.literal("Traveller's Handbook"));  // need to change to translatable key later
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
            groupedEntries.computeIfAbsent(entry.getCategory(), k -> new ArrayList<>()).add(entry);
        }
        // Expand the primary category on load if data map holds entries
        if (!groupedEntries.isEmpty() && expandedCategories.isEmpty()) {
            expandedCategories.add(groupedEntries.keySet().iterator().next());
        }
    }

    private void rebuildVisibleRows() {
        visibleRows.clear();
        int maxTitleWidth = 90;

        for (Map.Entry<String, List<JournalEntry>> group : groupedEntries.entrySet()) {
            String categoryName = group.getKey();
            boolean isExpanded = expandedCategories.contains(categoryName);

            Component headerComponent = Component.literal("§l" + (isExpanded ? "[-] " : "[+] ") + categoryName);
            FormattedCharSequence headerVisualText = headerComponent.getVisualOrderText();

            visibleRows.add(new UiRow(categoryName, null, isExpanded, headerVisualText));

            if (isExpanded) {
                for (JournalEntry entry : group.getValue()) {
                    Component fullTitleText = Component.literal(entry.getTitle());
                    List<FormattedCharSequence> splitTitleLines = this.font.split(fullTitleText, maxTitleWidth);

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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        RenderSystem.setShaderTexture(0, CUSTOM_BOOK_TEXTURE);
        guiGraphics.blit(CUSTOM_BOOK_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);


        // render left page
        int indexLeftX = this.leftPos + 24;
        int indexRowStartY = this.topPos + 32;
        int startRowOffset = (int) this.leftPageScrollAmount;

        if (mouseX >= indexLeftX && mouseX <= this.leftPos + 124) {
            for (int i = 0; i < Math.min(visibleRows.size(), maxVisibleRowsOnLeftPage); i++) {
                int rTop = indexRowStartY + (i * 10);
                if (mouseY >= rTop && mouseY <= rTop + 9) {
                    int hoveredIndex = startRowOffset + i;
                    if (hoveredIndex < visibleRows.size()) {
                        this.selectedRowIndex = hoveredIndex;
                    }
                }
            }
        }

        int leftPageX = this.leftPos + 24;
        int leftPageY = this.topPos + 18;

        guiGraphics.drawString(this.font, "§nJournal Index", leftPageX, leftPageY, 0xFF222222, false); // need to change to translatable key later
        leftPageY += 14;

        if (visibleRows.isEmpty()) {
            guiGraphics.drawString(this.font, "No entries found.", leftPageX, leftPageY, 0xFF555555, false);  // need to change to translatable key later
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
                if (selectedRowIndex >= 0 && selectedRowIndex < visibleRows.size()) {
                    UiRow selectedRow = visibleRows.get(selectedRowIndex);
                    if (row.isHeader && selectedRow.isHeader) {
                        isHighlighted = row.categoryName.equals(selectedRow.categoryName);
                    } else if (!row.isHeader && !selectedRow.isHeader) {
                        isHighlighted = (row.entry == selectedRow.entry);
                    }
                }

                int color = isHighlighted ? 0xFF007700 : (row.isHeader ? 0xFF444444 : 0xFF666666);

                if (row.isHeader) {
                    guiGraphics.drawString(this.font, row.renderedTextLine, leftPageX, currentY, color, false);
                } else {
                    int indentX = leftPageX + 6;

                    if (isHighlighted && !row.isTitleExtension) {
                        guiGraphics.drawString(this.font, "> ", leftPageX, currentY, 0xFF007700, false);
                    }

                    guiGraphics.drawString(this.font, row.renderedTextLine, indentX, currentY, color, false);
                }
            }

            // left page scrollbar
            if (visibleRows.size() > maxVisibleRowsOnLeftPage) {
                int leftBarX = this.leftPos + 126;
                int leftBarTrackY = this.topPos + 32;
                int leftBarTrackHeight = maxVisibleRowsOnLeftPage * 10;

                int thumbHeight = Math.max(8, (int) ((double) maxVisibleRowsOnLeftPage / visibleRows.size() * leftBarTrackHeight));
                int thumbYOffset = (int) ((this.leftPageScrollAmount / maxLeftScrollIndex) * (leftBarTrackHeight - thumbHeight));
                guiGraphics.fill(leftBarX, leftBarTrackY + thumbYOffset, leftBarX + 2, leftBarTrackY + thumbYOffset + thumbHeight, 0x88444444);
            }
        }

        // render right page
        int rightPageX = this.leftPos + 146;
        int rightPageY = this.topPos + 18;
        int maxPageWidth = 100;

        if (selectedRowIndex >= 0 && selectedRowIndex < visibleRows.size() && !visibleRows.isEmpty()) {
            UiRow activeRow = visibleRows.get(selectedRowIndex);

            if (!activeRow.isHeader) {
                JournalEntry activeEntry = activeRow.entry;

                guiGraphics.drawString(this.font, "§l" + activeEntry.getTitle(), rightPageX, rightPageY, 0xFF111111, false);
                rightPageY += 11;

                guiGraphics.drawString(this.font, "§o" + activeEntry.getCategory(), rightPageX, rightPageY, 0xFF666666, false);
                rightPageY += 13;

                Component bodyText = Component.literal(activeEntry.getDescription());
                List<FormattedCharSequence> lines = this.font.split(bodyText, maxPageWidth);
                this.totalWrappedLines = lines.size();

                int maxRightScrollIndex = Math.max(0, this.totalWrappedLines - maxVisibleLinesOnPage);
                if (this.rightPageScrollAmount > maxRightScrollIndex) {
                    this.rightPageScrollAmount = maxRightScrollIndex;
                }
                int startLineOffset = (int) this.rightPageScrollAmount;

                for (int i = 0; i < maxVisibleLinesOnPage; i++) {
                    int lineIndex = startLineOffset + i;
                    if (lineIndex >= this.totalWrappedLines) break;

                    int currentLineY = rightPageY + (i * 9);
                    guiGraphics.drawString(this.font, lines.get(lineIndex), rightPageX, currentLineY, 0xFF333333, false);
                }

                // right page scrollbar
                if (this.totalWrappedLines > maxVisibleLinesOnPage) {
                    int barX = this.leftPos + 252;
                    int barTrackY = this.topPos + 42;
                    int barTrackHeight = maxVisibleLinesOnPage * 9;

                    int thumbHeight = Math.max(8, (int) ((double) maxVisibleLinesOnPage / this.totalWrappedLines * barTrackHeight));
                    int thumbYOffset = (int) ((this.rightPageScrollAmount / maxRightScrollIndex) * (barTrackHeight - thumbHeight));
                    guiGraphics.fill(barX, barTrackY + thumbYOffset, barX + 2, barTrackY + thumbYOffset + thumbHeight, 0x88444444);
                }
            } else {
                guiGraphics.drawString(this.font, "§l" + activeRow.categoryName, rightPageX, rightPageY, 0xFF111111, false);
                rightPageY += 15;
                // need to change to translatable key later
                String helperMsg = activeRow.isExpanded ? "Click the index row name to collapse this category." : "Click the index row name to expand this category.";
                List<FormattedCharSequence> lines = this.font.split(Component.literal(helperMsg), maxPageWidth);
                for (int i = 0; i < lines.size(); i++) {
                    guiGraphics.drawString(this.font, lines.get(i), rightPageX, rightPageY + (i * 9), 0xFF777777, false);
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int leftPageX = this.leftPos + 24;
            int rowStartY = this.topPos + 32;
            int startRowOffset = (int) this.leftPageScrollAmount;

            if (mouseX >= leftPageX && mouseX <= this.leftPos + 124) {
                for (int i = 0; i < Math.min(visibleRows.size(), maxVisibleRowsOnLeftPage); i++) {
                    int rowTop = rowStartY + (i * 10);
                    int rowBottom = rowTop + 9;

                    if (mouseY >= rowTop && mouseY <= rowBottom) {
                        int actualDataIndex = startRowOffset + i;

                        if (actualDataIndex < visibleRows.size()) {
                            UiRow clickedRow = visibleRows.get(actualDataIndex);
                            this.selectedRowIndex = actualDataIndex;
                            this.rightPageScrollAmount = 0;

                            if (clickedRow.isHeader) {
                                if (expandedCategories.contains(clickedRow.categoryName)) {
                                    expandedCategories.remove(clickedRow.categoryName);
                                } else {
                                    expandedCategories.add(clickedRow.categoryName);
                                }
                                rebuildVisibleRows();
                            }

                            this.minecraft.getSoundManager().play(
                                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int centerCrease = this.leftPos + 135;

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
        return super.mouseScrolled(mouseX, mouseY, delta);
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
}