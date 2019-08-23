<template>
    <div class="panel">
        <router-link to="/foo">Channels</router-link>
        <div class="divider"></div>
        <router-link to="/test">Tests</router-link>
        <div class="divider"></div>
        <div v-if="$route.params.sessionId" class="right">
            Test Session:
            <b-form-select v-model="testSession" :options="testSessions"></b-form-select>
        </div>
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
        beforeRouteUpdate (to, from, next) {
            this.testSession = to.path.substring(to.path.lastIndexOf('/') + 1)
            next()
        },
        components: {

        },
        watch: {
            // if sessions changes run updateOptions()
            '$store.state.base.sessions': 'updateTestSessions',
            '$route' (to) {
                this.testSession = to.path.substring(to.path.lastIndexOf('/') + 1)
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
    /*h2 {*/
    /*    grid-area: header;*/
    /*}*/
    .divider{
        width:5px;
        height:auto;
        display:inline-block;
    }
    .right {
        display:inline-block;
        float: right;
    }
    .panel {
        padding: 20px;
    }
</style>
