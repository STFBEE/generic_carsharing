package ru.ovm.genericcarsharing.utils

import com.google.android.material.bottomsheet.BottomSheetBehavior

class BehaviorManager(
    private val behavior: BottomSheetBehavior<*>,
    var halfRatio: Float,
) {

    fun setBehaviorState(state: State) {
        when (state) {
            State.DEFAULT -> {
                behavior.isHideable = true
                behavior.isFitToContents = false
                behavior.skipCollapsed = false
                behavior.isDraggable = true
                behavior.halfExpandedRatio = halfRatio
            }
            State.DEFAULT_FULL -> {
                behavior.isHideable = true
                behavior.skipCollapsed = false
                behavior.isFitToContents = false
                behavior.isDraggable = true
                behavior.halfExpandedRatio = 0.99999f
            }
            State.COLLAPSED_EXPANDED -> {
                behavior.isHideable = false
                behavior.skipCollapsed = false
                behavior.isFitToContents = false
                behavior.isDraggable = true
                behavior.halfExpandedRatio = 0.99999f
            }
            State.HIDDEN_EXPANDED -> {
                behavior.isHideable = true
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                behavior.isDraggable = true
                behavior.halfExpandedRatio = 0.99999f
            }
            State.EXPANDED -> {
                behavior.isHideable = false
                behavior.skipCollapsed = true
                behavior.isFitToContents = false
                behavior.isDraggable = false
                behavior.halfExpandedRatio = 0.99999f
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    enum class State {
        DEFAULT,
        DEFAULT_FULL,
        COLLAPSED_EXPANDED,
        HIDDEN_EXPANDED,
        EXPANDED,
    }
}