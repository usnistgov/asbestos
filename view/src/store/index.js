import Vue from 'vue'
import Vuex from 'vuex'
import {baseStore} from "./base";
// import {testEditorStore} from "./testEditor";

Vue.use(Vuex)

export const store = new Vuex.Store(
    {
        modules: {
            base: baseStore,
          //  testEditor: testEditorStore
        }
    }
)




