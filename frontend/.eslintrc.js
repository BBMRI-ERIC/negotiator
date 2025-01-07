module.exports = {
    env: {
        browser: true,
        es2021: true
    },
    extends: [
        "eslint:recommended",
        "plugin:vue/vue3-essential",
        "plugin:vue/vue3-recommended",
        "plugin:vue/vue3-strongly-recommended",
        "@vue/eslint-config-standard"
    ],
    parserOptions: {
        ecmaVersion: 12,
        sourceType: "module"
    },
    plugins: [
        "vue"
    ],
    rules: {
        indent: [
            "error",
            2
        ],
        "linebreak-style": [
            "error",
            "unix"
        ],
        quotes: [
            "error",
            "double"
        ],
        semi: [
            "error",
            "never"
        ],
        "key-spacing": [
            "error", {beforeColon: false}
        ],
        "object-curly-spacing": [
            "error",
            "always"
        ],
        "space-before-blocks": [
            "error",
            "always"
        ]
    }
}
