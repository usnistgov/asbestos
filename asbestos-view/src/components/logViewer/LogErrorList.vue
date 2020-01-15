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
                <div v-if="attListName" class="has-cursor" @click.stop="listOpen = !listOpen">
                    <div class="divider"></div>
                    {{ attListName }}
                    <span v-if="listOpen"><img src="../../assets/arrow-down.png"></span>
                    <span v-else><img src="../../assets/arrow-right.png"></span>
                </div>
                <div v-if="listOpen || !attListName">
                    <div v-for="(att, atti) in attList"
                         :key="att + atti">
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <div class="divider"></div>
                        {{ att }}
                        <span v-if="errorList && errorList.indexOf(att) >= 0" class="red">Missing</span>
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
        created() {
            if (this.startOpen) {
                this.listOpen = true
            }
        },
        props: [
            'errorList', 'attList', 'attListName', 'startOpen'
        ],
        name: "LogErrorList"
    }
</script>

<style scoped>
    .details {
        font-size: smaller;
        /*border: 1px solid rgba(0, 0, 0, 0.8);*/
    }
    .red {
        color: red;
    }

</style>
