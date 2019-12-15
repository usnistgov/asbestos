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
            <div v-for="(error, errori) in errorList"
                :key="error + errori">
                <div class="divider"></div>
                {{ error }}
            </div>
        </div>
    </span>
</template>

<script>
    export default {
        data() {
            return {
                open: false,
            }
        },
        props: [
            'errorList'
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
