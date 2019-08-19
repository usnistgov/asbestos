import Vue from 'vue'
import VueRouter from 'vue-router'
import TopLayout from "../components/TopLayout";
import TestPanel from '@/components/TestPanel.vue'
import VariableEdit from '@/components/VariableEdit.vue'
import ChannelView from "../components/ChannelView";
import ChannelNav from "../components/ChannelNav";
import SessionView from "../components/SessionView";


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
            },
            {
                path: 'session/:sessionId', component: SessionView,
                children: [
                    {
                        path: 'channel', components: {
                            default: ChannelNav,
                            a: ChannelView
                        }
                    },
                    {
                        path: 'channel/:channelIndex', components: {
                            default: ChannelNav,
                            a: ChannelView
                        }
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
