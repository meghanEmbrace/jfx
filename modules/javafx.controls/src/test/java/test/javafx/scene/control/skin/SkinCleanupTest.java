/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.javafx.scene.control.skin;

import java.lang.ref.WeakReference;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.tk.Toolkit;

import static javafx.collections.FXCollections.*;
import static javafx.scene.control.ControlShim.*;
import static javafx.scene.control.skin.TextInputSkinShim.*;
import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Tests around the cleanup task JDK-8241364.
 */
public class SkinCleanupTest {

    private Scene scene;
    private Stage stage;
    private Pane root;

//------------ TextArea

    /**
     * Sanity: event filter must consume scrollEvent.
     */
    @Test
    public void testScrollEventFilter() {
        TextArea area = new TextArea("some text");
        showControl(area, true);
        setHandlePressed(area, true);
        ScrollEvent scrollEvent = new ScrollEvent(ScrollEvent.ANY, 0, 0, 0, 0, false, false, false, false,
                true, // direct
                false, 0, 0, 0, USE_PREF_SIZE, null, USE_COMPUTED_SIZE, null, 0, 0, null);
        assertTrue("sanity: created a fake direct event", scrollEvent.isDirect());
        // must use copy to detect change in consume
        ScrollEvent copy = scrollEvent.copyFor(area, area);
        Event.fireEvent(area, copy);
        assertTrue("scrollEvent must be consumed", copy.isConsumed());
    }

    /**
     * Sanity: change of selection must update textNode.
     */
    @Test
    public void testTextAreaSelectUpdate() {
        TextArea area = new TextArea("some text");
        installDefaultSkin(area);
        Text textNode = getTextNode(area);
        area.selectAll();
        textNode.getParent().getParent().layout();
        int end = area.getLength();
        assertEquals("sanity: area caret moved to end", end, area.getCaretPosition());
        assertEquals("sanity: area selection updated", end, area.getSelection().getEnd());
        assertEquals("textNode end", end, textNode.getSelectionEnd());
    }

    /**
     * Sanity: toggle textWrap must update scrollPane's fitToWidth.
     */
    @Test
    public void testTextAreaSetWrapUpdate() {
        TextArea area = new TextArea("some text");
        installDefaultSkin(area);
        boolean isWrap = area.isWrapText();
        ScrollPane scrollPane = getScrollPane(area);
        assertEquals(isWrap, scrollPane.isFitToWidth());
        area.setWrapText(!isWrap);
        assertEquals(!isWrap, scrollPane.isFitToWidth());
    }

    /**
     * NPE from listener to prefColumnCount.
     */
    @Test
    public void testTextAreaSetColumnCount() {
        TextArea area = new TextArea("some text");
        int prefColumn = area.getPrefColumnCount();
        assertEquals("sanity: initial count", TextArea.DEFAULT_PREF_COLUMN_COUNT, prefColumn);
        installDefaultSkin(area);
        replaceSkin(area);
        area.setPrefColumnCount(prefColumn * 2);
    }

    /**
     * Sanity: change of prefColumnCount must update scrollPane.
     */
    @Test
    public void testTextAreaSetColumnCountUpdate() {
        TextArea area = new TextArea("some text");
        int prefColumn = area.getPrefColumnCount();
        assertEquals("sanity: initial count", TextArea.DEFAULT_PREF_COLUMN_COUNT, prefColumn);
        installDefaultSkin(area);
        ScrollPane scrollPane = getScrollPane(area);
        double prefViewportWidth = scrollPane.getPrefViewportWidth();
        area.setPrefColumnCount(prefColumn * 2);
        assertEquals("prefViewportWidth must be updated", prefViewportWidth * 2, scrollPane.getPrefViewportWidth(), 1);
    }

    /**
     * NPE from listener to prefRowCount.
     */
    @Test
    public void testTextAreaSetRowCount() {
        TextArea area = new TextArea("some text");
        int prefRows = area.getPrefRowCount();
        installDefaultSkin(area);
        replaceSkin(area);
        area.setPrefRowCount(prefRows * 2);
    }

