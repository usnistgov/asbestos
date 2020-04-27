import Vue from 'vue'
import Vuex from 'vuex'
import {baseStore} from "./base";
import {testEditorStore} from "./testEditor";
import {channelStore} from "./channel";
import {logStore} from "./log";
import {testRunnerStore} from "./testRunner";
import {heartbeatStore} from "./heartbeat";
import {debugTestScriptStore} from "./debugTestScript";
import {debugAssertionEvalStore} from "./debugAssertionEval";

Vue.use(Vuex)

export const store = new Vuex.Store(
    {
        modules: {
            base: baseStore,  // session and channel
            testEditor: testEditorStore,
            channel: channelStore,
            log: logStore,
            testRunner: testRunnerStore,
            heartbeat: heartbeatStore,
            debugTestScript: debugTestScriptStore,
            debugAssertionEval: debugAssertionEvalStore,
        }
    }
)




