<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Channel</div>
        <b-form-select v-model="channelId" :options="channels"></b-form-select>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)

    export default {
        data() {
            return {
                channelId: null,
                channels: [],  // ids
            }
        },
        methods: {
            manage() {  // go edit channel definitions
                this.$router.push(`/session/${this.$store.state.base.session}/channels` +
                    (this.channelId ? `/${this.channelId}`: ''))
            },
            updateChannelsFromState() {
                const channelNames = this.$store.state.base.channelIds.sort()
                this.channels.length = 0
                channelNames.forEach(id => {
                    this.channels.push({ value: id, text: id })
                })
            },
            updateChannelFromState() {
                this.channel = this.$store.state.base.channelId
                if (this.channel === null)
                    return
                this.$router.push(`/session/${this.$store.state.base.session}/channel/${this.channel}`)
            },
            updateChannelFromUI() {
                this.channel = this.channelId
            },
        },
        computed: {
            channel: {
                set(name) {
                    if (name !== this.$store.state.base.channelId)
                        this.$store.commit('setChannelId', name)
                },
                get() {
                    return this.$store.state.base.channelId
                }
            },
        },
        created() {
            if (this.$store.state.base.channelIds.length === 0)
                this.$store.dispatch('loadChannelNames')
        },
        mounted() {
        },
        watch: {
            '$store.state.base.channelIds': 'updateChannelsFromState',
            '$store.state.base.channelId': 'updateChannelFromState',
            'channelId': 'updateChannelFromUI'
        },
        name: "ChannelControlPanel"
    }
</script>

<style scoped>

</style>