    /**
     * Sanity: change of prefRows must update scrollPane.
     */
    @Test
    public void testTextAreaSetRowCountUpdate() {
        TextArea area = new TextArea("some text");
        int prefRows = area.getPrefRowCount();
        assertEquals("sanity: initial row count", TextArea.DEFAULT_PREF_ROW_COUNT, prefRows);
        installDefaultSkin(area);
        ScrollPane scrollPane = getScrollPane(area);
        double prefViewportHeight = scrollPane.getPrefViewportHeight();
        area.setPrefRowCount(prefRows * 2);
        assertEquals("prefViewportHeight must be updated", prefViewportHeight * 2, scrollPane.getPrefViewportHeight(), 1);
    }

    /**
     * Sanity: change of text must update textNode.
     */
    @Test
    public void testTextAreaSetTextUpdate() {
        String initial = "some text";
        TextArea area = new TextArea(initial);
        installDefaultSkin(area);
        Text textNode = getTextNode(area);
        assertEquals("sanity initial text sync'ed to textNode", initial, textNode.getText());
        String replaced = "replaced text";
        area.setText(replaced);
        assertEquals(replaced, textNode.getText());
    }

    /**
     * NPE on changing promptText: binding to promptText triggers internal listener to usePromptText.
     */
    @Test
    public void testTextAreaPrompt() {
        TextArea area = new TextArea();
        installDefaultSkin(area);
        replaceSkin(area);
        area.setPromptText("prompt");
    }

    /**
     * Sanity: change of promptText must update promptNode.
     */
    @Test
    public void testTextAreaPromptUpdate() {
        TextArea area = new TextArea();
        installDefaultSkin(area);
        assertNull("sanity: default prompt is null", getPromptNode(area));
        area.setPromptText("prompt");
        assertNotNull("prompt node must be created", getPromptNode(area));
    }

    @Test
    public void testTextAreaChildren() {
        TextArea area = new TextArea("some text");
        installDefaultSkin(area);
        int children = area.getChildrenUnmodifiable().size();
        replaceSkin(area);
        assertEquals("children size must be unchanged: ", children, area.getChildrenUnmodifiable().size());
    }

    /**
     * NPE from listener to scrollPane's hValue.
     */
    @Test
    public void testTextAreaSetScrollLeft() {
        TextArea area = new TextArea(LOREM_IPSUM + LOREM_IPSUM);
        installDefaultSkin(area);
        replaceSkin(area);
        area.setScrollLeft(500);
    }

    /**
     * Sanity: change of scrollLeft must update scrollPane's hValue.
     */
    @Test
    public void testTextAreaSetScrollLeftUpdate() {
        TextArea area = new TextArea(LOREM_IPSUM + LOREM_IPSUM);
        showControl(area, true);
        ScrollPane scrollPane = getScrollPane(area);
        double scrollLeft = 500;
        area.setScrollLeft(scrollLeft);
        Toolkit.getToolkit().firePulse();
        assertEquals("sanity: scrollLeft updated", scrollLeft, area.getScrollLeft(), 0.1);
        assertTrue("scrollPane hValue > 0", scrollPane.getHvalue() > 0.0);
    }

    /**
     * NPE from listener to scrollPane's vValue.
     */
    @Test
    public void testTextAreaSetScrollTop() {
        TextArea area = new TextArea(LOREM_IPSUM + LOREM_IPSUM);
        area.setWrapText(true);
        installDefaultSkin(area);
        replaceSkin(area);
        area.setScrollTop(100);
    }

