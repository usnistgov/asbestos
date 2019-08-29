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
                channels: []  // ids
            }
        },
        methods: {
            manage() {
                console.info('Manage channels')
                //this.channel = null
                //this.$store.commit('setChannel', null)
                this.$router.push(`/session/${this.$store.state.base.session}/channels`)
            },
            update() {
                console.info(`ChannelControlPanel: update()`)
                let options = []
                const that = this
                this.$store.state.base.fullChannelIds.forEach(function(ts) {
                    const session = ts.split('__')[0]
                    const id = ts.split('__')[1]
                    if (session === that.$store.state.base.session) {
                        let it = {value: id, text: id}  // necessary for dropdown list
                        options.push(it)
                    }
                })
                this.channels = options
            },
            routeTo() {
                if (this.channel === null) {
                    return
                }
                const dest = `/session/${this.$store.state.base.session}/channel/${this.channel}`
                console.info(`ChannelControlPanel: route to ${dest}`)
                this.$store.commit('setChannelId', this.channel)
                this.$router.push(dest)
            },
            channelFromRoute(route) {
                const parts = route.split('/')
                return parts[4]
            },
            sectionFromRoute(route) {  // this will be channels or channel
                const parts = route.split('/')
                return parts[3]
            },
            loadChannelNames() {  //  same function exists in ChannelNav
                const that = this
                PROXY.get('channel')
                    .then(response => {
                        let theResponse = response.data
                        console.info(`ChannelControlPanel: loaded ${theResponse.length} ids`)
                        this.$store.commit('installChannelIds', theResponse.sort())
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
            },
        },
        created() {
            this.loadChannelNames()
        },
        mounted() {
            console.info('ChannelControlPanel mounted')
            this.loadChannelNames()
        },
        watch: {
            '$store.state.base.fullChannelIds': 'update',
            '$route' (to) {
                const newChannel = this.channelFromRoute(to.path)
                const section = this.sectionFromRoute(to.path)
                if (newChannel !== undefined && section === 'channel') {
                    console.info(`ChannelControlPanel:Route: (local) to channel ${newChannel}`)
                    //this.channel = newChannel
                }
            },
            'channel': 'routeTo'
        },
        name: "ChannelControlPanel"
    }
</script>

<style scoped>

</style>
