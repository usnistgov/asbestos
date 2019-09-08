import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export const channelStore = {
    state() {
        return {
            channel: null,  // private communication between ChannelNav and ChannelView
            channelTypes: [
                'passthrough',
                'mhd'
            ],
        }
    }
}
