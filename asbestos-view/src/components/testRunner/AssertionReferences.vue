<template>
<div>
    <p class="asbtsReferenceHeaderLabel" v-if="referenceMap.length > 0">Asbestos Assertion ID <span class="asbestosAssertionId">{{assertionId}}</span> has {{referenceMap.length}} reference(s):</p>
        <ol>
        <li v-for="(refMap,rmKeyIndex ) in referenceMap" :key="rmKeyIndex" >
            <div class="system-error" v-if="Object.keys(referenceTable(refMap)).length===0" :key="rmKeyIndex">
                Error: {{refMap}} Assertion Reference map exists, but the reference table is not defined in the references object literal.
            </div>
            <div v-else class="gridContainer" v-for="(referenceProperty, srKeyIndex) in Object.keys(referenceTable(refMap))"  :key="srKeyIndex">
                <template v-if="'context'!==referenceProperty">
                    <div v-if="'SpecificationText'!==referenceProperty">{{referenceProperty}}</div>
                    <template v-if="'SpecificationText'===referenceProperty">
                        <template v-for="(specTextObj, specTextObjKey) in referenceTable(refMap)[referenceProperty]">

                        <div class="specificationTextGridItem" :key="specTextObjKey">
                            <!-- Use markdown with html=false, if html is not desired anymore.
                                Change the css class usage in getSpecificationPropertyText method -->
                            <!--                    <p :title="getSpecificationPropertyComments(refMap,referenceProperty)"><vue-markdown :html="false">{{getSpecificationPropertyText(refMap,referenceProperty)}}</vue-markdown></p>-->
                            <p v-html="getSpecificationPropertyText(specTextObj, getVerbatimPhraseToFocus(refMap))"></p>
                            <template v-for="(comment, cKey) in getSpecificationPropertyComments(specTextObj)">
                                <p v-if="comment !== ''" :title="getCommentTitle(comment)" :key="cKey"  @click="showComments($event,comment)" >{{commentsLabel}}</p>
                            </template>
                        </div>
                        </template>
                    </template>
                    <template v-else>
                        <div>
                            {{getReferencePropertyText(refMap,referenceProperty)}}
                            <template v-for="(refLink, refLinkKey) in getReferencePropertyLink(refMap, referenceProperty)">
                                <a :href="refLink" target="_blank" :key="refLinkKey"><img
                                        alt="External link" src="../../assets/ext_link.png" style="vertical-align: top"
                                        title="Open link in a new browser tab"></a>
                            </template>
                        </div>
                    </template>
                </template>
            </div>
        </li>
       </ol>
</div>

</template>

