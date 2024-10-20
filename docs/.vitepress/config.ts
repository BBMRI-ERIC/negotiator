import {defineConfig} from 'vitepress'

export default defineConfig({
    title: "Negotiator",
    description: "BBMRI-ERIC® Negotiator documentation",

    base: process.env.DOCS_BASE || "",
    lastUpdated: true,

    themeConfig: {
        outline: false,

        editLink: {
            pattern: 'https://github.com/BBMRI-ERIC/negotiator/edit/master/docs/:path',
            text: 'Edit this page on GitHub'
        },

        socialLinks: [
            {icon: 'github', link: 'https://github.com/BBMRI-ERIC/negotiator'}
        ],

        footer: {
            message: 'Released under the <a href="https://www.gnu.org/licenses/agpl-3.0.html">GNU Affero General Public License v3.0</a>',
            copyright: 'Copyright 2019 - 2024 BBMRI-ERIC • Circuit icons created by <a href="https://www.flaticon.com/free-icons/circuit" title="circuit icons">Eucalyp - Flaticon</a>',
        },

        nav: [
            {text: 'Home', link: '/'},
            {
                text: "v3.0.0",
                items: [
                    {
                        text: 'Development',
                        link: '/CONTRIBUTING',
                    },
                ]
            }
        ],

        sidebar: [
            {
                items: [
                    {text: "Overview", link: "/README.md"},
                ]
            },
            {
                text: 'REST API',
                link: '/REST',
                items: [
                    {text: 'Logging', link: '/LOGGING.md'},
                    {text: 'Security', link: '/SECURITY.md'},
                ],
            }
        ],
    }
})