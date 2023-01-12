export default {
    data() {
        return {}
    },


    methods: {
        getChannelTypeIgTestCollectionArray(channelType) {
            try {
                // if (!this.$store.getters.isCtIgTcLoaded) {
                //     let r = await this.$store.dispatch('loadChannelTypeIgTestCollections').then(() => {
                        console.info('success? ' + this.$store.getters.isCtIgTcLoaded)
                        // if (this.$store.getters.isCtIgTcLoaded) {
                            let o = this.$store.getters.getChannelIgTestCollectionArray(channelType)
                            // o.forEach(x => {
                            //     console.debug(x.igName)
                            // })
                            return o

                /*
                            return {
                                data: {theArray: o}
                            }
                        }
                    })
                    if ('data' in r) {
                        console.log('has data' + r.data.theArray.length)
                        return r.data.theArray
                    }
                    console.log('no data')
                }
                 */
            } catch (error) {
                console.error('ctIgTc error.' + error)
            }
        },

        async ftkLoadChannel(channelIdentifier, updateUri, raiseFtkCommit) {
            console.log(`ftkLoadChannel ${channelIdentifier}`)
            const theChannelId = (channelIdentifier.includes('__') ? channelIdentifier /* name is indicative of an Included channel within the test session */ : this.$store.state.base.session + '__' + channelIdentifier /* a channel local to the test session */) // this.$store.getters.getChannelId
            const theSessionName = theChannelId.split('__')[0]
            const theChannelName = theChannelId.split('__')[1]
            return this.$store.dispatch('loadChannel', {channelId: theChannelId, raiseFtkCommit: raiseFtkCommit})
                .then(c => {
                    if (updateUri) {
                        if (c !== null && c !== undefined) {
                            const current = this.$router.currentRoute.path;
                            const parts = current.split("/");
                            const size = parts.length;
                            let i;
                            // https://fhirtoolkit.test:8082/session/default/channel/default/collection/Test_Documents
                            for (i = 0; i < size; i++) {
                                if (parts[i] === 'session') {
                                    i++;
                                    parts[i] = theSessionName
                                    // console.log('Updated test session part in the URL')
                                } else if (parts[i] === 'channel' || parts[i] === 'channels' && i + 1 <= size /*&& i<size+1*/) {
                                    i++;
                                    parts[i] = theChannelName;  // insert new channelId
                                    const newRoute = parts.join('/');
                                    if (newRoute !== current) {
                                        console.log('Updated route: ' + newRoute)
                                        this.$store.commit('setChannelName', theChannelName);
                                        this.$store.commit('setChannelIsNew', false);
                                        this.$router.push(newRoute, () => {
                                                // console.debug('push complete.')
                                                return true
                                            }
                                            , () => {
                                                console.error('Route push failed.')
                                                return false
                                            });
                                    }
                                    break;
                                }
                            }
                            return true
                        }
                    }
                })
        }
    }

}

