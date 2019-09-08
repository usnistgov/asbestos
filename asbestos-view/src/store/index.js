import Vue from 'vue'
import Vuex from 'vuex'
import {baseStore} from "./base";
import {testEditorStore} from "./testEditor";
import {channelStore} from "./channel";
import {logStore} from "./log";

Vue.use(Vuex)

export const store = new Vuex.Store(
    {
        modules: {
            base: baseStore,  // session and channel
            testEditor: testEditorStore,
            channel: channelStore,
            log: logStore,
        }
    }
)