    /**
     * Sanity: change of scrollTop must update scrollPane's vValue.
     */
    @Ignore("8272082")
    @Test
    public void testTextAreaSetScrollTopUpdate() {
        TextArea area = new TextArea(LOREM_IPSUM + LOREM_IPSUM);
        area.setWrapText(true);
        showControl(area, true, 300, 300);
        ScrollPane scrollPane = getScrollPane(area);
        double scrollTop = 100;
        area.setScrollTop(scrollTop);
        Toolkit.getToolkit().firePulse();
        assertEquals("sanity: scrollTop updated", scrollTop, area.getScrollTop(), 0.1);
        assertTrue("scrollPane vValue > 0", scrollPane.getVvalue() > 0.0);
    }

    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, "
            + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim "
            + "ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip "
            + "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate "
            + "velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat "
            + "cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";


// ----------- TextField

    /**
     * NPE from listener to caretPosition
     */
    @Test
    public void testTextFieldCaretPosition() {
        TextField field = new TextField("some text");
        showControl(field, true);
        int index = 2;
        field.positionCaret(index);
        replaceSkin(field);
        field.positionCaret(index + 1);
    }

    /**
     * Sanity: textNode caret must be updated on change of control caret.
     */
    @Test
    public void testTextFieldCaretPositionUpdate() {
        TextField field = new TextField("some text");
        showControl(field, true);
        Text textNode = getTextNode(field);
        field.positionCaret(2);
        assertEquals("textNode caret", field.getCaretPosition(), textNode.getCaretPosition());
    }

    /**
     * NPE from listener to selection
     */
    @Test
    public void testTextFieldSelection() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        replaceSkin(field);
        field.selectAll();
    }

    /**
     * Sanity: ensure that skin's updating itself on selection change
     */
    @Test
    public void testTextFieldSelectionUpdate() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        Text textNode = getTextNode(field);
        field.selectAll();
        int end = field.getLength();
        assertEquals("sanity: field caret moved to end", end, field.getCaretPosition());
        assertEquals("sanity: field selection updated", end, field.getSelection().getEnd());
        assertEquals("textNode end", end, textNode.getSelectionEnd());
    }

    /**
     * NPE on changing text: binding of text triggers internal listener to selectionShape.
     */
    @Test
    public void testTextFieldText() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        replaceSkin(field);
        field.setText("replaced");
    }

    /**
     * NPE on changing font: binding of font triggers internal listener to selectionShape.
     */
    @Test
    public void testTextFieldFont() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        replaceSkin(field);
        field.setFont(new Font(30));
    }

    /**
     * NPE from listener to alignment
     */
    @Test
    public void testTextFieldAlignment() {
        TextField field = new TextField("some text");
        showControl(field, true);
        assertTrue(field.getWidth() > 0);
        replaceSkin(field);
        field.setAlignment(Pos.TOP_RIGHT);
    }

    /**
     * Sanity: alignment updates still work after the fix.
     */
    @Test
    public void testTextFieldAlignmentUpdate() {
        // field to get the textTranslateX from
        TextField rightAligned = new TextField("dummy");
        rightAligned.setPrefColumnCount(50);
        rightAligned.setAlignment(Pos.CENTER_RIGHT);
        showControl(rightAligned, true);
        double rightTranslate = getTextTranslateX(rightAligned);
        // field to test: start with left, then change to right align while showing
        TextField field = new TextField("dummy");
        field.setPrefColumnCount(50);
        assertEquals("sanity: ", Pos.CENTER_LEFT, field.getAlignment());
        showControl(field, true);
        Toolkit.getToolkit().firePulse();
        double textTranslate = getTextTranslateX(field);
        assertEquals("sanity:", 0, textTranslate, 1);
        field.setAlignment(Pos.CENTER_RIGHT);
        assertEquals("translateX must be updated", rightTranslate, getTextTranslateX(field), 1);
    }

    /**
     * NPE on changing promptText: binding to promptText triggers internal listener to usePromptText.
     */
    @Test
    public void testTextFieldPrompt() {
        TextField field = new TextField();
        installDefaultSkin(field);
        replaceSkin(field);
        field.setPromptText("prompt");
    }

    /**
     * Sanity: prompt updates still working after the fix
     */
    @Test
    public void testTextFieldPromptUpdate() {
        TextField field = new TextField();
        installDefaultSkin(field);
        assertNull("sanity: default prompt is null", getPromptNode(field));
        field.setPromptText("prompt");
        assertNotNull("prompt node must be created", getPromptNode(field));
    }

    @Test
    public void testTextFieldChildren() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        int children = field.getChildrenUnmodifiable().size();
        replaceSkin(field);
        assertEquals("children size must be unchanged: ", children, field.getChildrenUnmodifiable().size());
    }

