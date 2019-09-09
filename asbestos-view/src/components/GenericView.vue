<template>
    <div>
        Generic View - {{ channelId }}
        <router-view></router-view>
    </div>
</template>

<script>
    export default {
        name: "GenericView",
        methods: {
            setChannelId() {
                console.log('GenericView: setting channelId')
                //if (this.channel === undefined || this.channel === null)
                    this.channel = this.channelId
            },
        },
        created() {
            this.setChannelId()
        },
        computed: {
            channel: {
                set(name) {
                    this.$store.commit('setChannelId', name)
                },
                get() {
                    return this.$store.state.base.channelId
                }
            }
        },
        watch: {
            'channelId': 'setChannelId',
        },
        props: [ 'channelId']
    }
</script>

<style scoped>

</style>
