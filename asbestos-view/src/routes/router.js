import Vue from 'vue'
import VueRouter from 'vue-router'
import TopLayout from "../components/wrapper/TopLayout";
// import TestPanel from '@/components/TestPanel.vue'
// import VariableEdit from '@/components/VariableEdit.vue'
import ChannelsView from "../components/channelEditor/ChannelsView";
//import ChannelNav from "../components/ChannelNav";
import SessionView from "../components/SessionView";
import ChannelView from "../components/channelEditor/ChannelView";
import LogsView from "../components/LogsView"
//import ChannelLogList from "../components/ChannelLogList"
import LogList from "../components/logViewer/LogList"
import InspectEvent from "../components/logViewer/InspectEvent"
import TestCollection from "../components/testRunner/TestCollection"
import TestOrEvalDetails from "../components/testRunner/TestOrEvalDetails"
import EvalDetails from "../components/testRunner/EvalDetails"
import EvalReportAssert from "../components/testRunner/EvalReportAssert";
import About from "../components/top/About"
import Home from "../components/top/Home"
import MhdTesting from "../components/top/MhdTesting"
import Configurations from "../components/top/Configurations"
import Getter from "../components/getter/Getter"
import Admin from "../components/wrapper/Admin";
import StaticFixtureDisplay from "../components/testRunner/StaticFixtureDisplay";
import Setup from "../components/top/Setup";
//import LogAnalysisReport from "@/components/logViewer/LogAnalysisReport";
import InspectUrl from "@/components/logViewer/InspectUrl";
import DisplayTestSource from "@/components/testRunner/DisplayTestSource";
//import Tester from "@/components/testRunner/Tester";

Vue.use( VueRouter )

export const routes = [
    {
        path: '/', component: TopLayout,
        meta: {
            title: 'FHIR Toolkit'
        },
        children: [
            {
                path: 'about',
                components: { default: About },
            },
            {
                path: 'mhdtesting',
                components: { default: MhdTesting },
            },
            {
                path: 'home',
                components: {
                    default: Home
                }
            },
            {
                path: 'setup',
                components: {
                    default: Setup
                }
            },
            {
                path: 'configurations',
                components: {
                    default: Configurations
                }
            },
            {
                path: 'admin',
                components: {
                    default: Admin
                }
            },
            {
                path: 'inspectUrl',
                component: InspectUrl,
                props: (route) => ({
                    dataObject: route.query.dataObject,
                    fhirPath: route.query.fhirPath
                })
            },
            {
                path: 'session/:sessionId',
                components: { session: SessionView },
                props: { session: true },
                children: [
                    {
                        path: 'channels/:channelName',
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
                        components: { default: ChannelView },
                        props: { default: true},
                        children: [
                            {
                                // Obsolete
                                path: 'logsold',
                                components: { default: LogsView },
                                props: { default: true }
                            },
                            {
                                path: 'logs',
                                components: { default: LogList },
                                props: { default: true },
                            },
                            {
                                path: 'getter',
                                components: { default: Getter },
                                props: { default: true },
                            },
                            // {
                            //     path: ':resourceType',
                            //     component: LogList,
                            //     props: true,
                            // },
                            {
                                path: 'lognav/:eventId/:reqresp',
                                component: InspectEvent,
                                props: true,
                            },
                            {
                                path: 'lognav/:eventId',
                                component: InspectEvent,
                                props: true,
                            },
                            {
                                path: 'collection/:testCollection',
                                component: TestCollection,
                                props: true,
                                children: [
                                    {
                                        path: 'test/:testId',
                                        component: TestOrEvalDetails,
                                        props: true,
                                        children: [
                                            {
                                                path: 'event/:eventId',
                                                component: EvalDetails,
                                                props: true,
                                                children: [
                                                    {
                                                        path: 'assert/:assertIndex',
                                                        component: EvalReportAssert,
                                                        props: true,
                                                    }
                                                ]
                                            },
                                        ]
                                    },
                                ],
                            },

                        ]
                    },

                ]
            },
            {
                path: 'session',
                components: { session: SessionView },
            },{
                path: 'collection/:testCollection/test/:testId/fixture/:path',
                component: StaticFixtureDisplay,
                props: true
            }, {
                path: 'script/collection/:testCollection/test/:testId/:scriptId',
                component: DisplayTestSource,
                props: true,
            }, {
                path: 'script/collection/:testCollection/test/:testId/:scriptId/:moduleId',
                component: DisplayTestSource,
                props: true,
            }
        ]
    },

]

document.title = "FHIR Toolkit"

export const router = new VueRouter({
    mode: 'history',
    routes
})

