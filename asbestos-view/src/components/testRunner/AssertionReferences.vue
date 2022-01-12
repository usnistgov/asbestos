<template>
<div>
    <p class="asbtsReferenceHeaderLabel" v-if="referenceMap.length > 0">&nbsp;Reference(s):</p>
<!--    <div class="specificationMargin">{{assertionId}}</div>-->
       <ol>
        <li v-for="(refMap,rmKeyIndex ) in referenceMap" :key="rmKeyIndex" >
          <div class="gridContainer" v-for="(referenceProperty, srKeyIndex) in Object.keys(referenceTable(refMap))"  :key="srKeyIndex">
            <div v-if="'SpecificationText'!==referenceProperty">{{referenceProperty}}</div>
            <template v-if="'SpecificationText'===referenceProperty">
                <div class="specificationTextGridItem">
                    <p :title="getSpecificationPropertyComments(refMap,referenceProperty)" v-html="getSpecificationPropertyText(refMap,referenceProperty)"></p>
                </div>
            </template>
            <template v-else>
                <div>
                {{getReferencePropertyText(refMap,referenceProperty)}}
                <template v-if="'link' in referenceTable(refMap)[referenceProperty]">
                    <a :href="referenceTable(refMap)[referenceProperty].link" target="_blank"><img
                            alt="External link" src="../../assets/ext_link.png" style="vertical-align: top"
                            title="Open link in a new browser tab"></a>
                </template>
                </div>
            </template>
          </div>
        </li>
       </ol>
</div>

</template>

<script>
    export default {
        props: {
            assertionObj: {
                type: Object,
                required: true
            },
            isFail: {
                type: Boolean,
                required: true
            }
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
                            console.error(`aId ${aId} does not exist in assertionReferences.`);
                        }
                    } else {
                        console.error("assertionsReference is not ready.")
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
            },
            getSpecificationPropertyText(refMap, referenceProperty) {
                const specRef = this.referenceTable(refMap)[referenceProperty]
                let specText = specRef.text
                let specTargetPhrase = ''
                if ('verbatimPhrase' in specRef) {
                    specTargetPhrase = specRef.verbatimPhrase
                } else {
                    /* user friendly language, or descriptive assertion */
                    specTargetPhrase = this.assertionObj.description
                    const startWords = ["Is","Has","(.*)(\\scontains)"];
                    for (const startWord of startWords) {
                        const re = new RegExp(`^${startWord}\\s`)
                        if (specTargetPhrase.match(re) !== null) {
                            specTargetPhrase = specTargetPhrase.replace(re, "")
                            break
                        }
                    }
                    if (specTargetPhrase.endsWith("."))
                        specTargetPhrase = specTargetPhrase.slice(0,-1)
                }
                if (specText === '') {
                    console.warn('empty specText for ' + referenceProperty)
                    return '';
                }
                // replace the main target focus assertion tag, along with the proper text underline focus
                //const re = new RegExp(`(\\[\\d.\\d\\])(${specTargetPhrase})`,'gi') // use $2 in replace.()
                const re = new RegExp(`(${specTargetPhrase})`,'i')
                const specTargetTextClass = (this.isFail ? "failedAssertionTargetClass" : "normalAssertionTargetClass")

                specText = specText.replace(re, `<span class='${specTargetTextClass}'>$1</span>`)

                if ('hasAssertionOrderBiasAnnotations' in specRef) {
                    if (specRef.hasAssertionOrderBiasAnnotations === true) {
                        // replace assertion order annotation tags
                        return specText.replace(/(\[\d.\d\])(\S)/g,"$2")
                    }
                }
                return specText
            },
            getReferencePropertyText(refMap, referenceProperty) {
                const specRef = this.referenceTable(refMap)[referenceProperty]
                if ('text' in specRef)
                    return specRef.text
                return ''
            },
            getSpecificationPropertyComments(refMap, referenceProperty) {
                const specRef = this.referenceTable(refMap)[referenceProperty]
                if ('comments' in specRef) {
                    const specComments = specRef.comments
                    return specComments
                }
                return ''
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
    .asbtsReferenceHeaderLabel {
        margin-left: 19px;
    }

    .specificationTextGridItem {
       grid-column: span 2;
        text-align: left;
    }

</style>