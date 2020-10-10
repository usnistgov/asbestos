<template>
  <div>
    <div class="control-panel-item-title" @click="manage()">Channels</div>
    <div>
      <span class="selectable" @click="manage()">Config</span>
    </div>
    <select v-model="channelName" size="10" class="control-panel-font">
      <option v-for="(chann, channeli) in channelNames"
              v-bind:value="chann"
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
      channelNames: [],
    }
  },
  methods: {
    manage() {  // go edit channel definitions
      this.$router.push(`/session/${this.$store.state.base.session}/channels` +
          (this.channelName ? `/${this.channelName}`: ''))
    },
    channelValid(channelId) {
      if (!channelId)
        return false;
      const parts = channelId.split('__', 2);
      return !(parts.length !== 2 || parts[0] === null || parts[1] === null)
    }
  },
  computed: {
    channelName: {
      set(name) {
        console.log(`set channelName to ${name}`)
        if (name !== this.$store.state.base.channelName) {
          this.$store.commit('setChannelName', name);
          this.$store.dispatch('loadChannel', this.$store.getters.getChannelId);
          const current = this.$router.currentRoute.path;
          const parts = current.split("/");
          const size = parts.length;
          let i;
          for (i=0; i<size; i++) {
            if (parts[i] === 'channel' && i<size+1) {
              i++;
              parts[i] = name;  // insert new channelId
              const newRoute = parts.join('/');
              this.$router.push(newRoute);
              break;
            }
          }
        }
      },
      get() {
        return this.$store.state.base.channelName;
      }
    },
    session: {
      set(id) {
        if (id !== this.$store.state.base.session)
          this.$store.commit('setSession', id);
      },
      get() {
        return this.$store.state.base.session;
      }
    },
  },
  created() {
  },
  mounted() {
    this.$store.subscribe((mutation) => {
        switch(mutation.type) {
          case 'installChannel':
            case 'installChannelIds':
              case 'setSession':
                case 'loadChannelNames':
            this.channelNames = this.$store.getters.getChannelNamesForCurrentSession;
            break;
        }
    })
  },
  watch: {
  },
  name: "ChannelControlPanel"
}
</script>

<style scoped>

</style>
