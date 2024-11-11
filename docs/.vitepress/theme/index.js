// .vitepress/theme/index.js
import DefaultTheme from 'vitepress/theme'
import FileContent from './FileContent.vue'

export default {
    ...DefaultTheme,
    enhanceApp({app}) {
        app.component('FileContent', FileContent)
    }
}
