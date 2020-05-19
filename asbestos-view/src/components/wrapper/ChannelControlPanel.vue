<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Channels</div>
        <div>
            <span class="selectable" @click="manage()">Config</span>
            &nbsp;
<!--            <span class="selectable" @click="showId()">List by ID</span>-->
<!--            &nbsp;-->
<!--            <span class="selectable" @click="showAddr()">List by URL</span>-->
        </div>
        <select v-model="channel" size="10" class="control-panel-font">
            <option v-for="(chann, channeli) in channelIds"
                    v-bind:value="getChannelIdByText(chann)"
                    :key="chann + channeli"
                    >
                {{ chann }}
            </option>
        </select>
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
            },
            showAddr() {
                this.show = 'addr'
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
                const ius = this.$store.state.base.channelURLs   // { id:  ... , url: ... }
                this.channels.length = 0
                if (this.show === 'id') {
                    ius.forEach(iu => {
                        this.channels.push({value: iu.id, text: iu.id})
                    })
                } else {
                    ius.forEach(iu => {
                        this.channels.push({value: iu.id, text: iu.url ? iu.url : iu.site })
                    })
                }
//                this.channels.sort()
            },
            updateChannelIdFromState() {
                //this.channel = this.$store.state.base.channelId
                if (this.channel === null)
                    return
                // if (this.channelId !== this.channel) {
                //     this.$router.push(`/session/${this.session}/channels/${this.channel}`)
                // }
                this.channelId = this.channel
            },
            updateChannelFromUI() {
                if (this.channelId !== this.channel)
                    this.$router.push(`/session/${this.session}/channels/${this.channelId}`)
            },
            getChannelIdByText(text) {
                let found = this.channels.find(function (element) {
                    return element.text === text
                })
                return found.value
            }
        },
        computed: {
            channelIds() {
                let ids = []
                this.channels.forEach(item => {
                    ids.push(item.text)
                })
                return ids.sort()
            },
            channel: {
                set(name) {
                    if (name !== this.$store.state.base.channelId) {
                        this.$store.commit('setChannelId', name);
                        this.$store.dispatch('loadChannel', this.$store.getters.getFullChannelId);
                    }
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
