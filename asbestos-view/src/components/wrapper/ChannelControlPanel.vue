<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Channels</div>
        <div>
            <span class="selectable" @click="manage()">Edit</span>
            &nbsp;
            <span class="selectable" @click="showId()">List by ID</span>
            &nbsp;
            <span class="selectable" @click="showAddr()">List by URL</span>
        </div>
        <b-form-select class="control-panel-font" v-model="channelId" :options="channels"></b-form-select>
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
                show: 'id',
            }
        },
        methods: {
            showId() {
                this.show = 'id'
                console.log(`show is ${this.show}`)
            },
            showAddr() {
                this.show = 'addr'
                console.log(`show is ${this.show}`)
            },
            manage() {  // go edit channel definitions
                this.$router.push(`/session/${this.$store.state.base.session}/channels` +
                    (this.channelId ? `/${this.channelId}`: ''))
            },
            updateChannelIdsFromState() {
                // const channelNames = this.$store.state.base.channelIds
                // this.channels.length = 0
                // channelNames.forEach(id => {
                //     this.channels.push({ value: id, text: id })
                // })
                console.log(`update ids`)
                const ius = this.$store.state.base.channelURLs   // { id:  ... , url: ... }
                this.channels.length = 0
                if (this.show === 'id') {
                    ius.forEach(iu => {
                        this.channels.push({value: iu.id, text: iu.id})
                    })
                } else {
                    ius.forEach(iu => {
                        this.channels.push({value: iu.id, text: iu.url})
                    })
                }
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
            if (this.$store.state.base.channelIds.length === 0) {
                //this.$store.dispatch('loadChannelNames')   // should go away
                this.$store.dispatch('loadChannelNamesAndURLs')
            }
            this.channelId = this.channel
        },
        mounted() {
        },
        watch: {
            '$store.state.base.channelURLs': 'updateChannelIdsFromState',
            'show': 'updateChannelIdsFromState',
            '$store.state.base.channelId': 'updateChannelIdFromState',
            'channelId': 'updateChannelFromUI'
        },
        name: "ChannelControlPanel"
    }
</script>

<style scoped>

</style>