<script>
    // import VueMarkdown from 'vue-markdown'

    export default {
        components: {
           // VueMarkdown
        },
        data() {
           return {
               commentsLabel: '[Comments]'
           }
        },
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
                        console.error("assertionsReference is not loaded.")
                    }
                } catch (e) {
                    console.error('referenceObj error ' + e)
                }
                return []
            },

        },
        methods: {
            getCommentTitle(comment) {
                if (comment !== undefined && comment !== null) {
                    return comment.replace(/(<([^>]+)>)/gi, '')
                }
                return ''
            },
            showComments: function(pElement, comment) {
                if (pElement.target.innerText === this.commentsLabel)
                    pElement.target.innerHTML = 'Comments<br>' + comment
            },
            getAssertionReferenceMapKey(assertionReferenceMap) {
                return Object.keys(assertionReferenceMap)[0]
            },
            getSpecReferenceValue(assertionReferenceMap, specSourceKey) {
                return assertionReferenceMap[specSourceKey]
            },
            getRawTable(assertionReferenceMap) {
                const rObj = this.$store.state.testRunner.testAssertions.references
                const specSourceKey = this.getAssertionReferenceMapKey(assertionReferenceMap)
                const specReference = this.getSpecReferenceValue(assertionReferenceMap, specSourceKey)
                if (specSourceKey in rObj) {
                    if  (specReference in rObj[specSourceKey]) {
                        const theTable = rObj[specSourceKey][specReference]  // Table (HTML Grid) to be rendered on the screen
                        // console.log(JSON.stringify(theTable))
                        return theTable
                    }
                    else {
                        const errorStr = 'specReference ' + specReference + ' does not exist in Source'
                        console.error('AssertionReferences Error 1: ' + errorStr)
                        // this.$store.commit('setError',errorStr)
                    }
                } else {
                    const errorStr = 'specSourceKey ' + specSourceKey + ' does not exist in rObj'
                    console.error('AssertionReferences Error 2: ' + errorStr)
                    // this.$store.commit('setError',errorStr)
                }
                return {}
            },
            referenceTable(assertionReferenceMap) {
                const theTable = this.getRawTable(assertionReferenceMap)
                // console.log(JSON.stringify(theTable))
                if ('context' in assertionReferenceMap) { // additional context was declared in map
                    const specContext = assertionReferenceMap.context
                    if (specContext in theTable.context) {
                         return {...theTable.common, ...theTable.context[specContext]}
                    } else {
                        const specSourceKey = this.getAssertionReferenceMapKey(assertionReferenceMap)
                        const specReference = this.getSpecReferenceValue(assertionReferenceMap, specSourceKey)
                        const errorStr = specContext + ' was declared in category: '+ specSourceKey + ' with key: '+ specReference
                            + ' in the assertionReference map, but not found in references table.'
                        console.error('AssertionReferences Error 3:' + errorStr)
                        // This $store.commit statement below causes infinite loop render vue warn
                        // this.$store.commit('setError',errorStr)
                        return {}
                    }
                }
                return theTable
            },
            getVerbatimPhraseToFocus(refMap) {
                if ('verbatimPhraseToFocus' in refMap)
                    return refMap.verbatimPhraseToFocus
                else
                    return ''
            },
            getSpecificationPropertyText(specRef, specTargetPhrase) {
                // const specRef = this.referenceTable(refMap)[referenceProperty]
                let specText = specRef.text
                if (specTargetPhrase === '' ) {
                    /* user friendly language, or descriptive assertion */
                    specTargetPhrase = this.assertionObj.description
                    const startWords = [
                        "Is",                   // Is ...
                        "Has",                  // Has ...
                        "(.*)(\\scontains)",    // bar bar contains ...
                        "^(.*:)"                // baz: ...
                    ];
                    for (const startWord of startWords) {
                        const re = new RegExp(`^${startWord}\\s`,'i') // ^start line, {pattern}, s is whitespace
                        if (specTargetPhrase.match(re) !== null) {
                            specTargetPhrase = specTargetPhrase.replace(re, "")
                            break
                        }
                    }
                    if (specTargetPhrase.endsWith("."))
                        specTargetPhrase = specTargetPhrase.slice(0,-1)
                }
                if (specText === '') {
                    console.warn('empty specText!' )
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
                if (typeof specRef === 'object') {
                    if ('text' in specRef) {
                        return specRef.text
                    } else if (Object.keys(specRef).length === 0) {
                        // text property is optional if context exists
                        // Only ONE empty JSON property will make sense, otherwise same text is returned for all other properties which may not be desired
                        // if context is available, return it
                        if ('context' in refMap) {
                            const specSourceKey = this.getAssertionReferenceMapKey(refMap)
                            const specReference = this.getSpecReferenceValue(refMap, specSourceKey)
                            return specReference.concat(refMap.context)
                        }
                    }
                }
                try {
                    console.warn("refMap Key: " + JSON.stringify(this.getAssertionReferenceMapKey(refMap)) + ", refMap table: " + JSON.stringify(this.getSpecReferenceValue(refMap, this.getAssertionReferenceMapKey(refMap))) + ":  '" + referenceProperty + "' specRef is not an object, probably a misplaced property!")
                } catch {console.error('getReferencePropertyText: an error occurred in console.warn.')}
                return ''
            },
            getSpecificationPropertyComments(specRef) {
                // const specRef = this.referenceTable(refMap)[referenceProperty]
                if ('comments' in specRef) {
                    const specComments = specRef.comments
                    return [specComments]
                }
                return ['']
            },
            getReferencePropertyLink(refMap, referenceProperty) {
                // const specRef = rawTable[referenceProperty]
                const specRef = this.referenceTable(refMap)[referenceProperty]
                    if ('link' in specRef) {
                        return [this.mhdVersionSpecificLink(specRef.link)]
                    } else if (Object.keys(specRef).length === 0
                        /* "Element": {} property type is an Empty object, a dynamically populated table property at runtime */
                        && 'context' in refMap) {
                        // build context link off the baseLink
                        const rawTable = this.getRawTable(refMap)
                        if ('baseLink' in  rawTable) {
                            const baseLink = rawTable.baseLink
                            return [this.mhdVersionSpecificLink(baseLink.concat(refMap.context))]
                        } else {
                            console.warn('context link nor baseLink is not available for ' + JSON.stringify(refMap) + ' referenceProperty ' + JSON.stringify(referenceProperty))
                        }
                    }
                    return []
            },
            mhdVersionSpecificLink(linkUrl) {
                //FIXME: make this test collection version specific
                // use this.$store.state.testRunner.currentTestCollectionName
                const currentMhdUrlBase = 'https://profiles.ihe.net/ITI/MHD'
                if (linkUrl.startsWith(currentMhdUrlBase)) {
                    const currentTcName = this.$store.state.testRunner.currentTestCollectionName
                    var tcObj = this.$store.state.testRunner.serverTestCollectionObjs.filter(e => e.name === currentTcName)
                    console.debug('tcObj length: ' + tcObj.length)
                    console.debug(tcObj[0].mhdVersion)
                    const mhdVersionSpecificDocBase = this.$store.state.testRunner.testAssertions.docBase[tcObj[0].mhdVersion]
                    console.debug(mhdVersionSpecificDocBase)
                    const re = new RegExp(`^${currentMhdUrlBase}`,'i') // ^start line, {pattern}
                    if (linkUrl.match(re) !== null) {
                        return linkUrl.replace(re, mhdVersionSpecificDocBase)
                    }
                }
                return linkUrl
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
        grid-template-columns: .2fr 1fr;
        grid-column-gap: 20px;
        /*text-align: left;*/
        /*justify-items: left;*/
        justify-content: start;
        /*grid-template-rows:  auto auto;*/
        /*grid-auto-flow: column;*/
    }

    .specificationTextGridItem {
       grid-column: span 2;
        text-align: left;
    }
    .asbestosAssertionId {
        font-family: monospace;
        background-color: #f5f5f5;
    }

</style>