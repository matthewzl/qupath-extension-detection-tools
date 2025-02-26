package qupath.ext.detectiontools;

import org.controlsfx.control.action.Action;
import javafx.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import qupath.lib.gui.scripting.QPEx;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import javafx.collections.ObservableList;
import javafx.scene.text.TextFlow;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationOverlayRunner implements Runnable{
    private static AnnotationOverlayRunner instance;
    private static final Logger logger = LoggerFactory.getLogger(AnnotationOverlayRunner.class);
    private EventHandler<MouseEvent> contextMenuHandler;
    private final QuPathGUI qupath;
    private Action actionInstance;
    private boolean isActive = false;

    public AnnotationOverlayRunner(final QuPathGUI qupath) {
        if (instance != null) {
            throw new IllegalStateException("An instance of this class already exists!");
        }
        instance = this;
        this.qupath = qupath;
    }
    @Override
    public void run() {
        var viewer = qupath.getViewer();
        var view = viewer.getView();

        if (isActive){
            logger.info("Disabling annotation-creation from cells...");
            actionInstance.setText("Allow annotation-creation from cells");
            view.removeEventHandler(MouseEvent.MOUSE_PRESSED, contextMenuHandler);
            isActive = false;
            return;
        }

        logger.info("Enabling annotation-creation from cells...");
        actionInstance.setText("✓ Allow annotation-creation from cells");
        isActive = true;

        var customMenu = new ContextMenu();

        ObservableList<PathClass> availablePathClasses = qupath.getAvailablePathClasses();
        Map<String, Integer> pathClassInfo = new LinkedHashMap<>(); // LinkedHashMap to maintain ordering
        for (PathClass pathClass : availablePathClasses) {
            if (pathClass.getColor() == null) {
                pathClassInfo.put(null, -1); // -1 corresponds to white
            } else {
                pathClassInfo.put(pathClass.toString(), pathClass.getColor());
            }
        }
        contextMenuHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Collection<PathObject> selectedCells;
                try {
                    selectedCells = QPEx.getSelectedObjects()
                        .stream()
                        .filter(PathObject::isCell)
                        .collect(Collectors.toList());
                } catch (NullPointerException e) {
                    return;
                }
                if (selectedCells.stream().noneMatch(PathObject::isCell)) return;

                if (event.isPopupTrigger() || event.isSecondaryButtonDown()) {
                    var pathClassMenu = new Menu("Set annotation");

                    for (Map.Entry<String, Integer> entry : pathClassInfo.entrySet()) {
                        Text coloredSquare = new Text("■"); // kind of a workaround
                        coloredSquare.setFont(new Font(12));
                        int colorCode = entry.getValue();
                        coloredSquare.setFill(Color.rgb((colorCode >> 16) & 0xFF, (colorCode >> 8) & 0xFF, colorCode & 0xFF));

                        Text labelText;
                        if (entry.getKey() == null){
                            labelText = new Text(" None");
                        } else {
                            labelText = new Text(" " + entry.getKey());
                        }

                        var label = new Label();
                        label.setGraphic(new TextFlow(coloredSquare, labelText));

                        var classMenuItem = new MenuItem();
                        classMenuItem.setGraphic(label);
                        classMenuItem.setOnAction(e -> overlayAnnotations(selectedCells, entry.getKey()));
                        pathClassMenu.getItems().add(classMenuItem);
                    }

                    customMenu.getItems().clear();
                    customMenu.getItems().add(pathClassMenu);
                    customMenu.show(viewer.getView(), event.getScreenX(), event.getScreenY() - 33);
                } else if (customMenu.isShowing()) {
                    customMenu.hide();
                }
            }
        };

        viewer.getView().addEventHandler(MouseEvent.MOUSE_PRESSED, contextMenuHandler);
    }

    private void overlayAnnotations(Collection<PathObject> selectedObjects, String pathClassName) {
        List<PathObject> annotations = new ArrayList<>();
        for (PathObject object : selectedObjects) {
            if (pathClassName == null){
                annotations.add(PathObjects.createAnnotationObject(object.getROI(), null));
            } else {
                annotations.add(PathObjects.createAnnotationObject(object.getROI(), QPEx.getPathClass(pathClassName)));
            }
        }
        QPEx.addObjects(annotations);
        QPEx.deselectAll(); // It is assumed the user no longer wants the cells to be selected after the annotations are overlaid.
        CellDragRunner.getSelectedObjectsList().clear(); // Similar assumption as above. The list is cleared so when dragging is resumed, the previous selections don't (annoyingly) persist.
    }

    public void setActionInstance(Action actionInstance){
        this.actionInstance = actionInstance;
    }

}