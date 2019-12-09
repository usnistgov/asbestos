export default {
    methods: {
        msg(msg) {
            //console.log(msg)
            this.$bvToast.toast(msg, {noCloseButton: true})
        },
        error(err) {
            this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
            console.log(err)
        },
    }
}
