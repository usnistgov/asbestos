import Vue from 'vue'
import VueRouter from 'vue-router'
import TopLayout from "../components/TopLayout";
import TestPanel from '@/components/TestPanel.vue'
import VariableEdit from '@/components/VariableEdit.vue'


Vue.use( VueRouter )

export const routes = [
    {
        path: '/', component: TopLayout,
        children: [
            {  // panel ties component to router-view name in ToolBody
                // these are simple ids
                path: 'test/:testId', components: { panel: TestPanel },
                children: [
                    {
                        path: 'variable/:variableId',
                        components:
                            {
                                // above TestPanel is called instead of VariableEdit
                                // don't know why
                                // both displays are directed through TestPanel
                                panel: VariableEdit,
                            },

                    }
                ]
            }
        ]
    }
]

export const router = new VueRouter({
    mode: 'history',
    routes
});
