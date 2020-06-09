<template>
    <span>
        <div class="divider"></div>
        <div class="divider"></div>
        <span class="has-cursor details">
            <span @click.stop="open = !open">Details</span>
            <span v-if="open"><img src="../../assets/arrow-down.png" @click.stop="open = !open"></span>
            <span v-else><img src="../../assets/arrow-right.png"  @click.stop="open = !open"></span>
        </span>
        <!--  errorList entry without matching in attList is a raw error  -->
        <div v-if="listOpen || !attListName">
            <div v-for="(err, erri) in errorList"
                 :key="err + erri">
<!--                <div class="divider"></div>-->
<!--                <div class="divider"></div>-->
<!--                <div class="divider"></div>-->
<!--                <span v-if="attList && attList.indexOf(err) < 0" class="red">{{ err }}</span>-->
            </div>
        </div>

        <div v-if="open">
                <div v-if="attListName" class="has-cursor" @click.stop="listOpen = !listOpen">
                    <div class="divider"></div>
                    {{ attListName }}
                    <span v-if="listOpen"><img src="../../assets/arrow-down.png"></span>
                    <span v-else><img src="../../assets/arrow-right.png"></span>
                </div>


            <!--  errorList is atts that are missing  -->
            <div v-if="listOpen || !attListName">
                <div v-for="(att, atti) in attList"
                     :key="att + atti">
<!--                    <div class="divider"></div>-->
<!--                    <div class="divider"></div>-->
<!--                    <div class="divider"></div>-->
                        {{ att }}
                    <span v-if="errorList && errorList.indexOf(att) >= 0" class="red">Missing</span>
                    <span v-if="extraList && extraList.indexOf(att) >= 0">Extra</span>
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
            'errorList', 'attList', 'attListName', 'startOpen', 'extraList',
        ],
        name: "LogErrorList"
    }
</script>

<style scoped>
    .details {
        font-size: smaller;
    }
    .red {
        color: red;
    }

</style>
