<template>
    <div>
        <div class="control-panel-item-title">Session</div>
        <b-form-select v-model="testSession" :options="testSessions"></b-form-select>
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
            update() {
                let options = []
                this.$store.state.base.sessions.forEach(function(ts) {
                    let it = { value: ts, text: ts }
                    options.push(it)
                })
                this.testSessions = options
            },
            routeTo() {
                this.$router.push(this.testSession)
            },
            fromRoute(route) {
                const parts = route.split('/')
                return parts[2]
            },
        },
        created() {
            this.update()
            this.testSession = this.sessionId
            if (this.sessionId === undefined || this.session === null) {
                this.testSession = 'default'
                this.$router.push('/session/default')
            } else {
                this.testSession = this.sessionId
            }
        },
        watch: {
            '$store.state.base.sessions': 'update',
            '$route' (to) {
                console.info(`Session route update (local) to ${to.path}`)
                this.testSession = this.fromRoute(to.path)
            },
            'testSession': 'routeTo'
        },
        name: "SessionControlPanel"
    }
</script>

<style scoped>

</style>
