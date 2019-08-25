<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Channel</div>
        <b-form-select v-model="channel" :options="channels"></b-form-select>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)
    import {PROXY} from '../common/http-common'

    export default {
        data() {
            return {
                channel: null,
                channels: null
            }
        },
        methods: {
            manage() {
                this.$router.push(`/session/${this.$store.state.base.session}/channel`)
            },
            update() {
                let options = []
                this.$store.state.base.channels.forEach(function(ts) {
                    let it = { value: ts, text: ts }
                    console.debug(`channel config item ${ts}`)
                    options.push(it)
                })
                this.channels = options
            },
            routeTo() {
                this.$router.push(`/session/${this.$store.state.base.session}/channel/${this.channel}`)
            },
            fromRoute(route) {
                const parts = route.split('/')
                return parts[4]
            },
            loadChannelNames() {
                const that = this
                PROXY.get('channel')
                    .then(response => {
                        let theResponse = response.data
                        this.$store.commit('installChannelIds', theResponse.sort())
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
        },
        created() {
            this.update()
        },
        mounted() {
            console.info('ChannelControlPanel mounted')
            this.loadChannelNames()
        },
        watch: {
            '$store.state.base.channels': 'update',
            '$route' (to) {
                console.info(`Channel route update to ${to.path}`)
                this.channel = this.fromRoute(to.path)
            },
            'channel': 'routeTo'
        },
        name: "ChannelControlPanel"
    }
</script>

<style scoped>

</style>
