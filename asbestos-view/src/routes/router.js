import Vue from 'vue'
import VueRouter from 'vue-router'
import TopLayout from "../components/TopLayout";
// import TestPanel from '@/components/TestPanel.vue'
// import VariableEdit from '@/components/VariableEdit.vue'
import ChannelsView from "../components/ChannelsView";
//import ChannelNav from "../components/ChannelNav";
import SessionView from "../components/SessionView";
import GenericView from "../components/GenericView";
import LogsView from "../components/LogsView"
import ChannelLogList from "../components/ChannelLogList"
import LogList from "../components/LogList"
import LogItem from "../components/LogItem"

Vue.use( VueRouter )

export const routes = [
    {
        path: '/', component: TopLayout,
        children: [
            {
                path: 'session/:sessionId',
                components: { session: SessionView },
                props: { session: true },
                children: [
                    {
                        path: 'channels/:channelId',
                        components: { default: ChannelsView },
                        props: { default: true},
                    },
                    {
                        path: 'channels',
                        components: { default: ChannelsView },
                        props: { default: true}
                    },
                    {
                        path: 'channel/:channelId',
                        components: { default: GenericView },
                        props: { default: true},
                        children: [
                            {
                                path: 'logs',
                                components: { default: LogsView },
                                props: { default: true }
                            },
                            {
                                path: 'logs2',
                                components: { default: ChannelLogList },
                                props: { default: true },
                            },
                            {
                                path: ':resourceType',
                                component: LogList,
                                props: true,
                            },
                            {
                                path: 'lognav/:eventId',
                                component: LogItem,
                                props: true,
                            },

                        ]
                    },

                ]
            },
            {
                path: 'session',
                components: { session: SessionView },
            },
            {
                path: '',
                components: { session: SessionView },
            },
        ]
    },

]

export const router = new VueRouter({
    mode: 'history',
    routes
})

