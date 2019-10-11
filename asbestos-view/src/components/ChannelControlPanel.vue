<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">FHIR Server</div>
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
            updateChannelIdsFromState() {
                const channelNames = this.$store.state.base.channelIds
//                console.log(`channelNames are ${channelNames}`)
                this.channels.length = 0
                channelNames.forEach(id => {
                    this.channels.push({ value: id, text: id })
                })
            },
            updateChannelIdFromState() {
                //this.channel = this.$store.state.base.channelId
                if (this.channel === null)
                    return
                if (this.channelId !== this.channel) {
                    this.$router.push(`/session/${this.session}/channel/${this.channel}`)
                }
                this.channelId = this.channel
            },
            updateChannelFromUI() {
                if (this.channelId !== this.channel)
                    this.$router.push(`/session/${this.session}/channel/${this.channelId}`)
            },
        },
        computed: {
            channel: {
                set(name) {
                    console.log(`change channel to ${name}`)
                    if (name !== this.$store.state.base.channelId)
                        this.$store.commit('setChannelId', name)
                },
                get() {
                    return this.$store.state.base.channelId
                }
            },
            session: {
                get() {
                    return this.$store.state.base.session
                }
            },
        },
        created() {
            if (this.$store.state.base.channelIds.length === 0)
                this.$store.dispatch('loadChannelNames')
            this.channelId = this.channel
        },
        mounted() {
        },
        watch: {
            '$store.state.base.channelIds': 'updateChannelIdsFromState',
            '$store.state.base.channelId': 'updateChannelIdFromState',
            'channelId': 'updateChannelFromUI'
        },
        name: "ChannelControlPanel"
    }
</script>

<style scoped>

</style>
