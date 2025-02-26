package qupath.ext.detectiontools;

import org.controlsfx.control.action.Action;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;

public class DetectionToolsExtension implements QuPathExtension, GitHubProject {
    public static final String VERSION = "0.0.2";
    private AnnotationOverlayRunner annotationOverlayRunner;
    private CellDragRunner cellDragRunner;

    @Override
    public void installExtension(QuPathGUI qupath) {

        // Setting up annotation-creation from cells functionality
        annotationOverlayRunner = new AnnotationOverlayRunner(qupath);
        Action annotationOverlayAction = ActionTools.createAction(annotationOverlayRunner, "Allow annotation-creation from cells");
        MenuTools.addMenuItems(
                qupath.getMenu("Extensions>Detection Tools", true),
                annotationOverlayAction
        );
        annotationOverlayRunner.setActionInstance(annotationOverlayAction);

        // Setting up drag selection for cells functionality
        cellDragRunner = new CellDragRunner(qupath);
        Action cellDragAction = ActionTools.createAction(cellDragRunner, "Allow enhanced drag selection for cells");
        MenuTools.addMenuItems(
                qupath.getMenu("Extensions>Detection Tools", true),
                cellDragAction
        );
        cellDragRunner.setActionInstance(cellDragAction);

    }

    @Override
    public String getName() {
        return "Detection Tools";
    }

    @Override
    public String getDescription() {
        return "Allow creation of classified annotations from cell detections via an added context menu. \n"
                + "Allow cell detections to be selected and deselected by mouse-drag. \n\n"
                + "Version " + VERSION;
    }

    @Override
    public GitHubProject.GitHubRepo getRepository() {
        return GitHubProject.GitHubRepo.create(getName(), "matthewzl", "qupath-extension-detection-tools");
    }

}
