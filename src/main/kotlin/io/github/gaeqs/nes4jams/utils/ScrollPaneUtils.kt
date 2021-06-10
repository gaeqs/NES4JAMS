package io.github.gaeqs.nes4jams.utils

import javafx.scene.control.ScrollPane

fun <T : ScrollPane> T.fit () : T {
    isFitToHeight = true
    isFitToWidth = true
    return this;
}