//--------------- TextInputControl

    /**
     * NPE from inputMethodRequests installed by TextInputControlSkin
     *
     * Note: this is a rather artificial test - in RL the replacing
     * skin will set its own and there's no valid path to invoking the old
     */
    @Test
    public void testTextInputMethodRequests() {
        TextField field = new TextField("some text");
        field.selectRange(2, 5);
        String selected = field.getSelectedText();
        installDefaultSkin(field);
        assertEquals("sanity: skin has set requests", selected, field.getInputMethodRequests().getSelectedText());
        field.getSkin().dispose();
        if (field.getInputMethodRequests() != null) {
            assertEquals(selected, field.getInputMethodRequests().getSelectedText());
        }
    }

    @Test
    public void testTextInputOnInputMethodTextChangedNoHandler() {
        TextField field = new TextField("some text");
        field.setOnInputMethodTextChanged(null);
        installDefaultSkin(field);
        field.getSkin().dispose();
        assertNull("skin dispose must remove handler it has installed", field.getOnInputMethodTextChanged());
    }

    @Test
    public void testTextInputOnInputMethodTextChangedWithHandler() {
        TextField field = new TextField("some text");
        EventHandler<? super InputMethodEvent> handler = e -> {};
        field.setOnInputMethodTextChanged(handler);
        installDefaultSkin(field);
        assertSame("sanity: skin must not replace handler", handler, field.getOnInputMethodTextChanged());
        field.getSkin().dispose();
        assertSame("skin dispose must not remove handler that was installed by control",
                handler, field.getOnInputMethodTextChanged());
    }

    /**
     * Test that skin does not remove a handler that's installed on the field
     * during the skin's lifetime.
     */
    @Test
    public void testTextInputOnInputMethodTextChangedReplacedHandler() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        EventHandler<? super InputMethodEvent> handler = e -> {};
        field.setOnInputMethodTextChanged(handler);
        field.getSkin().dispose();
        assertSame("skin dispose must not remove handler that was installed by control",
                handler, field.getOnInputMethodTextChanged());
    }

    /**
     * Test that handler installed by skin is reset on replacing skin.
     * Here we test the effect by firing an inputEvent.
     */
    @Ignore("JDK-8268877")
    @Test
    public void testTextInputOnInputMethodTextChangedEvent() {
        String initialText = "some text";
        String prefix = "from input event";
        TextField field = new TextField(initialText);
        installDefaultSkin(field);
        InputMethodEvent event = new InputMethodEvent(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                List.of(), prefix, 0);
        Event.fireEvent(field, event);
        assertEquals("sanity: prefix must be committed", prefix + initialText, field.getText());
        replaceSkin(field);
        Event.fireEvent(field, event);
        assertEquals(" prefix must be committed again", prefix + prefix + initialText, field.getText());
    }

    /**
     * Test that handler installed by skin is reset on replacing skin.
     * Here we test the instance of the handler.
     */
    @Ignore("JDK-8268877")
    @Test
    public void testTextInputOnInputMethodTextChangedHandler() {
        TextField field = new TextField("some text");
        installDefaultSkin(field);
        EventHandler<? super InputMethodEvent> handler = field.getOnInputMethodTextChanged();
        replaceSkin(field);
        assertNotSame("replaced skin must replace skin handler", handler, field.getOnInputMethodTextChanged());
        assertNotNull("handler must not be null  ", field.getOnInputMethodTextChanged());
    }


  //---------------- TreeView

    /**
     * Sanity: replacing the root has no side-effect, listener to rootProperty
     * is registered with skin api
     */
    @Test
    public void testTreeViewSetRoot() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.setRoot(createRoot());
    }

    /**
     * NPE from event handler to treeModification of root.
     */
    @Test
    public void testTreeViewAddRootChild() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.getRoot().getChildren().add(createRoot());
    }

    /**
     * NPE from event handler to treeModification of root.
     */
    @Test
    public void testTreeViewReplaceRootChildren() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.getRoot().getChildren().setAll(createRoot().getChildren());
    }

    /**
     * NPE due to properties listener not removed
     */
    @Test
    public void testTreeViewRefresh() {
        TreeView<String> treeView = new TreeView<>();
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.refresh();
    }

    /**
     * Sanity: guard against potential memory leak from root property listener.
     */
    @Test
    public void testMemoryLeakAlternativeSkinWithRoot() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        WeakReference<?> weakRef = new WeakReference<>(replaceSkin(treeView));
        assertNotNull(weakRef.get());
        attemptGC(weakRef);
        assertEquals("Skin must be gc'ed", null, weakRef.get());
    }

    /**
     * Creates and returns an expanded treeItem with two children
     */
    private TreeItem<String> createRoot() {
        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        root.getChildren().addAll(new TreeItem<>("child one"), new TreeItem<>("child two"));
        return root;
    }


