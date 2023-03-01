export default {
    data() {
        return {}
    },


    methods: {
        getChannelTypeIgTestCollectionArray(channelType) {
            try {
                // if (!this.$store.getters.isCtIgTcLoaded) {
                //     let r = await this.$store.dispatch('loadChannelTypeIgTestCollections').then(() => {
                //         console.info('success? ' + this.$store.getters.isCtIgTcLoaded)
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
            console.debug(`ftkLoadChannel ${channelIdentifier}`)
            const theChannelId = (channelIdentifier.includes('__') ? channelIdentifier /* name is indicative of an Included channel within the test session */ : this.$store.state.base.session + '__' + channelIdentifier /* a channel local to the test session */) // this.$store.getters.getChannelId
            const theSessionName = theChannelId.split('__')[0]
            const theChannelName = theChannelId.split('__')[1]
            const that = this
            return this.$store.dispatch('loadChannel', {channelId: theChannelId /*, raiseFtkCommit: raiseFtkCommit */})
                .then(c => {
                    if (updateUri) {
                        // console.debug(JSON.stringify(c))
                        if (c !== null && c !== undefined) {
                            const current = this.$router.currentRoute.path;
                            let parts = current.split("/");
                            const size = parts.length;
                            let i;
                            // https://fhirtoolkit.test:8082/session/default/channel/default/collection/Test_Documents
                            // or
                            // https://fhirtoolkit.test:8082/session/default/channels/copy7766
                            // or
                            // https://fhirtoolkit.test:8082/session/default/channels
                            for (i = 0; i < size; i++) {
                                if (parts[i] === 'session' && i + 1 < size) {
                                    i++; // forward to value
                                    parts[i] = theSessionName
                                } else if (parts[i] === 'channel' && i + 1 < size) {
                                    i++; // forward to value
                                    parts[i] = theChannelName;  // insert new channelId
                                } else if (parts[i] === 'channels' && i + 1 < size) {
                                    i++; // forward to value
                                    parts[i] = theChannelName;  // insert new channelId
                                } else if (parts[i] === 'channels' && i  == size-1) {
                                    parts.splice(size,0, theChannelName);  // add new channelId
                                    break;
                                } else if (parts[i] === 'collection' && i + 1 < size) {
                                    i++; // forward to value
                                    const tcName = parts[i]
                                    const clientTcCollection = that.$store.getters.clientTestCollectionNames(c.ccFhirIgName)
                                    if (clientTcCollection !== undefined && clientTcCollection !== null) {
                                        const foundTcName = clientTcCollection.find(item => item === tcName)
                                        if (foundTcName === undefined) {
                                            const serverTcCollection = that.$store.getters.serverTestCollectionNames(c.ccFhirIgName)
                                            if (serverTcCollection !== undefined && serverTcCollection !== null) {
                                                const foundServerTcName = serverTcCollection.find(item => item === tcName)
                                                if (foundServerTcName === undefined) {
                                                    console.debug('ftkLoadChannel find test collection list exhausted.')
                                                    that.$store.commit('setTestCollectionName', undefined)
                                                    parts = parts.slice(0, i) // remove test collection since it is not applicable to current channel, goes to temporary parking page
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                                    const newRoute = parts.join('/');
                                    if (newRoute !== current) {
                                        console.debug('ftkLoadChannel updating route: ' + newRoute + ' is raiseFtkCommit? ' + raiseFtkCommit + ' currentTestCollectionName: ' + that.$store.state.testRunner.currentTestCollectionName )
                                        that.$store.commit('setChannelName', theChannelName);
                                        that.$store.commit('setChannelIsNew', false);
                                        that.$router.push(newRoute, () => {
                                                // console.debug('push complete.')
                                                if (that.$store.state.testRunner.currentTestCollectionName !== undefined) {
                                                    if (raiseFtkCommit) {
                                                        that.$store.commit('ftkChannelLoaded', true);
                                                    }
                                                }
                                                return true
                                            }
                                            , () => {
                                                console.error('Route push failed.')
                                                return false
                                            })
                                    } else if (raiseFtkCommit) {
                                        that.$store.commit('ftkChannelLoaded', true);
                                    }
                                }
                            }
                            return true
                }).catch((e)=>{this.$store.commit('setError', 'ftkLoadChannel loadChannel error: ' + e)})
        }
    }

}

