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

        },
        created() {
            if (this.channel !== this.channelId)
                this.channel = this.channelId
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
