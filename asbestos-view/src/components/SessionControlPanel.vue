<template>
    <div>
        <div class="control-panel-item-title">Test Session</div>
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
                testSession: 'default',  // driven by drop down menu
                testSessions: [],  // drives drop down menu
            }
        },
        methods: {
            updateSessions() {
                let options = []
                this.$store.state.base.sessions.forEach(function(ts) {
                    let it = { value: ts, text: ts }
                    options.push(it)
                })
                this.testSessions = options
//                console.log('sessions updated on screen')
            },
            updateSession() {
                this.testSession = this.$store.state.base.session
            },
            routeTo() {
                this.$router.push(`/session/${this.testSession}`)
            },
        },
        created() {
            if (this.$store.state.base.sessions.length === 0)
                this.$store.dispatch('loadSessions')
            // for startup
            this.updateSessions()
            this.updateSession()
        },
        mounted() {
            this.$store.subscribe((mutation) => {
                //console.log(`mutation ${mutation.type}`)
                switch(mutation.type) {
                    case 'setSessions':  // to catch changes later
                        this.updateSessions()
                        this.updateSession()
                        break
                    case 'setSession':
                        this.updateSession()
                        break
                }
            })
        },
        watch: {
            'testSession': 'routeTo'
        },
        name: "SessionControlPanel"
    }
</script>

<style scoped>

</style>
