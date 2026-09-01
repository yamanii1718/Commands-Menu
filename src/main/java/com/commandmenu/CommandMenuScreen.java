package com.commandmenu;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class CommandMenuScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int COMMANDS_PER_PAGE = 8;
    private static final int PANEL_WIDTH       = 310;
    private static final int PANEL_PADDING     = 10;
    private static final int BUTTON_HEIGHT     = 22;
    private static final int BUTTON_GAP        = 2;

    // ── State ─────────────────────────────────────────────────────────────────
    private int currentPage = 0;
    private final List<CommandEntry> rootCommands = new ArrayList<>();
    private List<CommandEntry> filteredEntries = new ArrayList<>();
    private final List<CommandNode<?>> navigationNodes = new ArrayList<>();
    private final List<String> navigationPaths = new ArrayList<>();
    private CommandNode<?> currentNode;
    private String currentPath = "";

    // ── Panel geometry (computed in init) ─────────────────────────────────────
    private int panelX, panelY, panelHeight;

    // ── Widgets ───────────────────────────────────────────────────────────────
    private TextFieldWidget  searchField;
    private ButtonWidget     backButton;
    private ButtonWidget     chatButton;
    private ButtonWidget     prevButton;
    private ButtonWidget     nextButton;
    private final ButtonWidget[] commandButtons    = new ButtonWidget[COMMANDS_PER_PAGE];
    private final CommandEntry[] currentButtonEntries = new CommandEntry[COMMANDS_PER_PAGE];

    // ═════════════════════════════════════════════════════════════════════════
    public CommandMenuScreen() {
        super(Text.literal("Command Menu"));
        loadCommands();
    }

    // ── Command loading ───────────────────────────────────────────────────────

    private void loadCommands() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return;

        var dispatcher = handler.getCommandDispatcher();
        if (dispatcher == null) return;

        for (CommandNode<?> node : dispatcher.getRoot().getChildren()) {
            rootCommands.add(new CommandEntry("/" + node.getName(), node));
        }
        sortEntries(rootCommands);
        filteredEntries = new ArrayList<>(rootCommands);
    }

    // ── Screen initialisation ─────────────────────────────────────────────────

    @Override
    protected void init() {
        int commandAreaHeight = COMMANDS_PER_PAGE * (BUTTON_HEIGHT + BUTTON_GAP);
        panelHeight = 26              // title bar
                    + 10             // gap
                    + 18             // search field
                    + 8              // gap
                    + commandAreaHeight
                    + 8              // gap
                    + 22             // nav row
                    + 12;            // bottom padding

        panelX = (width  - PANEL_WIDTH)  / 2;
        panelY = (height - panelHeight) / 2;

        int contentX     = panelX + PANEL_PADDING;
        int contentWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        int y            = panelY + 36;

        // ── Search field ──
        searchField = new TextFieldWidget(textRenderer, contentX, y, contentWidth, 18, Text.empty());
        searchField.setPlaceholder(Text.literal("Search commands..."));
        searchField.setMaxLength(64);
        searchField.setChangedListener(query -> {
            currentPage = 0;
            filterCommands(query);
            updateCommandButtons();
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);
        y += 18 + 8;

        // ── Command slots (fixed; content swapped on page change) ──
        for (int i = 0; i < COMMANDS_PER_PAGE; i++) {
            final int slot = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal(""), b -> {
                        if (currentButtonEntries[slot] != null) {
                            onCommandClick(currentButtonEntries[slot]);
                        }
                    })
                    .dimensions(contentX, y + i * (BUTTON_HEIGHT + BUTTON_GAP), contentWidth, BUTTON_HEIGHT)
                    .build();
            commandButtons[i] = btn;
            addDrawableChild(btn);
        }
        y += commandAreaHeight + 8;

        // ── Navigation row ──
        int navWidth = 66;
        int navGap = 8;
        int navY = y;

        prevButton = ButtonWidget.builder(Text.literal("Prev"), b -> {
                    if (currentPage > 0) { currentPage--; updateCommandButtons(); }
                })
                .dimensions(contentX, navY, navWidth, 20)
                .build();

        backButton = ButtonWidget.builder(Text.literal("Back"), b -> goBack())
                .dimensions(contentX + navWidth + navGap, navY, navWidth, 20)
                .build();

        chatButton = ButtonWidget.builder(Text.literal("Open Chat"), b -> openCurrentInChat())
                .dimensions(contentX + (navWidth + navGap) * 2, navY, navWidth, 20)
                .build();

        nextButton = ButtonWidget.builder(Text.literal("Next"), b -> {
                    if (currentPage < getPageCount() - 1) { currentPage++; updateCommandButtons(); }
                })
                .dimensions(contentX + (navWidth + navGap) * 3, navY, navWidth, 20)
                .build();

        addDrawableChild(backButton);
        addDrawableChild(chatButton);
        addDrawableChild(prevButton);
        addDrawableChild(nextButton);

        filterCommands("");
        updateCommandButtons();
    }

    // ── Filtering & pagination ────────────────────────────────────────────────

    private void filterCommands(String query) {
        if (query == null || query.isBlank()) {
            filteredEntries = getCurrentEntries();
        } else {
            String lower = query.trim().toLowerCase();
            filteredEntries = getCurrentEntries().stream()
                    .filter(entry -> entry.label.toLowerCase().contains(lower)
                            || entry.path.toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }
    }

    private int getPageCount() {
        return Math.max(1, (int) Math.ceil((double) filteredEntries.size() / COMMANDS_PER_PAGE));
    }

    private void updateCommandButtons() {
        int start = currentPage * COMMANDS_PER_PAGE;
        for (int i = 0; i < COMMANDS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx < filteredEntries.size()) {
                CommandEntry entry = filteredEntries.get(idx);
                currentButtonEntries[i] = entry;
                String suffix = entry.node.getChildren().isEmpty() ? "" : "  >";
                commandButtons[i].setMessage(Text.literal(entry.label + suffix));
                commandButtons[i].active  = true;
                commandButtons[i].visible = true;
            } else {
                currentButtonEntries[i]  = null;
                commandButtons[i].setMessage(Text.literal(""));
                commandButtons[i].active  = false;
                commandButtons[i].visible = false;
            }
        }
        if (backButton != null) {
            backButton.active = !navigationNodes.isEmpty();
            backButton.visible = true;
        }
        if (chatButton != null) {
            chatButton.active = currentNode != null && !currentPath.isBlank();
            chatButton.visible = currentNode != null && !currentPath.isBlank();
        }
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < getPageCount() - 1;
    }

    private List<CommandEntry> getCurrentEntries() {
        if (currentNode == null) {
            return new ArrayList<>(rootCommands);
        }

        List<CommandEntry> entries = new ArrayList<>();
        for (CommandNode<?> child : currentNode.getChildren()) {
            String label = formatNodeLabel(child);
            entries.add(new CommandEntry(currentPath + " " + label, child));
        }
        sortEntries(entries);
        return entries;
    }

    private static void sortEntries(List<CommandEntry> entries) {
        Collections.sort(entries, (first, second) ->
                first.label.compareToIgnoreCase(second.label));
    }

    private static String formatNodeLabel(CommandNode<?> node) {
        if (node instanceof ArgumentCommandNode<?, ?>) {
            return "<" + node.getName() + ">";
        }
        return node.getName();
    }

    private void onCommandClick(CommandEntry entry) {
        if (!entry.node.getChildren().isEmpty()) {
            navigationNodes.add(currentNode);
            navigationPaths.add(currentPath);
            currentNode = entry.node;
            currentPath = entry.path;
            currentPage = 0;
            searchField.setText("");
            searchField.setPlaceholder(Text.literal("Search subcommands..."));
            filterCommands("");
            updateCommandButtons();
            return;
        }

        if (entry.node instanceof ArgumentCommandNode<?, ?> || entry.node.getCommand() == null) {
            openChat(entry.path);
            return;
        }

        executeCommand(entry.path);
    }

    private void goBack() {
        if (navigationNodes.isEmpty()) return;

        int last = navigationNodes.size() - 1;
        currentNode = navigationNodes.remove(last);
        currentPath = navigationPaths.remove(last);
        currentPage = 0;
        searchField.setText("");
        searchField.setPlaceholder(Text.literal(
                currentNode == null ? "Search commands..." : "Search subcommands..."));
        filterCommands("");
        updateCommandButtons();
    }

    private void openCurrentInChat() {
        if (currentNode != null && !currentPath.isBlank()) {
            openChat(currentPath);
        }
    }

    private void openChat(String commandPath) {
        MinecraftClient.getInstance().setScreen(new ChatScreen(commandPath + " ", false));
    }

    // ── Command execution ─────────────────────────────────────────────────────

    private void executeCommand(String commandPath) {
        String raw = commandPath.startsWith("/") ? commandPath.substring(1) : commandPath;
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler != null) {
            handler.sendChatCommand(raw);
        }
        MinecraftClient.getInstance().setScreen(null);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // In 1.21.11 the game framework already calls renderBackground() before render() —
        // calling it again here causes "Can only blur once per frame" crash.

        int px = panelX, py = panelY, pw = PANEL_WIDTH, ph = panelHeight;

        // Panel body + title bar
        context.fill(px,      py,      px + pw, py + ph, 0xEE0D0D1A);
        context.fill(px,      py,      px + pw, py + 26, 0xFF16213E);

        // Accent border
        int border = 0xFF3A86FF;
        context.fill(px,          py,          px + pw,     py + 1,      border);
        context.fill(px,          py + ph - 1, px + pw,     py + ph,     border);
        context.fill(px,          py,          px + 1,      py + ph,     border);
        context.fill(px + pw - 1, py,          px + pw,     py + ph,     border);
        context.fill(px + 1,      py + 26,     px + pw - 1, py + 27,     border);

        // Title
        String title = currentNode == null
                ? "⚡  Command Menu"
                : "⚡  " + currentPath;
        context.drawCenteredTextWithShadow(textRenderer, title, px + pw / 2, py + 8, 0xFFFFAA);

        // Keybind hint
        String hint = "[" + CommandMenuClient.openMenuKey.getBoundKeyLocalizedText().getString() + " to close]";
        context.drawTextWithShadow(textRenderer, hint,
                px + pw - PANEL_PADDING - textRenderer.getWidth(hint), py + 8, 0x556677AA);

        // Page / result info
        String info = filteredEntries.isEmpty()
            ? "No commands found"
            : "Page " + (currentPage + 1) + " / " + getPageCount()
              + "   (" + filteredEntries.size()
              + (currentNode == null ? " command" : " option")
              + (filteredEntries.size() == 1 ? "" : "s") + ")";
        context.drawCenteredTextWithShadow(textRenderer, info, px + pw / 2, py + ph - 34, 0x88AAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class CommandEntry {
        private final String path;
        private final String label;
        private final CommandNode<?> node;

        private CommandEntry(String path, CommandNode<?> node) {
            this.path = path;
            this.label = path.substring(path.lastIndexOf(' ') + 1);
            this.node = node;
        }

        private CommandEntry(String path, String label, CommandNode<?> node) {
            this.path = path;
            this.label = label;
            this.node = node;
        }
    }
}
