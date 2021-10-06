<template>

  <div>
        <channel-nav :session-id="sessionId" :channel-name="channelName" class="nav"></channel-nav>
    </div>
</template>

<script>
    import ChannelNav from "./ChannelNav"

    export default {
        data() {
            return {

            }
        },
        components: {
            ChannelNav,
        },
        name: "ChannelsView",
        mounted() {
        },
      computed: {
      },
        methods: {
          channelsLink(channelId) {
            const chan = channelId.split('__', 2);
            // const session = chan[0];
//            const
                this.channelName = chan[1];
            //return '/session/' + session + '/channels/' + channelName;
          },

          // for create a new channel
          pushNewChannelRoute() {
            return this.$router.push(this.newChannelRoute())
          },

        },
        props: [
           'sessionId', 'channelName'
        ]
    }
</script>

<style scoped>

</style>
