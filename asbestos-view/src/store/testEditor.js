import Vue from 'vue'
import Vuex from 'vuex'
import {newTest} from "../types/test"


Vue.use(Vuex)

let idCounter = 1
// TODO: When assigning id - check it doesnt already exist

export const testEditorStore =
    {
        state() {
            return newTest()
        },
        mutations: {
            addTestVariable(state, variable) {

                if (!variable.name) {
                    let id = idCounter++
                    let name = '#variable' + id
                    variable.id = id
                    variable.name = name
                }

                state.variables.push(variable)
            },
            delTestVariable(state, id) {
                state.variables = state.variables.filter( function (variable) {
                    return variable.id !== id
                })
            }
        },

        getters: {
            variableIndexById: (state) =>  (id) => {
                return state.variables.findIndex(function (variable) {
                    return variable.id === id
                })
            }
        }
    }




