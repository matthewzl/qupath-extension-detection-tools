# QuPath Detection Tools Extension

The [QuPath](https://qupath.github.io/) Detection Tools extension simplifies the process of overlaying annotations on detection (e.g., cell) objects. This can be useful when creating training annotations for object classifiers.

## Using the Detection Tools Extension

1. Open an image on QuPath containing detection (e.g., cell) objects.
2. Navigate to and enable `Extensions > Detection Tools > Allow annotation-creation from cells`.
3. Select one or more detection objects and right-click on them. A context menu called `Set annotation` should appear. Use this to overlay annotations of a specified class.
4. (Optional) Navigate to and enable `Extensions > Detection Tools > Allow enhanced drag selection for cells`. This will allow you to select multiple cells by Ctrl/Cmd + drag and deselect cells by Alt/Opt + drag.

> **Note:** QuPath has built-in multi-selection (Alt/Opt + drag), but this extension may offer a more reliable alternative.

For a visual demonstration, see in `example_video.mp4` in the repo.