package com.github.paulpaulych.intermirrorbot.bot.userflows

interface State<CMD> {
    suspend fun init(): State<CMD>?
    suspend fun handle(action: CMD): State<CMD>?
}

class StateMachine<CMD> private constructor(
    private var state: State<CMD>
) {

    companion object {
        suspend fun <CMD> init(state: State<CMD>): StateMachineStartResult<CMD> {
            val nextState = state.init()
                ?: return StateMachineStartResult.Finished
            return StateMachineStartResult.Success(StateMachine(nextState))
        }
    }

    sealed interface StateMachineStartResult<out CMD> {
        data class Success<CMD>(val stateMachine: StateMachine<CMD>): StateMachineStartResult<CMD>
        object Finished: StateMachineStartResult<Nothing>
    }

    sealed interface StateMachineFlowResult {
        object Continue: StateMachineFlowResult
        object Finish: StateMachineFlowResult
    }

    suspend fun handle(cmd: CMD): StateMachineFlowResult {
        val newStateAfterHandle = state.handle(cmd)
            ?: return StateMachineFlowResult.Finish
        if (this.state == newStateAfterHandle) {
            return StateMachineFlowResult.Continue
        }
        this.state = newStateAfterHandle
        val newStateAfterInit = this.state.init()
            ?: return StateMachineFlowResult.Finish
        if (this.state == newStateAfterInit) {
            return StateMachineFlowResult.Continue
        }
        this.state = newStateAfterInit
        return StateMachineFlowResult.Continue
    }
}