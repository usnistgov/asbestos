<template>
    <div>
        <span style="color: red">{{banner}}</span>
        <div class="gridContainer">
            <div class="gridItemUserNameLabel">Username</div><div class="gridItemUserNameTextBox"><input id="bauser" type="text" v-model="usernameTxt"></div>
            <div class="gridItemPasswordLabel">Password</div><div class="gridItemUserPasswordTextBox"><input id="bapw" type="password" v-model="passwordTxt"></div>
            <div class="gridItemButtonBar">
                <button v-bind:disabled="usernameTxt.length === 0  || passwordTxt.length === 0" class="ok-button action-button" @click="onOkClick">Ok</button>
                <button v-if="showCancelButton" class="cancel-button action-button" @click="onCancelClick">Cancel</button>
            </div>
        </div>

        <div v-if="! usernameTxt"  class="loginError">
            <div class="vdivider"></div>
            <div class="vdivider"></div>
            <div class="vdivider"></div>
            <div class="vdivider"></div>
            Username must be provided
        </div>
        <div v-if="! passwordTxt"  class="loginError">
            <div class="vdivider"></div>
            <div class="vdivider"></div>
            <div class="vdivider"></div>
            <div class="vdivider"></div>
            Password must be provided
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { ButtonGroupPlugin, ButtonPlugin, ToastPlugin  } from 'bootstrap-vue'
    Vue.use(ButtonGroupPlugin)
    Vue.use(ButtonPlugin)
    Vue.use(ToastPlugin)

    import VueFlashMessage from 'vue-flash-message';
    Vue.use(VueFlashMessage);

    import {TLS_UI_PROXY} from "../common/http-common";



    export default {
        data() {
            return {
                usernameTxt : "",
                passwordTxt : ""
            }
        },
        props: {
            banner: {
                type: String,
                required: true
            },
            doDefaultSignIn: {
                type: Boolean,
                required: true
            },
            showCancelButton: {
                type: Boolean,
                required: true
            },
            userProps: {
                type: Object,
                required: true
            }
        },
        components: {
        },
        directives: {
            // Note that Vue automatically prefixes directive names with `v-`
        },
        mounted() {
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
                if (this.doDefaultSignIn) {
                     this.defaultSignIn().then(response => {
                         if (typeof response === 'boolean') {
                             this.userProps.signedIn = response
                         }
                         this.$emit('onOkClick')
                    })
                } else {
                   this.userProps.signedIn = false
                   this.$emit('onOkClick')
                }
            },
            onCancelClick() {
                this.$emit('onCancelClick')
            },
            async defaultSignIn() {
                let signedIn = false
                const that = this
                await TLS_UI_PROXY.get('signIn',  { auth: {username: this.usernameTxt, password: this.passwordTxt}})
                    .then(function () {
                        that.userProps.bauser = that.usernameTxt
                        that.userProps.bapw = that.passwordTxt
                        signedIn = true
                        that.msg('You are signed-in.')
                    })
                    .catch(function (error) {
                        let msg = ((error) ? error.message: '' )
                        msg += ((error && error.response && error.response.status && error.response.statusText) ? (error.response.status +  ': ' + error.response.statusText) : "")
                        that.error({message: msg})
                    })
                return signedIn
            }

        },
        name: "SignIn"
    }
</script>

<style scoped>
    .gridContainer {
        display: grid;
        grid-template-columns: auto;
        grid-template-rows: auto;
        grid-gap: 15px 10px;
        justify-items: start;
        justify-content: start;
        margin-top: 10px;
        margin-bottom: 10px;
    }

    .gridItemUserNameLabel {
        grid-column: 1 / span 1;
        grid-row: 1 / span 1;
    }

    .gridItemUserNameTextBox {
        grid-column: 2 / span 1;
        grid-row: 1 / span 1;
    }

    .gridItemPasswordLabel {
        grid-column: 1 / span 1;
        grid-row: 2 / span 1;
    }

    .gridItemUserPasswordTextBox {
        grid-column: 2 / span 1;
        grid-row: 2 / span 1;
    }

    .gridItemButtonBar {
        grid-column: 2 / span 1; /* span 2 */
        grid-row: 3 / span 1;
        justify-self: start; /* center */
    }

    .action-button {
        margin: 10px;
    }

    .loginError {
        color: red;
        margin-bottom: 10px;
    }
</style>
