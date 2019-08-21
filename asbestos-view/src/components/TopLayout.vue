<template>
    <div>
        <router-link :to="sessionLink()">Channels</router-link>
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
    import { BFormSelect, BToast } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)
    Vue.component('b-toast', BToast)

    export default {
        data() {
            return {
                testSession: 'default',
                testSessions: null
            }
        },
        created() {
            this.updateTestSessions()
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
            sessionLink() {
                return '/session/' + this.testSession + '/channel'
            },
            routeToSession() {
                this.$router.push(this.sessionLink())
            }
        },
        computed: {
        },
        watch: {
            // if sessions changes run updateOptions()
            '$store.state.base.sessions': 'updateTestSessions',
            'testSession': 'routeToSession'
        },
        name: 'TopLayout',
        components: {

        }
    }
</script>
<style scoped>
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
