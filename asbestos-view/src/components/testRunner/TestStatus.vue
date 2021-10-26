<template>
    <span>
        <span v-if="statusOnRight">
            <span v-if="isPass">
                <img src="../../assets/checked.png" class="align-right">
            </span>
            <span v-else-if="isFail">
                <img src="../../assets/error.png" class="align-right">
            </span>
            <span v-else-if="isError">
                <img src="../../assets/yellow-error.png" class="align-right">
            </span>
            <span v-else>
                <img src="../../assets/blank-circle.png" class="align-right">
            </span>
        </span>
        <span v-if="!statusOnRight">
            <span v-if="isConditional">
                <span v-if="isPass">
                    <img src="../../assets/like.png" class="align-left">
                </span>
                <span v-else-if="isFail">
                    <img src="../../assets/thumb-down.png" class="align-left">
                </span>
                <span v-else-if="isError">
                    <img src="../../assets/yellow-error.png" class="align-left">
                </span>
                <span v-else>
                    <img src="../../assets/blank-circle.png" class="align-left">
                </span>
            </span>
            <span v-else-if="isExpectFailure">
                <span v-if="isPass">
                    <img src="../../assets/yellow-error.png" class="align-left" title="Partial failure expected">
                </span>
                <span v-else-if="isFail">
                    <img src="../../assets/error.png" class="align-left">
                </span>
                <span v-else-if="isError">
                    <img src="../../assets/yellow-error.png" class="align-left">
                </span>
                <span v-else>
                    <img src="../../assets/blank-circle.png" class="align-left">
                </span>
            </span>
            <span v-else>
                <span v-if="isPass">
                    <img src="../../assets/checked.png" class="align-left">
                </span>
                <span v-else-if="isTrue">
                    <img src="../../assets/like.png">
                </span>
                <span v-else-if="isFail">
                    <img src="../../assets/error.png" class="align-left">
                </span>
                <span v-else-if="isFalse">
                    <img src="../../assets/thumb-down.png" class="align-left">
                </span>
                <span v-else-if="isError">
                    <img src="../../assets/yellow-error.png" class="align-left">
                </span>
                <span v-else>
                    <img src="../../assets/blank-circle.png" class="align-left">
                </span>
            </span>
        </span>
    </span>
</template>

<script>
    import colorizeTestReports from "../../mixins/colorizeTestReports";

    export default {
        computed: {
            isConditional() {
                if (!this.script) return false;
                if (!this.script.modifierExtension) return false;
                let cond = false;
                this.script.modifierExtension.forEach(ex => {
                    if (ex.url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional')
                        cond = true;
                })
                return cond;
            },
            isExpectFailure() {
                if (this.report === null || this.report===undefined) return false;
                if (!this.report.action === null || this.report.action === undefined || this.report.action.length < 1) return false;
                if (!this.report.action[0].modifierExtension === null || this.report.action[0].modifierExtension === undefined) return false;
                let expectFailure = false;
                this.report.action[0].modifierExtension.forEach(ex => {
                    if (ex.url === 'urn:asbestos:test:action:expectFailure')
                        expectFailure = true;
                })
                return expectFailure;
            }
        },
        props: [
            'statusOnRight', 'script', 'report'
        ],
        mixins: [colorizeTestReports],
        name: "TestStatus"
    }
</script>

<style scoped>

</style>