// ------------------ TreeCell

    @Test
    public void testTreeCellReplaceTreeViewWithNull() {
        TreeCell<Object> cell =  new TreeCell<>();
        TreeView<Object> treeView = new TreeView<>();
        cell.updateTreeView(treeView);
        installDefaultSkin(cell);
        cell.updateTreeView(null);
        // 8253634: updating the old treeView must not throw NPE in skin
        treeView.setFixedCellSize(100);
    }

    @Test
    public void testTreeCellPrefHeightOnReplaceTreeView() {
        TreeCell<Object> cell =  new TreeCell<>();
        cell.updateTreeView(new TreeView<>());
        installDefaultSkin(cell);
        TreeView<Object> treeView = new TreeView<>();
        treeView.setFixedCellSize(100);
        cell.updateTreeView(treeView);
        assertEquals("fixed cell set to value of new treeView",
                cell.getTreeView().getFixedCellSize(),
                cell.prefHeight(-1), 1);
    }

// ------------------ ListCell

    @Test
    public void testListCellReplaceListViewWithNull() {
        ListCell<Object> cell =  new ListCell<>();
        ListView<Object> listView = new ListView<>();
        cell.updateListView(listView);
        installDefaultSkin(cell);
        cell.updateListView(null);
        // 8246745: updating the old listView must not throw NPE in skin
        listView.setFixedCellSize(100);
    }

   @Test
   public void testListCellPrefHeightOnReplaceListView() {
       ListCell<Object> cell =  new ListCell<>();
       cell.updateListView(new ListView<>());
       installDefaultSkin(cell);
       ListView<Object> listView = new ListView<>();
       listView.setFixedCellSize(100);
       cell.updateListView(listView);
       assertEquals("fixed cell set to value of new listView",
               cell.getListView().getFixedCellSize(),
               cell.prefHeight(-1), 1);
   }

  //-------------- listView

    @Test
    public void testListViewAddItems() {
        ListView<String> listView = new ListView<>();
        installDefaultSkin(listView);
        replaceSkin(listView);
        listView.getItems().add("addded");
    }

    @Test
    public void testListViewRefresh() {
        ListView<String> listView = new ListView<>();
        installDefaultSkin(listView);
        replaceSkin(listView);
        listView.refresh();
    }

    @Test
    public void testListViewSetItems() {
        ListView<String> listView = new ListView<>();
        installDefaultSkin(listView);
        replaceSkin(listView);
        listView.setItems(observableArrayList());
    }

