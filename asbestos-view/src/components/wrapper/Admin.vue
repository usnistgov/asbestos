<template>
    <div class="left">
        <h2>Sign-In</h2>
        <sign-in :banner="''" :userProps="this.asbts_UserProps" :doDefaultSignIn="true" :showCancelButton="false" @onOkClick="onOkClick" @onCancelClick="onCancelClick" />
    </div>
</template>

<script>
    import Vue from 'vue'
    import VueFlashMessage from 'vue-flash-message';
    Vue.use(VueFlashMessage);

    import { ButtonGroupPlugin, ButtonPlugin, ToastPlugin } from 'bootstrap-vue'
    Vue.use(ButtonGroupPlugin)
    Vue.use(ButtonPlugin)
    Vue.use(ToastPlugin)

    import SignIn from "../SignIn";
    import {ASBTS_USERPROPS} from "../../common/http-common";


    export default {
        data() {
            return {
               asbts_UserProps: ASBTS_USERPROPS
            }
        },
        components: {
            SignIn
        },
        methods: {
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
            },
            onOkClick() {

            },
            onCancelClick() {
                // Go to home page?
            }

        },
        name: "Admin"
    }
</script>
