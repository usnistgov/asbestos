<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Channels</div>
        <div>
            <span class="selectable" @click="manage()">Config</span>
        </div>
        <select v-model="channelId" size="10" class="control-panel-font">
            <option v-for="(chann, channeli) in effectiveChannelIds"
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
            }
        },
        methods: {
            manage() {  // go edit channel definitions
                this.$router.push(`/session/${this.$store.state.base.session}/channels` +
                    (this.channelName ? `/${this.channelName}`: ''))
            },
        },
        computed: {
          effectiveChannelIds: {
              get() {
                return this.$store.getters.getEffectiveChannelIds;
              }
          },
          channelId: {
            set(id) {
                const parts = id.split('__', 2);
                this.session = parts[0];
                this.channelName = parts[1];
            },
            get() {
              return this.$store.getters.getChannelId
            }
          },
            channelName: {
                set(name) {
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
        },
        watch: {
        },
        name: "ChannelControlPanel"
    }
</script>

<style scoped>

</style>
