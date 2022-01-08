<template>

<!--    <div class="specificationMargin">{{assertionId}}</div>-->
        <ol>
    <li class="asbtsReferenceTable" v-for="(refMap,rmKeyIndex ) in referenceMap" :key="rmKeyIndex" >
        <div class="gridContainer" v-for="(specRef, srKeyIndex) in Object.keys(referenceTable(refMap))"  :key="srKeyIndex">
            <div v-if="'specificationText'!==specRef">{{specRef}}</div>
            <div :class="{'specText':'specificationText'===specRef}">
                {{referenceTable(refMap)[specRef].text}}
                <template v-if="'link' in referenceTable(refMap)[specRef]">
                  <a :href="referenceTable(refMap)[specRef].link" target="_blank"><img
                          alt="External link" src="../../assets/ext_link.png" style="vertical-align: top"
                          title="Open link in a new browser tab"></a>
                </template>
            </div>
        </div>
    </li>
        </ol>


</template>

<script>
    export default {
        props: {
            assertionObj: {
                type: Object,
                required: true
            },
        },
        computed: {
            assertionId() {
                if ('id' in this.assertionObj)
                    return this.assertionObj.id
                return ''
            },
            referenceMap() {
                try {
                    const arObj = this.$store.state.testRunner.testAssertions
                    if (arObj !== null || arObj !== undefined) {
                        const aId = this.assertionId
                        if (aId in arObj.assertionReferences) {
                            const refArray = arObj.assertionReferences[aId]
                            return refArray
                        } else {
                            console.log("aId does not exist in assertionReferences");
                        }
                    } else {
                        console.log("assertionsReference is not ready")
                    }
                } catch (e) {
                    console.error('referenceObj error ' + e)
                }
                return []
            },

        },
        methods: {
            referenceTable(tableKeyMap) {
                const rObj = this.$store.state.testRunner.testAssertions.references
                const specSourceKey = Object.keys(tableKeyMap)[0]
                const specReference = tableKeyMap[specSourceKey]
                if (specSourceKey in rObj) {
                    if  (specReference in rObj[specSourceKey]) {
                        const theTable = rObj[specSourceKey][specReference]
                        // console.log(JSON.stringify(theTable))
                        return theTable
                    }
                    else
                        console.error('specReference ' + specReference + ' does not exist in Source')
                } else {
                   console.error('specSourceKey ' + specSourceKey + ' does not exist in rObj')
                }
                return {}
            }

        }
    }
</script>

<style scoped>
    .specificationMargin {
        /*margin-bottom: 10px;*/
    }
    .gridContainer {
        display: grid;
        grid-template-columns: minmax(120px,150px) auto;
        /*justify-items: left;*/
        /*justify-content: start;*/
        /*grid-template-rows:  auto auto;*/
        /*grid-auto-flow: column;*/
    }
    .asbtsReferenceTable {
        margin-top: 15px;
    }

    .specText {
       grid-column: span 2;
        margin-top: 5px;
        text-align: left;
    }

</style>