import { defineConfig } from 'vitepress'
import { execSync } from 'child_process'


function getLatestGitTag() {
    try {
        const tag = execSync('git describe --tags --abbrev=0', {stdio: 'pipe'}).toString().trim();
        if (!tag) {
            throw new Error("No Git tags found.");
        }
        return tag;
    } catch (error) {
        console.error("Error fetching Git tag:", error.message || error);
        return 'latest'; // Fallback version if no tags are found
    }
}

const latestVersion = getLatestGitTag();
export default defineConfig({
    title: 'Negotiator Docs',
    description: "Documentation for the BBMRI-ERIC Negotiator",
    head: [['link', { rel: 'icon', href: '/negotiator/favicon.ico' }]],
    cleanUrls: true,
    lastUpdated: true,
    ignoreDeadLinks: true,
    base: process.env.DOCS_BASE || "",
    themeConfig: {
        logo: {src: '/handshake.svg', width: 24, height: 24},
        nav: [
            {text: 'Docs', link: '/what-is-negotiator'},
            {
                text: 'BBMRI-ERIC', items: [
                    {
                        text: 'Web',
                        link: 'https://bbmri-eric.eu',
                    },
                    {
                        text: 'Acceptance',
                        link: 'https://negotiator.acc.bbmri-eric.eu'
                    },
                    {
                        text: 'Production',
                        link: 'https://negotiator.bbmri-eric.eu'
                    }
                ]
            },
            {
                text: latestVersion,
                items: [
                    {
                        text: 'Releases',
                        link: 'https://github.com/BBMRI-ERIC/negotiator/releases',
                    }
                ]
            }
        ],
        search: {
            provider: 'local'
        },
        editLink: {
            pattern: 'https://github.com/bbmri-eric/negotiator/edit/main/docs/:path',
            text: 'Edit this page on GitHub'
        },
        sidebar: [
            {
                text: 'Introduction',
                collapsed: false,
                items: [
                    {text: 'What is Negotiator?', link: '/what-is-negotiator'},
                    {text: 'Key features', link: '/key-features'},
                    {text: 'Quickstart', link: '/getting-started'}
                ]
            },
            {
                text: 'Users',
                collapsed: false,
                items: [
                    {text: 'Administrator', link: '/administrator'},
                    {text: 'Representative', link: '/representative'},
                    { text: 'Requester', link: '/requester' },
                    { text: 'Network Manager', link: '/network_manager' }
                ]
            },
            {
                text: 'Developers',
                collapsed: false,
                items: [
                    {text: 'Connecting to a Negotiator', link: 'connecting-to-negotiator'},
                    {text: 'Contributing', link: '/contributing'},
                    {text: 'Security', link: '/SECURITY' },
                    {text: 'Deployment', link: '/deployment'},
                    {text: 'Notifications', link: '/notifications'},
                    {text: 'REST API', link: '/REST'},
                    {text: 'Authentication and Authorization', link: '/auth'},
                    {text: 'Logging', link: '/logging'},
                    {text: 'Lifecycle', link: '/lifecycle'},
                    {text: 'Dynamic Access Form', link: '/dynamic-access-form'},
                    {text: 'Database Migration', link: '/database_migration'},
                    {text: 'Internationalization', link: '/internationalization'}
                ]
            }
        ],
        footer: {
            message: 'Released under the AGPL-3.0 License.',
            copyright: 'Copyright© 2019-present BBMRI-ERIC'
        },
        socialLinks: [
            {icon: 'github', link: 'https://github.com/BBMRI-ERIC/negotiator'}
        ]
    }
})
