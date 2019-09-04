<template>
    <div class="panel">
<!--        <router-link :to="channelsLink">Channels</router-link>-->
<!--        <div class="divider"></div>-->
<!--        <router-link :to="logsLink">Logs</router-link>-->
<!--        <div class="divider"></div>-->
<!--        <div v-if="$route.params.sessionId" class="right">-->
<!--            Test Session:-->
<!--            <b-form-select v-model="testSession" :options="testSessions"></b-form-select>-->
<!--        </div>-->
        <div class="panel">
            <router-view></router-view>
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)

    export default {
        data() {
            return {
                testSession: 'default',
                testSessions: null
            }
        },
        methods: {
            updateTestSessions() {
                let options = []
                this.$store.state.base.sessions.forEach(function(ts) {
                    let it = { value: ts, text: ts }
                    options.push(it)
                })
                this.testSessions = options
            },
            routeToTestSession() {
                this.$router.push(this.testSession)
            },
            testSessionFromRoute(route) {
                const parts = route.split('/')
                return parts[2]
            }
        },
        created() {
            this.updateTestSessions()
            this.testSession = this.sessionId
            if (this.sessionId === undefined || this.session === null) {
                this.testSession = 'default'
                this.$router.push('/session/default')
            } else {
                this.testSession = this.sessionId
            }
        },
        computed: {
            channelsLink() {
                return `/session/${this.testSession}/channels`
            },
            logsLink() {
                return `/session/${this.testSession}/log`
            }
        },
        beforeRouteUpdate (to, from, next) {
//            console.log(`Session update (local) to ${to.path}`)
            this.testSession = this.testSessionFromRoute(to.path)
            next()
        },
        components: {

        },
        watch: {
            // if sessions changes run updateOptions()
            '$store.state.base.sessions': 'updateTestSessions',
            '$route' (to) {
//                console.log(`Session route update (local) to ${to.path}`)
                this.testSession = this.testSessionFromRoute(to.path)
            },
            'testSession': 'routeToTestSession'
        },
        props: [
            'sessionId'
        ],
        name: "SessionView"
    }
</script>

<style scoped>
</style>
