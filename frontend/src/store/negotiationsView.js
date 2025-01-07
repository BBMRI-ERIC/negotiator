import {ref} from "vue"
import {defineStore} from "pinia"

export const useNegotiationsViewStore = defineStore("negotiationsView", () => {
    const savedNegotiationsView = ref("")

    return {savedNegotiationsView}
})
