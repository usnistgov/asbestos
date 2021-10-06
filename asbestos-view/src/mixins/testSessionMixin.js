export default {
    data() {
        return {}
    },
    methods: {},
    computed: {
        isSessionConfigLocked() {
            let sc = this.$store.getters.getSessionConfig
            if (sc !== undefined && sc !== null) {
                return sc.sessionConfigLocked
            } else
                return false
        },
        isCanAddChannel() {
            let sc = this.$store.getters.getSessionConfig
            if (sc !== undefined && sc !== null) {
                if (this.isSessionConfigLocked) {
                   return (sc.canAddChannel)
                } else {
                    return true
                }
            }
        },
        isCanRemoveChannel() {
            let sc = this.$store.getters.getSessionConfig
            if (sc !== undefined && sc !== null) {
                if (this.isSessionConfigLocked) {
                    return (sc.canRemoveChannel)
                } else {
                    return true
                }
            }

        },


    }
}