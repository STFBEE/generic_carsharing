package ru.ovm.genericcarsharing.utils

import com.google.android.material.bottomsheet.BottomSheetBehavior

class BehaviorManager(
    private val behavior: BottomSheetBehavior<*>,
) {

    var use99forHalfState = true
        set(value) {
            field = value
            setBehaviorState(state)
        }

    var halfRatio: Float = .5f
        set(value) {
            field = value
            setBehaviorState(state)
        }

    private var state: State = State.DEFAULT

    fun setBehaviorState(state: State) {
        this.state = state
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
                behavior.halfExpandedRatio = if (use99forHalfState) 0.99999f else 0.00001f
            }
            State.COLLAPSED_EXPANDED -> {
                behavior.isHideable = false
                behavior.skipCollapsed = false
                behavior.isFitToContents = false
                behavior.isDraggable = true
                behavior.halfExpandedRatio = if (use99forHalfState) 0.99999f else 0.00001f
            }
            State.HIDDEN_EXPANDED -> {
                behavior.isHideable = true
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                behavior.isDraggable = true
                behavior.halfExpandedRatio = if (use99forHalfState) 0.99999f else 0.00001f
            }
            State.EXPANDED -> {
                behavior.isHideable = false
                behavior.skipCollapsed = true
                behavior.isFitToContents = false
                behavior.isDraggable = false
                behavior.halfExpandedRatio = if (use99forHalfState) 0.99999f else 0.00001f
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