//-------- choiceBox, toolBar

    /**
     * NPE on sequence setItems -> modify items after skin is replaced.
     */
    @Test
    public void testChoiceBoxSetItems() {
        ChoiceBox<String> box = new ChoiceBox<>();
        installDefaultSkin(box);
        replaceSkin(box);
        box.setItems(observableArrayList("one"));
        box.getItems().add("added");
    }

    /**
     * NPE when adding items after skin is replaced
     */
    @Test
    public void testChoiceBoxAddItems() {
        ChoiceBox<String> box = new ChoiceBox<>();
        installDefaultSkin(box);
        replaceSkin(box);
        box.getItems().add("added");
    }

    @Test
    public void testToolBarAddItems() {
        ToolBar bar = new ToolBar();
        installDefaultSkin(bar);
        replaceSkin(bar);
        bar.getItems().add(new Rectangle());
    }

    /**
     * Sanity test - fix changed listening to focusProperty, ensure
     * that it's still working as before.
     */
    @Test
    public void testToolBarFocus() {
        ToolBar bar = new ToolBar();
        bar.getItems().addAll(new Button("dummy"), new Button("other"));
        showControl(bar, false);
        Button outside = new Button("outside");
        showControl(outside, true);
        bar.requestFocus();
        assertEquals("first item in toolbar must be focused", bar.getItems().get(0), scene.getFocusOwner());
    }

//-------- TabPane
    @Test
    public void testChildrenCountAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        int childrenCount = tabPane.getChildrenUnmodifiable().size();
        replaceSkin(tabPane);
        assertEquals(childrenCount, tabPane.getChildrenUnmodifiable().size());
    }

    @Test
    public void testChildrenCountAfterSkinIsRemoved() {
        TabPane tabPane = new TabPane();
        assertEquals(0, tabPane.getChildrenUnmodifiable().size());
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        assertEquals(3, tabPane.getChildrenUnmodifiable().size());
        tabPane.setSkin(null);
        assertNull(tabPane.getSkin());
        assertEquals(0, tabPane.getChildrenUnmodifiable().size());
    }

    @Test
    public void testNPEWhenTabsAddedAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        replaceSkin(tabPane);
        tabPane.getTabs().addAll(new Tab("2"), new Tab("3"));
    }

    @Test
    public void testNPEWhenTabRemovedAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        replaceSkin(tabPane);
        tabPane.getTabs().remove(0);
    }

    @Test
    public void testAddRemoveTabsAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        assertEquals(2, tabPane.getTabs().size());
        assertEquals(3, tabPane.getChildrenUnmodifiable().size());
        replaceSkin(tabPane);
        tabPane.getTabs().addAll(new Tab("2"), new Tab("3"));
        assertEquals(4, tabPane.getTabs().size());
        assertEquals(5, tabPane.getChildrenUnmodifiable().size());
        tabPane.getTabs().clear();
        assertEquals(0, tabPane.getTabs().size());
        assertEquals(1, tabPane.getChildrenUnmodifiable().size());
    }

//---------------- setup and initial

    /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     */
    protected void showControl(Control control, boolean focused) {
        showControl(control, focused, -1, -1);
    }

    /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     * On first call, sizes the scene to sceneX/Y if sceneX > 0
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     * @param sceneX the width of the scene or -1 for autosizing
     * @param sceneY the height of the scene or -1 for autosing
     */
    protected void showControl(Control control, boolean focused, double sceneX, double sceneY) {
        if (root == null) {
            root = new VBox();
            if (sceneX > 0) {
                scene = new Scene(root, sceneX, sceneY);
            } else {
                scene = new Scene(root);
            }
            stage = new Stage();
            stage.setScene(scene);
        }
        if (!root.getChildren().contains(control)) {
            root.getChildren().add(control);
        }
        stage.show();
        if (focused) {
            stage.requestFocus();
            control.requestFocus();
            assertTrue(control.isFocused());
            assertSame(control, scene.getFocusOwner());
        }
    }

    @After
    public void cleanup() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @Before
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

}


