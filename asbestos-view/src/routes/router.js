import Vue from 'vue'
import VueRouter from 'vue-router'
import TopLayout from "../components/TopLayout";
// import TestPanel from '@/components/TestPanel.vue'
// import VariableEdit from '@/components/VariableEdit.vue'
import ChannelView from "../components/ChannelView";
import ChannelNav from "../components/ChannelNav";
import SessionView from "../components/SessionView";
// import EventView from "../components/EventView";


Vue.use( VueRouter )

export const routes = [
    {
        path: '/', component: TopLayout,
        children: [
            // {  // panel ties component to router-view name in ToolBody
            //     // these are simple ids
            //     path: 'test/:testId', components: { panel: TestPanel },
            //     children: [
            //         {
            //             path: 'variable/:variableId',
            //             components:
            //                 {
            //                     // above TestPanel is called instead of VariableEdit
            //                     // don't know why
            //                     // both displays are directed through TestPanel
            //                     panel: VariableEdit,
            //                 },
            //
            //         }
            //     ]
            // },
            {
                path: 'session/:sessionId/channel/:channelId',
                components: { nav: ChannelNav, detail: ChannelView },
                props: { nav: true, detail: true },
            },
            {
                path: 'session/:sessionId',
                components: { nav: SessionView },
                props: { nav: true }
            }

        ]
    }
]

export const router = new VueRouter({
    mode: 'history',
    routes
})

//router.push('/session/default')
