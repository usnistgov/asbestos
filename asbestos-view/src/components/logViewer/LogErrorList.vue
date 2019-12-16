<template>
    <span>
        <div class="divider"></div>
        <div class="divider"></div>
        <span class="has-cursor details">
            <span @click.stop="open = !open">Details</span>
            <span v-if="open"><img src="../../assets/arrow-down.png" @click.stop="open = !open"></span>
            <span v-else><img src="../../assets/arrow-right.png"  @click.stop="open = !open"></span>
        </span>
        <div v-if="open">
            <div v-if="errorList.length > 0">
                <div v-for="(error, errori) in errorList"
                    :key="error + errori">
                    <div class="divider"></div>
                    {{ error }}
                </div>
            </div>
            <div v-else>
                    <div class="divider"></div>
                No Errors
            </div>

            <div>
                <div class="has-cursor" @click.stop="listOpen = !listOpen">
                    <div class="divider"></div>
                    Required Attributes
                    <span v-if="listOpen"><img src="../../assets/arrow-down.png"></span>
                    <span v-else><img src="../../assets/arrow-right.png"></span>
                </div>
                <div v-if="listOpen">
                    <div class="divider"></div>
                    <div class="divider"></div>
                    {{ attList }}
                </div>
            </div>
        </div>
    </span>
</template>

<script>
    export default {
        data() {
            return {
                open: false,
                listOpen: false,
            }
        },
        props: [
            'errorList', 'attList'
        ],
        name: "LogErrorList"
    }
</script>

<style scoped>
    .details {
        font-size: smaller;
        /*border: 1px solid rgba(0, 0, 0, 0.8);*/
    }

</style>
